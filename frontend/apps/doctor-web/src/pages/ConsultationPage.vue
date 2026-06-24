<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import {
  api,
  fieldText,
  formatApiError,
  medicalRecordStreamUrl,
  toNumber,
  useAuthStore,
  useDoctorWorkflowStore,
  type DataRow,
  type DrugItem,
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

const props = defineProps<{ registrationId: string }>();
const emit = defineEmits<{ refresh: [] }>();
const auth = useAuthStore();
const workflow = useDoctorWorkflowStore();
const router = useRouter();
const { registrations, triageRecords, drugs, streamText, streamStatus } = storeToRefs(workflow);
const displayRegistrations = liveRows(registrations);
const displayTriage = liveRows(triageRecords);
const displayDrugs = liveRows(drugs);
const loading = reactive({ record: false, prescription: false, complete: false });
const error = ref("");
const notice = ref("");
const contextOpen = ref(false);
const previewOpen = ref(false);
const saveConfirmOpen = ref(false);
const riskOpen = ref(false);
const highRiskOpen = ref(false);
const completeOpen = ref(false);
const toastRef = ref<ToastHandle | null>(null);
const dialogueText = ref("患者诉咳嗽、咽痛、发热 3 天，最高体温 38.4°C。夜间咳嗽加重，少量黄痰。既往青霉素过敏。查体咽部充血，双肺呼吸音粗，未闻及明显湿啰音。");
const checkResult = ref<DataRow | null>(null);
const recordAiProvider = ref("Dify");
const recordAiModel = ref("medical-record-v2");
let recordStream: AbortController | null = null;

const registration = computed(() => displayRegistrations.value.find((item) => toNumber(item.registrationId) === toNumber(props.registrationId)) ?? displayRegistrations.value[0] ?? null);
const triage = computed(() => displayTriage.value.find((item) => toNumber(item.triageRecordId) === toNumber(registration.value?.triageRecordId)) ?? displayTriage.value[0] ?? null);
const triageRisk = computed(() => fieldText(triage.value, "riskLevel", fieldText(registration.value, "riskLevel", "MEDIUM")));
const isCompleted = computed(() => fieldText(registration.value, "status", "").toUpperCase() === "COMPLETED");
const medicalForm = reactive({
  registrationId: toNumber(props.registrationId),
  chiefComplaint: "咳嗽、咽痛、发热 3 天",
  presentIllness: "3 天前无明显诱因出现咳嗽、咽痛、发热，夜间咳嗽加重，伴少量黄痰。",
  pastHistory: "青霉素过敏史。否认慢性基础疾病。",
  physicalExam: "咽部充血，双肺呼吸音粗，未闻及明显湿啰音。",
  diagnosis: "急性上呼吸道感染",
  treatmentAdvice: "完善血常规及 CRP；对症退热、止咳化痰；注意休息和补液。",
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
const canSaveRecord = computed(() => medicalForm.registrationId > 0 && medicalForm.chiefComplaint.trim() && medicalForm.diagnosis.trim());
const canCheck = computed(() => prescription.drugs.every((item) => item.drugName.trim() && item.dosage.trim() && item.frequency.trim() && item.usageMethod.trim()));
const canCreate = computed(() => prescription.medicalRecordId > 0 && canCheck.value);

function applyRegistration() {
  medicalForm.registrationId = toNumber(props.registrationId, toNumber(registration.value?.registrationId));
  medicalForm.chiefComplaint = fieldText(triage.value, "chiefComplaint", medicalForm.chiefComplaint);
}

function setError(message: string) {
  error.value = message;
  notice.value = "";
  toastRef.value?.error("操作失败", message);
}

function setNotice(message: string, variant: "success" | "info" = "success") {
  notice.value = message;
  error.value = "";
  if (variant === "info") {
    toastRef.value?.info("提示", message);
  } else {
    toastRef.value?.success("操作完成", message);
  }
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
  return text || fieldText(draft, "soapContent", formatAiDraft());
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
    recordAiProvider.value = fieldText(draft, "provider", "Dify");
    recordAiModel.value = fieldText(draft, "model", "medical-record-v2");
    streamText.value = formatDraft(draft);
    streamStatus.value = "DRAFT_READY";
  } else if (eventName === "error") {
    throw new Error(fieldText(parseEventData(dataText), "message", "智能病历生成失败"));
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
    streamText.value = formatAiDraft();
    streamStatus.value = "DRAFT_READY";
    previewOpen.value = true;
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
    const saved = await api.saveMedicalRecord(auth.token(), { ...medicalForm });
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
    checkResult.value = await api.checkPrescription(auth.token(), {
      patientId: toNumber(registration.value?.patientId),
      doctorId: auth.session?.userId,
      medicalRecordId: prescription.medicalRecordId || undefined,
      diagnosis: medicalForm.diagnosis,
      pastHistory: medicalForm.pastHistory,
      drugs: prescription.drugs,
    });
    prescription.riskLevel = fieldText(checkResult.value, "riskLevel", "UNREVIEWED");
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
    await api.createPrescription(auth.token(), {
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
    await api.completeRegistration(auth.token(), medicalForm.registrationId);
    completeOpen.value = false;
    await workflow.refresh(auth.token());
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
    <header class="patient-context">
      <div class="patient-main">
        <span class="eyebrow">当前患者</span>
        <div class="patient-title">
          <strong>{{ registration ? patientName(registration) : "未选择" }}</strong>
          <span class="tag" :class="statusTone(registration?.status)">{{ statusLabel(registration?.status, "接诊中") }}</span>
          <span class="tag" :class="statusTone(triageRisk)">{{ statusLabel(triageRisk, "中风险") }}</span>
        </div>
        <p>
          患者 ID #{{ fieldText(registration, "patientId") || "-" }} ·
          挂号 #{{ fieldText(registration, "registrationId", registrationId) }} ·
          {{ fieldText(registration, "departmentName", "未分配科室") }} ·
          {{ fieldText(registration, "appointmentTime", "待确认预约") }}
        </p>
      </div>
      <div class="patient-grid">
        <div><b>患者 ID</b><span>{{ fieldText(registration, "patientId") }}</span></div>
        <div><b>挂号 ID</b><span>#{{ fieldText(registration, "registrationId", registrationId) }}</span></div>
        <div><b>科室</b><span>{{ fieldText(registration, "departmentName") }}</span></div>
        <div><b>预约</b><span>{{ fieldText(registration, "appointmentTime") }}</span></div>
        <div><b>状态</b><span><span class="tag" :class="statusTone(registration?.status)">{{ statusLabel(registration?.status, "接诊中") }}</span></span></div>
      </div>
      <div class="context-actions">
        <button type="button" @click="contextOpen = true">上下文</button>
        <button v-if="!isCompleted" type="button" class="primary" :disabled="loading.complete" @click="completeOpen = true">完成接诊</button>
      </div>
    </header>

    <div v-if="error" class="notice-stack danger">
      <div><strong>操作失败：</strong>{{ error }}</div>
      <span>请根据提示复核后重试</span>
    </div>
    <div v-else-if="notice" class="notice-stack success">
      <div><strong>状态更新：</strong>{{ notice }}</div>
      <span>{{ statusLabel(streamStatus) }} · {{ recordAiProvider }} · {{ recordAiModel }}</span>
    </div>

    <div class="consult-layout">
      <!-- Left: Triage + Prescription -->
      <div class="left-column">
        <aside class="panel triage-card">
          <header class="panel-header">
            <div class="panel-title">
              <h3>患者分诊</h3>
              <p>接诊侧栏摘要，详情仍由患者上下文抽屉承载。</p>
            </div>
            <button type="button" @click="contextOpen = true">详情</button>
          </header>
          <div class="panel-body">
            <div class="triage-level">
              <div>
                <strong>AI 分诊建议：{{ statusLabel(triageRisk, "中风险") }}</strong>
                <span>{{ fieldText(triage, "reason", "发热 3 天伴咽痛、黄痰，需复核过敏史并避免青霉素类用药。") }}</span>
              </div>
              <span class="tag" :class="statusTone(triageRisk)">需复核</span>
            </div>
            <div class="vitals">
              <div class="vital"><b>年龄</b><strong>37 岁</strong></div>
              <div class="vital"><b>性别</b><strong>女</strong></div>
              <div class="vital"><b>体温</b><strong>38.1°C</strong></div>
            </div>
            <div class="dl-grid">
              <div><b>分诊等级</b><span><span class="tag" :class="statusTone(triageRisk)">{{ statusLabel(triageRisk, "中风险") }}</span></span></div>
              <div><b>到诊状态</b><span><span class="tag" :class="statusTone(registration?.status)">{{ statusLabel(registration?.status, "接诊中") }}</span></span></div>
              <div class="span"><b>主诉</b><span>{{ fieldText(triage, "chiefComplaint", medicalForm.chiefComplaint) }}</span></div>
              <div class="span"><b>既往史</b><span>{{ fieldText(triage, "pastHistory", medicalForm.pastHistory) }}</span></div>
            </div>
          </div>
        </aside>

        <aside class="panel">
          <header class="panel-header">
            <div class="panel-title">
              <h3>处方与风险</h3>
              <p>结合过敏史、诊断、剂量和相互作用进行风险提示。</p>
            </div>
            <span class="tag" :class="statusTone(prescription.riskLevel)">{{ statusLabel(prescription.riskLevel) }}</span>
          </header>
          <div class="table-wrap">
            <table class="order-table">
              <thead><tr><th class="drug-name">药品</th><th>剂量</th><th>频次</th><th>用法</th><th></th></tr></thead>
              <tbody>
                <tr v-for="(drug, index) in prescription.drugs" :key="index">
                  <td><input v-model.trim="drug.drugName" list="drug-options" /></td>
                  <td><input v-model.trim="drug.dosage" /></td>
                  <td><input v-model.trim="drug.frequency" /></td>
                  <td><input v-model.trim="drug.usageMethod" /></td>
                  <td><button class="danger" type="button" @click="removeDrug(index)">删除</button></td>
                </tr>
              </tbody>
            </table>
          </div>
          <datalist id="drug-options">
            <option v-for="drug in displayDrugs" :key="String(drug.id)" :value="String(drug.name)" />
          </datalist>
          <div class="risk-note">
            <strong>{{ checkResult ? "风险审核结果" : "待审方" }}</strong>
            <span>{{ checkResult ? fieldText(checkResult, "suggestions", "请医生复核用药风险。") : "系统将结合过敏史、诊断、剂量和相互作用进行风险提示。" }}</span>
          </div>
          <footer class="footer-actions">
            <button type="button" @click="addDrug">新增药品</button>
            <button class="primary" type="button" :disabled="loading.prescription || !canCheck" @click="checkPrescription">{{ loading.prescription ? "审核中" : "风险审核" }}</button>
            <button type="button" :disabled="loading.prescription || !canCreate" @click="createPrescription">创建处方</button>
          </footer>
        </aside>
      </div>

      <!-- Right: Medical Record Editor -->
      <main class="panel record-panel">
        <header class="panel-header">
          <div class="panel-title">
            <h3>病历工作区</h3>
            <p>问诊文本、AI 草稿和结构化病历在同一工作区内闭环。</p>
          </div>
          <div class="record-status">
            <span class="tag info">{{ statusLabel(streamStatus) }}</span>
            <span>{{ recordAiProvider }} · {{ recordAiModel }}</span>
          </div>
        </header>
        <div class="editor-grid">
          <label class="field full">
            <span>问诊文本 <small>AI 生成病历前的核心输入</small></span>
            <div class="prompt-box">
              <textarea v-model.trim="dialogueText" class="consultation-textarea" rows="4" />
              <div class="prompt-actions">
                <span class="prompt-chip">青霉素过敏</span>
                <span class="prompt-chip">发热</span>
                <span class="prompt-chip">黄痰</span>
                <span class="spacer"></span>
                <button class="primary" type="button" :disabled="loading.record" @click="generateRecord">{{ loading.record ? "生成中" : "生成病历" }}</button>
              </div>
            </div>
          </label>
          <div class="ai-pane full">
            <div class="inline-toolbar">
              <strong>智能病历草稿</strong>
              <span class="tag info">SOAP</span>
              <span v-if="streamText" class="tag success">可保存</span>
              <span class="spacer"></span>
              <button type="button" :disabled="!streamText" @click="previewOpen = true">预览</button>
            </div>
            <pre class="ai-draft">{{ streamText || "尚未生成。点击\"生成病历\"后展示结构化 SOAP 草稿。" }}</pre>
          </div>
          <label class="field"><span>主诉</span><input v-model.trim="medicalForm.chiefComplaint" /></label>
          <label class="field"><span>诊断</span><input v-model.trim="medicalForm.diagnosis" /></label>
          <label class="field full"><span>现病史</span><textarea v-model.trim="medicalForm.presentIllness" rows="2" /></label>
          <label class="field full"><span>既往史</span><textarea v-model.trim="medicalForm.pastHistory" rows="2" /></label>
          <label class="field full"><span>体格检查</span><textarea v-model.trim="medicalForm.physicalExam" rows="2" /></label>
          <label class="field full"><span>处理建议</span><textarea v-model.trim="medicalForm.treatmentAdvice" rows="2" /></label>
        </div>
        <footer class="footer-actions">
          <button class="primary" type="button" :disabled="loading.record" @click="generateRecord">{{ loading.record ? "生成中" : "生成病历" }}</button>
          <button type="button" :disabled="!canSaveRecord || loading.record" @click="saveConfirmOpen = true">保存病历</button>
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
  </section>
</template>
