<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import {
  api,
  displayText,
  toNumber,
  type Drug,
  type DrugItem,
  useAuthStore,
  useDoctorWorkflowStore,
} from "@smart-cloud-brain/shared-api";
import { Toast } from "@smart-cloud-brain/shared-ui";
import PatientContextDrawer from "../components/PatientContextDrawer.vue";
import AiRecordPreviewModal from "../components/AiRecordPreviewModal.vue";
import SaveRecordConfirmModal from "../components/SaveRecordConfirmModal.vue";
import PrescriptionRiskModal from "../components/PrescriptionRiskModal.vue";
import HighRiskConfirmModal from "../components/HighRiskConfirmModal.vue";
import CompleteRegistrationConfirmModal from "../components/CompleteRegistrationConfirmModal.vue";
import DrugCatalogSelect from "../components/DrugCatalogSelect.vue";
import { formatTime, liveRows, patientName, statusLabel, statusTone } from "../doctorPresentation";
import { useConsultationRecord } from "../composables/useConsultationRecord";
import { useConsultationPrescription } from "../composables/useConsultationPrescription";
import { useDoctorSettings } from "../composables/useDoctorSettings";
import type { DrugCatalogOption } from "../types/drugCatalog";

type ToastHandle = {
  success: (title: string, message?: string) => number;
  error: (title: string, message?: string) => number;
  info: (title: string, message?: string) => number;
};

const props = defineProps<{ registrationId: string }>();
const emit = defineEmits<{ refresh: [] }>();
const auth = useAuthStore();
const workflow = useDoctorWorkflowStore();
const router = useRouter();
const { registrations, triageRecords, streamText, streamStatus } = storeToRefs(workflow);
const displayRegistrations = liveRows(registrations);
const displayTriage = liveRows(triageRecords);
const error = ref("");
const contextOpen = ref(false);
const previewOpen = ref(false);
const saveConfirmOpen = ref(false);
const riskOpen = ref(false);
const highRiskOpen = ref(false);
const completeOpen = ref(false);
const completeLoading = ref(false);
const toastRef = ref<ToastHandle | null>(null);
const { settings } = useDoctorSettings();
const drugCatalog = ref<DrugCatalogOption[]>([]);
const drugCatalogLoading = ref(false);
const drugCatalogLoaded = ref(false);
const drugCatalogError = ref("");

const registration = computed(
  () => displayRegistrations.value.find((item) => toNumber(item.registrationId) === toNumber(props.registrationId)) ?? null,
);
const hasRegistration = computed(() => Boolean(registration.value && toNumber(registration.value.registrationId)));
const triage = computed(() =>
  hasRegistration.value
    ? displayTriage.value.find((item) => toNumber(item.triageRecordId) === toNumber(registration.value?.triageRecordId)) ?? null
    : null,
);
const triageRisk = computed(() => displayText(triage.value?.riskLevel, displayText(registration.value?.riskLevel, "MEDIUM")));
const isCompleted = computed(() => displayText(registration.value?.status, "").toUpperCase() === "COMPLETED");
const selectableDrugNames = computed(() =>
  new Set(drugCatalog.value.filter((drug) => !drug.disabled).map((drug) => drug.name.trim())),
);
const drugCatalogStatus = computed(() => {
  if (drugCatalogLoading.value) return "药品目录同步中";
  if (drugCatalogError.value) return "目录不可用";
  const count = selectableDrugNames.value.size;
  return count ? `可选 ${count} 种目录药品` : "暂无可用目录药品";
});

const record = useConsultationRecord(
  () => props.registrationId,
  () => registration.value,
  () => triage.value,
  auth,
  emit,
  () => toastRef.value,
);

const prescription = useConsultationPrescription(
  () => registration.value,
  record.medicalForm,
  { session: auth.session ? { userId: auth.session.userId } : undefined },
  emit,
  () => toastRef.value,
  { invalidMessage: validatePrescriptionCatalog },
);



function isDisabledDrugStatus(status: unknown) {
  const value = String(status ?? "").trim().toUpperCase();
  return ["DISABLED", "RETIRED", "OFFLINE", "INACTIVE", "STOPPED", "停用", "禁用", "下线"].includes(value);
}

function toDrugCatalogOption(row: Drug | Record<string, unknown>): DrugCatalogOption {
  const name = displayText(row.name);
  const rawStatus = displayText(row.status, "ENABLED");
  return {
    id: toNumber(row.id) || undefined,
    name,
    specification: displayText(row.specification),
    contraindication: displayText(row.contraindication),
    interactionRule: displayText(row.interactionRule),
    status: statusLabel(rawStatus, rawStatus),
    disabled: isDisabledDrugStatus(row.status),
  };
}

