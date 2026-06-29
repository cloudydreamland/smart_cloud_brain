<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import {
  api,
  displayText,
  formatApiError,
  medicalRecordStreamUrl,
  toNumber,
  useAuthStore,
  useDoctorWorkflowStore,
  type DrugItem,
  type MedicalRecordDraft,
  type PrescriptionCheckResult,
} from "@smart-cloud-brain/shared-api";
import { Toast } from "@smart-cloud-brain/shared-ui";
import PatientContextDrawer from "../components/PatientContextDrawer.vue";
import AiRecordPreviewModal from "../components/AiRecordPreviewModal.vue";
import SaveRecordConfirmModal from "../components/SaveRecordConfirmModal.vue";
import PrescriptionRiskModal from "../components/PrescriptionRiskModal.vue";
import HighRiskConfirmModal from "../components/HighRiskConfirmModal.vue";
import CompleteRegistrationConfirmModal from "../components/CompleteRegistrationConfirmModal.vue";
import {
  formatAiDraft,
  formatTime,
  liveRows,
  patientName,
  statusLabel,
  statusTone,
} from "../doctorPresentation";

type ToastHandle = {
  success: (title: string, message?: string) => number;
  error: (title: string, message?: string) => number;
  info: (title: string, message?: string) => number;
};
type StreamPayload = MedicalRecordDraft & {
  data?: unknown;
  message?: string;
  soapContent?: string;
  text?: string;
};

const props = defineProps<{ registrationId: string }>();
const emit = defineEmits<{ refresh: [] }>();
const auth = useAuthStore();
const workflow = useDoctorWorkflowStore();
const router = useRouter();
const { registrations, triageRecords, streamText, streamStatus } = storeToRefs(workflow);
const displayRegistrations = liveRows(registrations);
const displayTriage = liveRows(triageRecords);
const loading = reactive({ record: false, prescription: false, complete: false });
const error = ref("");
const contextOpen = ref(false);
const previewOpen = ref(false);
const saveConfirmOpen = ref(false);
const riskOpen = ref(false);
const highRiskOpen = ref(false);
const completeOpen = ref(false);
const toastRef = ref<ToastHandle | null>(null);
const dialogueText = ref("");
const checkResult = ref<PrescriptionCheckResult | null>(null);
const recordAiProvider = ref("Dify");
const recordAiModel = ref("medical-record-v2");
const recordMode = ref<"ai" | "manual">("ai");
watch(recordMode, (mode) => { medicalForm.aiGenerated = mode === "ai"; });
const aiDraftMode = ref<"preview" | "auto">("preview");
onMounted(() => {
  try {
    const saved = JSON.parse(localStorage.getItem("doctor-settings") || "{}");
    if (saved.aiDraftMode) aiDraftMode.value = saved.aiDraftMode;
  } catch { /* ignore */ }
});
let recordStream: AbortController | null = null;

const registration = computed(() => displayRegistrations.value.find((item) => toNumber(item.registrationId) === toNumber(props.registrationId)) ?? null);
const hasRegistration = computed(() => Boolean(registration.value && toNumber(registration.value.registrationId)));
const triage = computed(() => hasRegistration.value ? displayTriage.value.find((item) => toNumber(item.triageRecordId) === toNumber(registration.value?.triageRecordId)) ?? null : null);
const triageRisk = computed(() => displayText(triage.value?.riskLevel, displayText(registration.value?.riskLevel, "MEDIUM")));
const isCompleted = computed(() => displayText(registration.value?.status, "").toUpperCase() === "COMPLETED");
const medicalForm = reactive({
  registrationId: toNumber(props.registrationId),
  chiefComplaint: "",
  presentIllness: "",
  pastHistory: "",
  physicalExam: "",
  diagnosis: "",
  treatmentAdvice: "",
  aiGenerated: true,
});
const prescription = reactive({
  medicalRecordId: 0,
  riskLevel: "UNREVIEWED",
  drugs: [
    { drugName: "阿莫西林胶囊", dosage: "0.5g", frequency: "tid", usageMethod: "口服" },
    { drugName: "氨溴索片", dosage: "30mg", frequency: "tid", usageMethod: "口服" },
  ] as DrugItem[],
});
const canSaveRecord = computed(() => hasRegistration.value && medicalForm.registrationId > 0 && medicalForm.chiefComplaint.trim() && medicalForm.diagnosis.trim());
const canCheck = computed(() => prescription.drugs.every((item) => item.drugName.trim() && item.dosage.trim() && item.frequency.trim() && item.usageMethod.trim()));
const canCreate = computed(() => prescription.medicalRecordId > 0 && canCheck.value);

