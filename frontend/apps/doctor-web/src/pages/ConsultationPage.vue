<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import {
  api,
  aiSourceLabel,
  aiSourceTone,
  fieldText,
  formatApiError,
  medicalRecordStreamUrl,
  statusClass,
  toNumber,
  useAuthStore,
  useDoctorWorkflowStore,
  type DataRow,
  type DrugItem,
} from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, LoadingState, StatusTag } from "@smart-cloud-brain/shared-ui";
import PatientContextDrawer from "../components/PatientContextDrawer.vue";
import AiRecordPreviewModal from "../components/AiRecordPreviewModal.vue";
import SaveRecordConfirmModal from "../components/SaveRecordConfirmModal.vue";
import PrescriptionRiskModal from "../components/PrescriptionRiskModal.vue";
import HighRiskConfirmModal from "../components/HighRiskConfirmModal.vue";
import CompleteRegistrationConfirmModal from "../components/CompleteRegistrationConfirmModal.vue";

const props = defineProps<{ registrationId: string }>();
const emit = defineEmits<{ refresh: [] }>();
const auth = useAuthStore();
const workflow = useDoctorWorkflowStore();
const router = useRouter();
const { registrations, triageRecords, drugs, streamText, streamStatus } = storeToRefs(workflow);
const loading = reactive({ record: false, prescription: false, complete: false });
const error = ref("");
const notice = ref("");
const contextOpen = ref(false);
const previewOpen = ref(false);
const saveConfirmOpen = ref(false);
const riskOpen = ref(false);
const highRiskOpen = ref(false);
const completeOpen = ref(false);
const dialogueText = ref("");
const checkResult = ref<DataRow | null>(null);
const recordAiProvider = ref("");
const recordAiModel = ref("");
let recordStream: AbortController | null = null;

const registration = computed(() => registrations.value.find((item) => toNumber(item.registrationId) === toNumber(props.registrationId)) ?? null);
const triage = computed(() => triageRecords.value.find((item) => toNumber(item.triageRecordId) === toNumber(registration.value?.triageRecordId)) ?? null);
const patientName = computed(() => fieldText(registration.value, "patientName", `患者${fieldText(registration.value, "patientId", "-")}`));
const isCompleted = computed(() => fieldText(registration.value, "status", "").toUpperCase() === "COMPLETED");
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
  drugs: [{ drugName: "", dosage: "", frequency: "", usageMethod: "口服" }] as DrugItem[],
});
const canSaveRecord = computed(() => medicalForm.registrationId > 0 && medicalForm.chiefComplaint.trim() && medicalForm.diagnosis.trim());
const canCheck = computed(() => prescription.drugs.every((item) => item.drugName.trim() && item.dosage.trim() && item.frequency.trim() && item.usageMethod.trim()));
const canCreate = computed(() => prescription.medicalRecordId > 0 && canCheck.value);

function applyRegistration() {
  medicalForm.registrationId = toNumber(props.registrationId);
  medicalForm.chiefComplaint = fieldText(triage.value, "chiefComplaint", fieldText(registration.value, "patientName", ""));
}

function setError(message: string) {
  error.value = message;
  notice.value = "";
}

function setNotice(message: string) {
  notice.value = message;
  error.value = "";
}

function applyDraft(draft: DataRow) {
  medicalForm.chiefComplaint = fieldText(draft, "chiefComplaint", medicalForm.chiefComplaint);
  medicalForm.presentIllness = fieldText(draft, "presentIllness", medicalForm.presentIllness);
  medicalForm.pastHistory = fieldText(draft, "pastHistory", medicalForm.pastHistory);
  medicalForm.physicalExam = fieldText(draft, "physicalExam", medicalForm.physicalExam);
  medicalForm.diagnosis = fieldText(draft, "diagnosis", medicalForm.diagnosis);
  medicalForm.treatmentAdvice = fieldText(draft, "treatmentAdvice", medicalForm.treatmentAdvice);
}

