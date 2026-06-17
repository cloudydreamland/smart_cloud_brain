<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { storeToRefs } from "pinia";
import {
  api,
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
import { EmptyState, ErrorState, FormField, LoadingState, StatusTag } from "@smart-cloud-brain/shared-ui";
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
let recordStream: AbortController | null = null;

const registration = computed(() => registrations.value.find((item) => toNumber(item.registrationId) === toNumber(props.registrationId)) ?? null);
const triage = computed(() => triageRecords.value.find((item) => toNumber(item.triageRecordId) === toNumber(registration.value?.triageRecordId)) ?? null);
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
    applyDraft(parseEventData(dataText));
    streamStatus.value = "DRAFT_READY";
  } else if (eventName === "error") {
    throw new Error(fieldText(parseEventData(dataText), "message", "AI generation failed"));
  }
}

async function generateRecord() {
  if (!dialogueText.value.trim()) return setError("请先填写问诊摘要。");
  loading.record = true;
  error.value = "";
  streamText.value = "";
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
    setNotice("病历草稿已生成，请医生确认后保存。");
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
    emit("refresh");
    setNotice("接诊已完成。");
  } catch (err) {
    setError(formatApiError(err, "完成接诊失败"));
  } finally {
    loading.complete = false;
  }
}

watch(() => props.registrationId, applyRegistration, { immediate: true });
</script>

<template>
  <section class="encounter-grid">
    <div class="stack">
      <section class="panel">
        <header class="panel-header">
          <div class="panel-title"><p class="eyebrow">CONSULTATION</p><h2>{{ registration ? fieldText(registration, "patientName", `患者 ${fieldText(registration, "patientId")}`) : "未选择患者" }}</h2><p>#{{ registrationId }} · {{ fieldText(registration, "departmentName") }} · {{ fieldText(registration, "appointmentTime") }}</p></div>
          <StatusTag v-if="registration" :status="fieldText(registration, 'status')" :tone="statusClass(registration.status)" />
        </header>
        <div class="panel-body toolbar">
          <button type="button" @click="contextOpen = true">查看患者上下文</button>
          <button type="button" class="primary" @click="completeOpen = true">完成接诊</button>
        </div>
      </section>

      <ErrorState v-if="error" :message="error" />
      <div v-if="notice" class="notice success">{{ notice }}</div>
      <EmptyState v-if="!registration" title="未找到挂号" message="请从队列页面选择一条挂号记录。" />

      <section v-else class="panel">
        <header class="panel-header"><div class="panel-title"><h2>AI 病历草稿与保存</h2><p>流式生成后必须由医生确认并保存。</p></div><StatusTag :status="streamStatus" tone="info" /></header>
        <div class="panel-body stack">
          <FormField label="问诊摘要"><textarea v-model.trim="dialogueText" rows="4" /></FormField>
          <div class="toolbar">
            <button class="primary" type="button" :disabled="loading.record" @click="generateRecord">流式生成病历</button>
            <button type="button" :disabled="!canSaveRecord || loading.record" @click="saveConfirmOpen = true">保存病历</button>
            <button type="button" :disabled="!streamText" @click="previewOpen = true">预览草稿</button>
          </div>
          <LoadingState v-if="loading.record" title="正在处理病历" />
          <pre v-if="streamText" class="stream-box">{{ streamText }}</pre>
          <div class="form-grid">
            <FormField label="主诉"><input v-model.trim="medicalForm.chiefComplaint" /></FormField>
            <FormField label="诊断"><input v-model.trim="medicalForm.diagnosis" /></FormField>
            <FormField label="现病史"><textarea v-model.trim="medicalForm.presentIllness" /></FormField>
            <FormField label="既往史"><textarea v-model.trim="medicalForm.pastHistory" /></FormField>
            <FormField label="体格检查"><textarea v-model.trim="medicalForm.physicalExam" /></FormField>
            <FormField label="处理建议"><textarea v-model.trim="medicalForm.treatmentAdvice" /></FormField>
          </div>
        </div>
      </section>
    </div>

    <aside class="panel">
      <header class="panel-header"><div class="panel-title"><p class="eyebrow">PRESCRIPTION</p><h2>处方录入和 AI 审核</h2><p>先审核风险，再确认创建处方。</p></div><StatusTag :status="prescription.riskLevel" :tone="statusClass(prescription.riskLevel)" /></header>
      <div class="panel-body stack">
        <div>
          <div v-for="(drug, index) in prescription.drugs" :key="index" class="drug-row">
            <FormField label="药品"><input v-model.trim="drug.drugName" list="drug-options" /></FormField>
            <FormField label="剂量"><input v-model.trim="drug.dosage" /></FormField>
            <FormField label="频次"><input v-model.trim="drug.frequency" /></FormField>
            <FormField label="用法"><input v-model.trim="drug.usageMethod" /></FormField>
            <button class="danger" type="button" @click="removeDrug(index)">删除</button>
          </div>
        </div>
        <datalist id="drug-options"><option v-for="drug in drugs" :key="String(drug.id)" :value="String(drug.name)" /></datalist>
        <div class="toolbar">
          <button type="button" @click="addDrug">新增药品</button>
          <button class="primary" type="button" :disabled="loading.prescription || !canCheck" @click="checkPrescription">审核处方</button>
          <button type="button" :disabled="loading.prescription || !canCreate" @click="createPrescription">创建处方</button>
        </div>
        <div v-if="checkResult" class="notice warning">{{ fieldText(checkResult, "suggestions", "请医生复核用药风险。") }}</div>
      </div>
    </aside>

    <PatientContextDrawer :open="contextOpen" :registration="registration" :triage="triage" @close="contextOpen = false" />
    <AiRecordPreviewModal :open="previewOpen" :text="streamText" @close="previewOpen = false" />
    <SaveRecordConfirmModal :open="saveConfirmOpen" :busy="loading.record" @close="saveConfirmOpen = false" @confirm="saveRecord" />
    <PrescriptionRiskModal :open="riskOpen" :result="checkResult" @close="riskOpen = false" @confirm="createPrescription" />
    <HighRiskConfirmModal :open="highRiskOpen" :busy="loading.prescription" @close="highRiskOpen = false" @confirm="createPrescription" />
    <CompleteRegistrationConfirmModal :open="completeOpen" :busy="loading.complete" @close="completeOpen = false" @confirm="completeRegistration" />
  </section>
</template>