function insertChip(text: string) {
  const sep = dialogueText.value.trim() ? "，" : "";
  dialogueText.value += `${sep}${text}`;
}

const suggestedChips = computed(() => {
  const chips: string[] = [];
  const chief = triage.value?.chiefComplaint || "";
  const history = triage.value?.pastHistory || "";
  if (chief) {
    const symptomPart = chief.replace(/^症状[：:]\s*/, "").split(/[；;]/)[0];
    symptomPart.split(/[、，,]/).forEach((s) => {
      const t = s.trim();
      if (t && t.length <= 8 && !chips.includes(t)) chips.push(t);
    });
  }
  if (history) {
    history.split(/[。；;]/).forEach((s) => {
      const t = s.trim().replace(/^[，,\s]+/, "");
      if (t && t.length <= 8 && !chips.includes(t)) chips.push(t);
    });
  }
  return chips.slice(0, 6);
});

function applyRegistration() {
  medicalForm.registrationId = toNumber(props.registrationId, toNumber(registration.value?.registrationId));
  medicalForm.chiefComplaint = displayText(triage.value?.chiefComplaint, "");
  medicalForm.pastHistory = displayText(triage.value?.pastHistory, "");
  if (!dialogueText.value.trim() && triage.value) {
    const parts: string[] = [];
    const chief = triage.value.chiefComplaint || "";
    const history = triage.value.pastHistory || "";
    if (chief) parts.push(chief);
    if (history) parts.push(`既往${history}`);
    dialogueText.value = parts.join("。");
  }
}

function setError(message: string) {
  error.value = message;
  toastRef.value?.error("操作失败", message);
}

function setNotice(message: string, variant: "success" | "info" = "success") {
  error.value = "";

  if (variant === "info") {
    toastRef.value?.info("提示", message);
  } else {
    toastRef.value?.success("操作完成", message);
  }
}

function applyDraft(draft: StreamPayload) {
  medicalForm.chiefComplaint = displayText(draft.chiefComplaint, medicalForm.chiefComplaint);
  medicalForm.presentIllness = displayText(draft.presentIllness, medicalForm.presentIllness);
  medicalForm.pastHistory = displayText(draft.pastHistory, medicalForm.pastHistory);
  medicalForm.physicalExam = displayText(draft.physicalExam, medicalForm.physicalExam);
  medicalForm.diagnosis = displayText(draft.diagnosis, medicalForm.diagnosis);
  medicalForm.treatmentAdvice = displayText(draft.treatmentAdvice, medicalForm.treatmentAdvice);
}

function normalizeDraft(payload: StreamPayload) {
  const nested = payload.data;
  return nested && typeof nested === "object" && !Array.isArray(nested) ? nested as StreamPayload : payload;
}

function formatDraft(draft: StreamPayload) {
  const sections = [
    ["主诉", displayText(draft.chiefComplaint, "")],
    ["现病史", displayText(draft.presentIllness, "")],
    ["既往史", displayText(draft.pastHistory, "")],
    ["体格检查", displayText(draft.physicalExam, "")],
    ["诊断", displayText(draft.diagnosis, "")],
    ["处理建议", displayText(draft.treatmentAdvice, "")],
  ];
  const text = sections
    .filter(([, value]) => value)
    .map(([label, value]) => `${label}：${value}`)
    .join("\n");
  return text || displayText(draft.soapContent, formatAiDraft());
}

function parseEventData(raw: string) {
  try { return JSON.parse(raw) as StreamPayload; } catch { return { text: raw }; }
}