function normalizeDraft(payload: DataRow) {
  const nested = payload.data;
  return nested && typeof nested === "object" && !Array.isArray(nested) ? nested as DataRow : payload;
}

function formatDraft(draft: DataRow) {
  const sections = [
    ["主诉", fieldText(draft, "chiefComplaint", "")],
    ["现病史", fieldText(draft, "presentIllness", "")],
    ["既往史", fieldText(draft, "pastHistory", "")],
    ["体格检查", fieldText(draft, "physicalExam", "")],
    ["诊断", fieldText(draft, "diagnosis", "")],
    ["处理建议", fieldText(draft, "treatmentAdvice", "")],
  ];
  const text = sections
    .filter(([, value]) => value)
    .map(([label, value]) => `${label}：${value}`)
    .join("\n");
  return text || fieldText(draft, "soapContent", "暂无生成内容");
}

function parseEventData(raw: string) {
  try { return JSON.parse(raw) as DataRow; } catch { return { text: raw }; }
}

function handleStreamBlock(block: string) {
  const lines = block.split(/\r?\n/);
  const eventName = lines.find((line) => line.startsWith("event:"))?.slice("event:".length).trim();
  const dataText = lines.filter((line) => line.startsWith("data:")).map((line) => line.slice("data:".length).trim()).join("\n");
  if (!dataText) return;
  if (eventName === "delta") {
    const data = parseEventData(dataText);
    streamText.value += `${fieldText(data, "text", dataText)}\n`;
  } else if (eventName === "structured") {
    const draft = normalizeDraft(parseEventData(dataText));
    applyDraft(draft);
    recordAiProvider.value = fieldText(draft, "provider", "");
    recordAiModel.value = fieldText(draft, "model", "");
    streamText.value = formatDraft(draft);
    streamStatus.value = "DRAFT_READY";
  } else if (eventName === "error") {
    throw new Error(fieldText(parseEventData(dataText), "message", "智能病历生成失败"));
  }
}