function validatePrescriptionCatalog(drugs: DrugItem[]) {
  if (drugCatalogLoading.value) return "药品目录加载中，请稍后再开处方。";
  if (drugCatalogError.value) return "药品目录暂不可用，请联系管理员维护药品。";
  if (!selectableDrugNames.value.size) return "药品目录暂无可用药品，请联系管理员维护药品。";
  const invalidIndex = drugs.findIndex((drug) => drug.drugName.trim() && !selectableDrugNames.value.has(drug.drugName.trim()));
  return invalidIndex >= 0 ? `第 ${invalidIndex + 1} 行请选择药品目录中的药品。` : "";
}

async function loadDrugCatalog() {
  drugCatalogLoading.value = true;
  drugCatalogError.value = "";
  try {
    const rows = await api.doctorDrugs();
    drugCatalog.value = (rows as Drug[])
      .map(toDrugCatalogOption)
      .filter((drug) => drug.name);
    drugCatalogLoaded.value = true;
  } catch {
    drugCatalog.value = [];
    drugCatalogError.value = "药品目录加载失败，请稍后重试。";
  } finally {
    drugCatalogLoading.value = false;
  }
}

function handleDrugSelect(drug: DrugItem, option: DrugCatalogOption) {
  if (!drug.dosage.trim() && option.specification) drug.dosage = option.specification;
}

function setError(message: string) {
  error.value = message;
  toastRef.value?.error("操作失败", message);
}

async function completeRegistration() {
  completeLoading.value = true;
  try {
    await api.completeRegistration(record.medicalForm.registrationId);
    completeOpen.value = false;
    await workflow.refresh();
  } catch {
    completeOpen.value = false;
  } finally {
    completeLoading.value = false;
    await router.push({ name: "doctor-notifications" });
  }
}

async function createPrescriptionWithPolicy() {
  if (String(prescription.prescription.riskLevel).toUpperCase() === "HIGH" && settings.highRiskConfirm) {
    highRiskOpen.value = true;
    return;
  }
  const ok = await prescription.createPrescription();
  if (ok) {
    riskOpen.value = false;
    highRiskOpen.value = false;
  }
}

watch(() => props.registrationId, record.applyRegistration, { immediate: true });
watch(() => settings.aiDraftMode, (mode) => {
  record.aiDraftMode.value = mode;
}, { immediate: true });
onMounted(loadDrugCatalog);
</script>