function handleStreamBlock(block: string) {
  const lines = block.split(/\r?\n/);
  const eventName = lines.find((line) => line.startsWith("event:"))?.slice("event:".length).trim();
  const dataText = lines.filter((line) => line.startsWith("data:")).map((line) => line.slice("data:".length).trim()).join("\n");
  if (!dataText) return;
  if (eventName === "delta") {
    const data = parseEventData(dataText);
    streamText.value += `${displayText(data.text, dataText)}\n`;
  } else if (eventName === "structured") {
    const draft = normalizeDraft(parseEventData(dataText));
    if (aiDraftMode.value === "auto") applyDraft(draft);
    recordAiProvider.value = displayText(draft.provider, "Dify");
    recordAiModel.value = displayText(draft.model, "medical-record-v2");
    streamText.value = formatDraft(draft);
    streamStatus.value = "DRAFT_READY";
  } else if (eventName === "error") {
    throw new Error(displayText(parseEventData(dataText).message, "智能病历生成失败"));
  }
}

async function generateRecord() {
  if (!dialogueText.value.trim()) return setError("请先填写问诊文本。");
  loading.record = true;
  error.value = "";
  streamText.value = "";
  streamStatus.value = "GENERATING";
  try {
    recordStream?.abort();
    recordStream = new AbortController();
    const response = await fetch(medicalRecordStreamUrl(medicalForm.registrationId, dialogueText.value, displayText(triage.value?.departmentCode, "")), {
      headers: { Authorization: `Bearer ${auth.token()}` },
      signal: recordStream.signal,
    });
    if (!response.ok || !response.body) throw new Error("病历流式生成服务不可用");
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const blocks = buffer.split("\n\n");
      buffer = blocks.pop() ?? "";
      blocks.forEach(handleStreamBlock);
    }
    if (buffer.trim()) handleStreamBlock(buffer);
    streamStatus.value = "DRAFT_READY";
    if (aiDraftMode.value === "preview") previewOpen.value = true;
    setNotice("病历草稿已生成，请确认后保存。");
  } catch (err) {
    streamText.value = formatAiDraft();
    streamStatus.value = "DRAFT_READY";
    if (aiDraftMode.value === "preview") previewOpen.value = true;
    setNotice(`${formatApiError(err, "病历生成服务不可用")}；已保留本地空白草稿，请确认后再保存。`, "info");
  } finally {
    loading.record = false;
    recordStream = null;
  }
}

async function saveRecord() {
  if (!canSaveRecord.value) return setError("主诉和诊断为必填项。");
  loading.record = true;
  try {
    const saved = await api.saveMedicalRecord({ ...medicalForm });
    prescription.medicalRecordId = toNumber(saved.medicalRecordId);
    emit("refresh");
    saveConfirmOpen.value = false;
    setNotice("病历已保存，可以继续处方录入和审核。");
  } catch (err) {
    saveConfirmOpen.value = false;
    setError(formatApiError(err, "病历保存失败，请稍后重试。"));
  } finally {
    loading.record = false;
  }
}

function addDrug() {
  prescription.drugs.push({ drugName: "", dosage: "", frequency: "", usageMethod: "口服" });
}

function removeDrug(index: number) {
  if (prescription.drugs.length === 1) {
    prescription.drugs[0] = { drugName: "", dosage: "", frequency: "", usageMethod: "口服" };
    return;
  }
  prescription.drugs.splice(index, 1);
}

async function checkPrescription() {
  if (!canCheck.value) return setError("请完整填写药品、剂量、频次和用法。");
  loading.prescription = true;
  try {
    checkResult.value = await api.checkPrescription({
      patientId: toNumber(registration.value?.patientId),
      doctorId: auth.session?.userId,
      medicalRecordId: prescription.medicalRecordId || undefined,
      diagnosis: medicalForm.diagnosis,
      pastHistory: medicalForm.pastHistory,
      drugs: prescription.drugs,
    }) as PrescriptionCheckResult;
    prescription.riskLevel = displayText(checkResult.value.riskLevel, "UNREVIEWED");
  } catch (err) {
    checkResult.value = null;
    prescription.riskLevel = "UNREVIEWED";
    setError(formatApiError(err, "处方风险审核失败，请稍后重试。"));
    return;
  } finally {
    loading.prescription = false;
    if (checkResult.value) riskOpen.value = true;
  }
}