async function generateRecord() {
  if (!dialogueText.value.trim()) return setError("请先填写问诊摘要。");
  loading.record = true;
    error.value = "";
    streamText.value = "";
    recordAiProvider.value = "";
    recordAiModel.value = "";
    streamStatus.value = "GENERATING";
  try {
    recordStream?.abort();
    recordStream = new AbortController();
    const response = await fetch(medicalRecordStreamUrl(medicalForm.registrationId, dialogueText.value, fieldText(triage.value, "departmentCode", "")), {
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
    previewOpen.value = true;
    setNotice("病历草稿已生成，请确认后保存。");
  } catch (err) {
    streamStatus.value = "FAILED";
    setError(formatApiError(err, "病历生成失败"));
  } finally {
    loading.record = false;
    recordStream = null;
  }
}

async function saveRecord() {
  if (!canSaveRecord.value) return setError("主诉和诊断为必填项。");
  loading.record = true;
  try {
    const saved = await api.saveMedicalRecord(auth.token(), { ...medicalForm });
    prescription.medicalRecordId = toNumber(saved.medicalRecordId);
    emit("refresh");
    saveConfirmOpen.value = false;
    setNotice("病历已保存，可以继续处方录入和审核。");
  } catch (err) {
    setError(formatApiError(err, "病历保存失败"));
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
    checkResult.value = await api.checkPrescription(auth.token(), {
      patientId: toNumber(registration.value?.patientId),
      doctorId: auth.session?.userId,
      medicalRecordId: prescription.medicalRecordId || undefined,
      diagnosis: medicalForm.diagnosis,
      pastHistory: medicalForm.pastHistory,
      drugs: prescription.drugs,
    });
    prescription.riskLevel = fieldText(checkResult.value, "riskLevel", "UNREVIEWED");
    riskOpen.value = true;
  } catch (err) {
    setError(formatApiError(err, "处方审核失败"));
  } finally {
    loading.prescription = false;
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
    await api.createPrescription(auth.token(), {
      patientId: toNumber(registration.value?.patientId),
      medicalRecordId: prescription.medicalRecordId,
      registrationId: toNumber(registration.value?.registrationId),
      riskLevel: prescription.riskLevel,
      drugs: prescription.drugs,
    });
    riskOpen.value = false;
    highRiskOpen.value = false;
    emit("refresh");
    setNotice("处方已创建。");
  } catch (err) {
    setError(formatApiError(err, "处方创建失败"));
  } finally {
    loading.prescription = false;
  }
}

async function completeRegistration() {
  loading.complete = true;
  try {
    await api.completeRegistration(auth.token(), medicalForm.registrationId);
    completeOpen.value = false;
    await workflow.refresh(auth.token());
    await router.push({ name: "doctor-queue" });
  } catch (err) {
    setError(formatApiError(err, "完成接诊失败"));
  } finally {
    loading.complete = false;
  }
}

watch(() => props.registrationId, applyRegistration, { immediate: true });
</script>

<template>
  <section class="clinical-page consultation-workbench">
    <header class="patient-context-bar">
      <div class="patient-name-block">
        <span>患者</span>
        <strong>{{ registration ? patientName : "未选择" }}</strong>
      </div>
      <div class="patient-context-grid">
        <span><b>患者 ID</b>{{ fieldText(registration, "patientId", "-") }}</span>
        <span><b>挂号 ID</b>#{{ registrationId }}</span>
        <span><b>科室</b>{{ fieldText(registration, "departmentName", "-") }}</span>
        <span><b>预约</b>{{ fieldText(registration, "appointmentTime", "-") }}</span>
        <span><b>状态</b><StatusTag v-if="registration" :status="fieldText(registration, 'status')" :tone="statusClass(registration.status)" /></span>
      </div>
      <div class="encounter-actions">
        <button type="button" @click="contextOpen = true">上下文</button>
        <button v-if="!isCompleted" type="button" class="primary" :disabled="loading.complete" @click="completeOpen = true">完成接诊</button>
      </div>
    </header>

    <ErrorState v-if="error" :message="error" />
    <div v-if="notice" class="clinical-alert success">{{ notice }}</div>
    <EmptyState v-if="!registration" title="未找到挂号" message="" />

    <div v-else class="encounter-layout">
      <aside class="clinical-section patient-rail">
        <header class="section-toolbar">
          <h2>患者/分诊</h2>
          <button type="button" class="compact-action" @click="contextOpen = true">详情</button>
        </header>
        <dl class="clinical-dl">
          <div><dt>患者</dt><dd>{{ patientName }}</dd></div>
          <div><dt>患者 ID</dt><dd>{{ fieldText(registration, "patientId", "-") }}</dd></div>
          <div><dt>科室</dt><dd>{{ fieldText(registration, "departmentName", "-") }}</dd></div>
          <div><dt>分诊状态</dt><dd>{{ fieldText(triage, "status", "-") }}</dd></div>
          <div class="span"><dt>主诉</dt><dd>{{ fieldText(triage, "chiefComplaint", medicalForm.chiefComplaint || "-") }}</dd></div>
          <div class="span"><dt>历史信息</dt><dd>{{ fieldText(triage, "pastHistory", medicalForm.pastHistory || "-") }}</dd></div>
        </dl>
      </aside>

      <main class="clinical-section record-workspace">
        <header class="section-toolbar">
          <h2>病历工作区</h2>
          <StatusTag :status="streamStatus" tone="info" />
        </header>

        <div class="record-editor-grid">
          <label class="clinical-field full">
            <span>问诊文本</span>
            <textarea v-model.trim="dialogueText" class="consultation-textarea" rows="5" />
          </label>

          <div class="ai-draft-pane full">
            <div class="inline-toolbar">
              <strong>智能草稿</strong>
              <span v-if="recordAiProvider" class="tag" :class="aiSourceTone(recordAiProvider)">
                {{ aiSourceLabel(recordAiProvider) }} · {{ recordAiProvider }}{{ recordAiModel ? ` / ${recordAiModel}` : "" }}
              </span>
              <button type="button" class="compact-action" :disabled="!streamText" @click="previewOpen = true">预览</button>
            </div>
            <LoadingState v-if="loading.record" title="正在处理病历" />
            <pre v-else-if="streamText" class="stream-box">{{ streamText }}</pre>
            <span v-else class="muted-line">暂无草稿</span>
          </div>

          <label class="clinical-field">
            <span>主诉</span>
            <input v-model.trim="medicalForm.chiefComplaint" />
          </label>
          <label class="clinical-field">
            <span>诊断</span>
            <input v-model.trim="medicalForm.diagnosis" />
          </label>
          <label class="clinical-field">
            <span>现病史</span>
            <textarea v-model.trim="medicalForm.presentIllness" />
          </label>
          <label class="clinical-field">
            <span>既往史</span>
            <textarea v-model.trim="medicalForm.pastHistory" />
          </label>
          <label class="clinical-field">
            <span>体格检查</span>
            <textarea v-model.trim="medicalForm.physicalExam" />
          </label>
          <label class="clinical-field">
            <span>处理建议</span>
            <textarea v-model.trim="medicalForm.treatmentAdvice" />
          </label>
        </div>

        <footer class="area-actions">
          <button class="primary" type="button" :disabled="loading.record" @click="generateRecord">生成病历</button>
          <button type="button" :disabled="!canSaveRecord || loading.record" @click="saveConfirmOpen = true">保存病历</button>
        </footer>
      </main>

      <aside class="clinical-section prescription-rail">
        <header class="section-toolbar">
          <h2>处方</h2>
          <StatusTag :status="prescription.riskLevel" :tone="statusClass(prescription.riskLevel)" />
        </header>

        <div class="table-scroll prescription-scroll">
          <table class="clinical-table order-table">
            <thead>
              <tr>
                <th>序号</th>
                <th>药品</th>
                <th>剂量</th>
                <th>频次</th>
                <th>用法</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(drug, index) in prescription.drugs" :key="index">
                <td>{{ index + 1 }}</td>
                <td><input v-model.trim="drug.drugName" list="drug-options" /></td>
                <td><input v-model.trim="drug.dosage" /></td>
                <td><input v-model.trim="drug.frequency" /></td>
                <td><input v-model.trim="drug.usageMethod" /></td>
                <td><button class="danger compact-action" type="button" @click="removeDrug(index)">删除</button></td>
              </tr>
            </tbody>
          </table>
        </div>

        <datalist id="drug-options"><option v-for="drug in drugs" :key="String(drug.id)" :value="String(drug.name)" /></datalist>

        <div v-if="checkResult" class="risk-result">
          {{ fieldText(checkResult, "suggestions", "请医生复核用药风险。") }}
        </div>

        <footer class="area-actions prescription-actions">
          <button type="button" @click="addDrug">新增药品</button>
          <button class="primary" type="button" :disabled="loading.prescription || !canCheck" @click="checkPrescription">风险审核</button>
          <button type="button" :disabled="loading.prescription || !canCreate" @click="createPrescription">创建处方</button>
        </footer>
      </aside>
    </div>

    <PatientContextDrawer :open="contextOpen" :registration="registration" :triage="triage" @close="contextOpen = false" />
    <AiRecordPreviewModal :open="previewOpen" :text="streamText" @close="previewOpen = false" />
    <SaveRecordConfirmModal :open="saveConfirmOpen" :busy="loading.record" @close="saveConfirmOpen = false" @confirm="saveRecord" />
    <PrescriptionRiskModal :open="riskOpen" :result="checkResult" @close="riskOpen = false" @confirm="createPrescription" />
    <HighRiskConfirmModal :open="highRiskOpen" :busy="loading.prescription" @close="highRiskOpen = false" @confirm="createPrescription" />
    <CompleteRegistrationConfirmModal :open="completeOpen" :busy="loading.complete" @close="completeOpen = false" @confirm="completeRegistration" />
  </section>
</template>