<template>
  <section class="clinical-page consultation-workbench">
    <div v-if="!hasRegistration" class="panel consultation-empty-state">
      <div class="empty-state">
        <strong>未找到接诊患者</strong>
        <span>当前链接没有匹配到有效挂号记录，请回到队列选择具体患者后进入接诊。</span>
        <button type="button" class="action-btn primary" @click="router.push({ name: 'doctor-queue' })">返回队列</button>
      </div>
    </div>

    <template v-else>
    <header class="patient-context">
      <div class="patient-main">
        <span class="eyebrow">当前患者</span>
        <div class="patient-title">
          <strong>{{ registration ? patientName(registration) : "未选择" }}</strong>
          <span class="patient-status-chip" :class="statusTone(registration?.status)">{{ statusLabel(registration?.status, "接诊中") }}</span>
        </div>
      </div>
      <div class="patient-chip-row" aria-label="当前患者上下文">
        <div class="patient-chip"><b>患者 ID</b><span>{{ displayText(registration?.patientId) }}</span></div>
        <div class="patient-chip"><b>挂号 ID</b><span>#{{ displayText(registration?.registrationId, registrationId) }}</span></div>
        <div class="patient-chip"><b>科室</b><span>{{ displayText(registration?.departmentName) }}</span></div>
        <div class="patient-chip"><b>预约</b><span>{{ formatTime(displayText(registration?.appointmentTime)) }}</span></div>
      </div>
      <div class="context-actions">
        <button type="button" class="action-btn" @click="contextOpen = true">上下文</button>
        <button v-if="!isCompleted" type="button" class="action-btn primary" :disabled="completeLoading" @click="completeOpen = true">完成接诊</button>
      </div>
    </header>

    <div v-if="error" class="notice-stack danger">
      <div><strong>操作失败：</strong>{{ error }}</div>
      <span>请根据提示复核后重试</span>
    </div>

    <div class="consult-layout">
      <!-- Left: Triage + Prescription -->
      <div class="left-column">
        <aside class="panel triage-card">
          <header class="panel-header">
            <div class="panel-title"><h3>患者分诊</h3><p>先看风险与关键病史</p></div>
            <button type="button" class="action-btn" @click="contextOpen = true">详情</button>
          </header>
          <div class="panel-body">
            <div class="triage-level">
              <div>
                <strong>AI 分诊建议：{{ statusLabel(triageRisk, "中风险") }}</strong>
                <span>{{ displayText(triage?.reason, "发热 3 天伴咽痛、黄痰，需复核过敏史并避免青霉素类用药。") }}</span>
              </div>
              <span class="tag" :class="statusTone(triageRisk)">需复核</span>
            </div>
            <div class="vitals">
              <div class="vital"><b>年龄</b><strong>{{ registration?.patientAge ? registration.patientAge + ' 岁' : '--' }}</strong></div>
              <div class="vital"><b>性别</b><strong>{{ statusLabel(registration?.patientGender, '--') }}</strong></div>
            </div>
            <div class="dl-grid">
              <div><b>分诊等级</b><span><span class="tag" :class="statusTone(triageRisk)">{{ statusLabel(triageRisk, "中风险") }}</span></span></div>
              <div><b>到诊状态</b><span><span class="tag" :class="statusTone(registration?.status)">{{ statusLabel(registration?.status, "接诊中") }}</span></span></div>
              <div class="span"><b>主诉</b><span>{{ displayText(triage?.chiefComplaint, record.medicalForm.chiefComplaint) || '暂无' }}</span></div>
              <div class="span"><b>既往史</b><span>{{ displayText(triage?.pastHistory, record.medicalForm.pastHistory) || '暂无' }}</span></div>
            </div>
          </div>
        </aside>

        <aside class="panel">
          <header class="panel-header">
            <div class="panel-title"><h3>处方与风险</h3><p>保存病历后创建处方</p></div>
            <span class="tag" :class="statusTone(prescription.prescription.riskLevel)">{{ statusLabel(prescription.prescription.riskLevel) }}</span>
          </header>
          <div class="drug-catalog-bar" :class="{ danger: drugCatalogError || (!drugCatalogLoading && drugCatalogLoaded && !selectableDrugNames.size) }">
            <div>
              <strong>药品目录选择</strong>
              <span>{{ drugCatalogError || "仅可选择管理端已维护的可用药品" }}</span>
            </div>
            <button type="button" class="action-btn" :disabled="drugCatalogLoading" @click="loadDrugCatalog">
              {{ drugCatalogLoading ? "同步中" : drugCatalogStatus }}
            </button>
          </div>
          <div class="table-wrap prescription-table-wrap">
            <table class="data-table prescription-table">
              <thead><tr><th class="drug-name">药品目录</th><th>剂量</th><th>频次</th><th>用法</th><th></th></tr></thead>
              <tbody>
                <tr v-for="(drug, index) in prescription.prescription.drugs" :key="index">
                  <td>
                    <DrugCatalogSelect
                      v-model="drug.drugName"
                      :drugs="drugCatalog"
                      :disabled="drugCatalogLoading || Boolean(drugCatalogError) || !selectableDrugNames.size"
                      :aria-label="`选择第 ${index + 1} 行药品`"
                      @select="(option) => handleDrugSelect(drug, option)"
                    />
                  </td>
                  <td><input v-model.trim="drug.dosage" /></td>
                  <td><input v-model.trim="drug.frequency" /></td>
                  <td><input v-model.trim="drug.usageMethod" /></td>
                  <td><button class="action-btn danger" type="button" @click="prescription.removeDrug(index)">删除</button></td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="risk-note">
            <strong>{{ prescription.checkResult.value ? "风险审核结果" : "待审方" }}</strong>
            <span>{{ prescription.checkResult.value ? displayText(prescription.checkResult.value.suggestions, "请医生复核用药风险。") : "系统将结合过敏史、诊断、剂量和相互作用进行风险提示。" }}</span>
          </div>
          <footer class="footer-actions">
            <button type="button" class="action-btn" @click="prescription.addDrug">新增药品</button>
            <button type="button" class="action-btn primary" :disabled="prescription.prescriptionLoading.value || !prescription.canCheck.value" @click="async () => { const ok = await prescription.checkPrescription(); if (ok) riskOpen = true; }">{{ prescription.prescriptionLoading.value ? "审核中" : "风险审核" }}</button>
            <button type="button" class="action-btn" :disabled="prescription.prescriptionLoading.value || !prescription.canCreate.value" @click="createPrescriptionWithPolicy">创建处方</button>
          </footer>
        </aside>
      </div>

      <!-- Right: Medical Record Editor -->
      <main class="panel record-panel">
        <header class="panel-header">
          <div class="panel-title"><h3>病历工作区</h3><p>主诉与诊断为保存必填项</p></div>
        </header>
        <div class="editor-grid">
          <div class="record-mode-toggle">
            <span class="mode-label">录入方式</span>
            <div class="segmented-control">
              <button :class="{ active: record.recordMode.value === 'ai' }" type="button" @click="record.recordMode.value = 'ai'">AI 生成</button>
              <button :class="{ active: record.recordMode.value === 'manual' }" type="button" @click="record.recordMode.value = 'manual'">手动录入</button>
            </div>
          </div>
          <label v-if="record.recordMode.value === 'ai'" class="field full">
            <span>问诊文本</span>
            <div class="prompt-box">
              <textarea v-model.trim="record.dialogueText.value" class="consultation-textarea" rows="4" />
              <div class="prompt-actions">
                <span v-for="chip in record.suggestedChips.value" :key="chip" class="prompt-chip" role="button" tabindex="0" @click="record.insertChip(chip)" @keydown.enter.prevent="record.insertChip(chip)" @keydown.space.prevent="record.insertChip(chip)">{{ chip }}</span>
                <span class="spacer"></span>
                <button class="action-btn primary" type="button" :disabled="record.recordLoading.value" @click="record.generateRecord">{{ record.recordLoading.value ? "生成中" : "生成病历" }}</button>
              </div>
            </div>
          </label>
          <label class="field"><span>主诉</span><input v-model.trim="record.medicalForm.chiefComplaint" /></label>
          <label class="field"><span>诊断</span><input v-model.trim="record.medicalForm.diagnosis" /></label>
          <label class="field full"><span>现病史</span><textarea v-model.trim="record.medicalForm.presentIllness" rows="2" /></label>
          <label class="field full"><span>既往史</span><textarea v-model.trim="record.medicalForm.pastHistory" rows="2" /></label>
          <label class="field full"><span>体格检查</span><textarea v-model.trim="record.medicalForm.physicalExam" rows="2" /></label>
          <label class="field full"><span>处理建议</span><textarea v-model.trim="record.medicalForm.treatmentAdvice" rows="2" /></label>
        </div>
        <footer class="footer-actions">
          <button type="button" class="action-btn" :disabled="!record.canSaveRecord.value || record.recordLoading.value" @click="saveConfirmOpen = true">保存病历</button>
        </footer>
      </main>
    </div>

    <PatientContextDrawer :open="contextOpen" :registration="registration" :triage="triage" @close="contextOpen = false" />
    <AiRecordPreviewModal :open="previewOpen" :text="streamText" @close="previewOpen = false" />
    <SaveRecordConfirmModal :open="saveConfirmOpen" :busy="record.recordLoading.value" @close="saveConfirmOpen = false" @confirm="async () => { const id = await record.saveRecord(); if (id) { prescription.prescription.medicalRecordId = id; saveConfirmOpen = false; } }" />
    <PrescriptionRiskModal :open="riskOpen" :result="prescription.checkResult.value" @close="riskOpen = false" @confirm="async () => { const ok = await prescription.createPrescription(); if (ok) riskOpen = false; }" />
    <HighRiskConfirmModal :open="highRiskOpen" :busy="prescription.prescriptionLoading.value" @close="highRiskOpen = false" @confirm="async () => { const ok = await prescription.createPrescription(); if (ok) { riskOpen = false; highRiskOpen = false; } }" />
    <CompleteRegistrationConfirmModal :open="completeOpen" :busy="completeLoading" @close="completeOpen = false" @confirm="completeRegistration" />
    <Toast ref="toastRef" />
    </template>
  </section>
</template>

<style scoped>
.record-mode-toggle {
  display: flex;
  align-items: center;
  gap: var(--space-2, 8px);
  grid-column: 1 / -1;
  margin-bottom: var(--space-2, 8px);
}
.mode-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text-secondary, #6b7280);
  white-space: nowrap;
}
.segmented-control {
  display: inline-flex;
  border: 1px solid var(--line, #e5e7eb);
  border-radius: var(--radius-sm, 6px);
  overflow: hidden;
}
.segmented-control button {
  flex: 1;
  height: 34px;
  padding: 0 14px;
  border: 0;
  background: transparent;
  color: var(--muted, #9ca3af);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}
.segmented-control button.active {
  background: var(--primary, #0b5f78);
  color: #fff;
}
</style>