async function createPrescription() {
  if (!canCreate.value) return setError("请先保存病历并完成处方药品录入。");
  if (String(prescription.riskLevel).toUpperCase() === "HIGH" && !highRiskOpen.value) {
    highRiskOpen.value = true;
    return;
  }
  loading.prescription = true;
  try {
    await api.createPrescription({
      patientId: toNumber(registration.value?.patientId),
      medicalRecordId: prescription.medicalRecordId,
      registrationId: toNumber(registration.value?.registrationId),
      riskLevel: prescription.riskLevel,
      drugs: prescription.drugs,
    });
    emit("refresh");
    setNotice("处方已创建。");
  } catch (err) {
    setError(formatApiError(err, "处方创建失败，请稍后重试。"));
  } finally {
    riskOpen.value = false;
    highRiskOpen.value = false;
    loading.prescription = false;
  }
}

async function completeRegistration() {
  loading.complete = true;
  try {
    await api.completeRegistration(medicalForm.registrationId);
    completeOpen.value = false;
    await workflow.refresh();
  } catch {
    completeOpen.value = false;
  } finally {
    loading.complete = false;
    await router.push({ name: "doctor-notifications" });
  }
}

watch(() => props.registrationId, applyRegistration, { immediate: true });
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
        </div>
      </div>
      <div class="patient-grid">
        <div><b>患者 ID</b><span>{{ displayText(registration?.patientId) }}</span></div>
        <div><b>挂号 ID</b><span>#{{ displayText(registration?.registrationId, registrationId) }}</span></div>
        <div><b>科室</b><span>{{ displayText(registration?.departmentName) }}</span></div>
        <div><b>预约</b><span>{{ formatTime(displayText(registration?.appointmentTime)) }}</span></div>
        <div><b>状态</b><span><span class="tag" :class="statusTone(registration?.status)">{{ statusLabel(registration?.status, "接诊中") }}</span></span></div>
      </div>
      <div class="context-actions">
        <button type="button" class="action-btn" @click="contextOpen = true">上下文</button>
        <button v-if="!isCompleted" type="button" class="action-btn primary" :disabled="loading.complete" @click="completeOpen = true">完成接诊</button>
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
            <div class="panel-title">
              <h3>患者分诊</h3>
            </div>
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
              <div class="vital"><b>性别</b><strong>{{ registration?.patientGender || '--' }}</strong></div>
            </div>
            <div class="dl-grid">
              <div><b>分诊等级</b><span><span class="tag" :class="statusTone(triageRisk)">{{ statusLabel(triageRisk, "中风险") }}</span></span></div>
              <div><b>到诊状态</b><span><span class="tag" :class="statusTone(registration?.status)">{{ statusLabel(registration?.status, "接诊中") }}</span></span></div>
              <div class="span"><b>主诉</b><span>{{ displayText(triage?.chiefComplaint, medicalForm.chiefComplaint) }}</span></div>
              <div class="span"><b>既往史</b><span>{{ displayText(triage?.pastHistory, medicalForm.pastHistory) }}</span></div>
            </div>
          </div>
        </aside>

        <aside class="panel">
          <header class="panel-header">
            <div class="panel-title">
              <h3>处方与风险</h3>
            </div>
            <span class="tag" :class="statusTone(prescription.riskLevel)">{{ statusLabel(prescription.riskLevel) }}</span>
          </header>
          <div class="table-wrap prescription-table-wrap">
            <table class="data-table prescription-table">
              <thead><tr><th class="drug-name">药品</th><th>剂量</th><th>频次</th><th>用法</th><th></th></tr></thead>
              <tbody>
                <tr v-for="(drug, index) in prescription.drugs" :key="index">
                  <td><input v-model.trim="drug.drugName" list="drug-options" /></td>
                  <td><input v-model.trim="drug.dosage" /></td>
                  <td><input v-model.trim="drug.frequency" /></td>
                  <td><input v-model.trim="drug.usageMethod" /></td>
                  <td><button class="action-btn danger" type="button" @click="removeDrug(index)">删除</button></td>
                </tr>
              </tbody>
            </table>
          </div>
          <datalist id="drug-options" />
          <div class="risk-note">
            <strong>{{ checkResult ? "风险审核结果" : "待审方" }}</strong>
            <span>{{ checkResult ? displayText(checkResult.suggestions, "请医生复核用药风险。") : "系统将结合过敏史、诊断、剂量和相互作用进行风险提示。" }}</span>
          </div>
          <footer class="footer-actions">
            <button type="button" class="action-btn" @click="addDrug">新增药品</button>
            <button type="button" class="action-btn primary" :disabled="loading.prescription || !canCheck" @click="checkPrescription">{{ loading.prescription ? "审核中" : "风险审核" }}</button>
            <button type="button" class="action-btn" :disabled="loading.prescription || !canCreate" @click="createPrescription">创建处方</button>
          </footer>
        </aside>
      </div>

      <!-- Right: Medical Record Editor -->
      <main class="panel record-panel">
        <header class="panel-header">
          <div class="panel-title">
            <h3>病历工作区</h3>
          </div>
        </header>
        <div class="editor-grid">
          <div class="record-mode-toggle">
            <span class="mode-label">录入方式</span>
            <div class="segmented-control">
              <button :class="{ active: recordMode === 'ai' }" type="button" @click="recordMode = 'ai'">AI 生成</button>
              <button :class="{ active: recordMode === 'manual' }" type="button" @click="recordMode = 'manual'">手动录入</button>
            </div>
          </div>
          <label v-if="recordMode === 'ai'" class="field full">
            <span>问诊文本</span>
            <div class="prompt-box">
              <textarea v-model.trim="dialogueText" class="consultation-textarea" rows="4" />
              <div class="prompt-actions">
                <span v-for="chip in suggestedChips" :key="chip" class="prompt-chip" @click="insertChip(chip)">{{ chip }}</span>
                <span class="spacer"></span>
                <button class="action-btn primary" type="button" :disabled="loading.record" @click="generateRecord">{{ loading.record ? "生成中" : "生成病历" }}</button>
              </div>
            </div>
          </label>
          <label class="field"><span>主诉</span><input v-model.trim="medicalForm.chiefComplaint" /></label>
          <label class="field"><span>诊断</span><input v-model.trim="medicalForm.diagnosis" /></label>
          <label class="field full"><span>现病史</span><textarea v-model.trim="medicalForm.presentIllness" rows="2" /></label>
          <label class="field full"><span>既往史</span><textarea v-model.trim="medicalForm.pastHistory" rows="2" /></label>
          <label class="field full"><span>体格检查</span><textarea v-model.trim="medicalForm.physicalExam" rows="2" /></label>
          <label class="field full"><span>处理建议</span><textarea v-model.trim="medicalForm.treatmentAdvice" rows="2" /></label>
        </div>
        <footer class="footer-actions">
          <button v-if="recordMode === 'ai'" class="action-btn primary" type="button" :disabled="loading.record" @click="generateRecord">{{ loading.record ? "生成中" : "生成病历" }}</button>
          <button type="button" class="action-btn" :disabled="!canSaveRecord || loading.record" @click="saveConfirmOpen = true">保存病历</button>
        </footer>
      </main>
    </div>

    <PatientContextDrawer :open="contextOpen" :registration="registration" :triage="triage" @close="contextOpen = false" />
    <AiRecordPreviewModal :open="previewOpen" :text="streamText" @close="previewOpen = false" />
    <SaveRecordConfirmModal :open="saveConfirmOpen" :busy="loading.record" @close="saveConfirmOpen = false" @confirm="saveRecord" />
    <PrescriptionRiskModal :open="riskOpen" :result="checkResult" @close="riskOpen = false" @confirm="createPrescription" />
    <HighRiskConfirmModal :open="highRiskOpen" :busy="loading.prescription" @close="highRiskOpen = false" @confirm="createPrescription" />
    <CompleteRegistrationConfirmModal :open="completeOpen" :busy="loading.complete" @close="completeOpen = false" @confirm="completeRegistration" />
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
