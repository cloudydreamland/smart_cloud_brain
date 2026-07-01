import { computed, reactive, ref, watch } from "vue";
import {
  api,
  displayText,
  formatApiError,
  medicalRecordStreamUrl,
  toNumber,
  type MedicalRecordDraft,
} from "@smart-cloud-brain/shared-api";
import { formatAiDraft } from "../doctorPresentation";

type StreamPayload = MedicalRecordDraft & {
  data?: unknown;
  message?: string;
  soapContent?: string;
  text?: string;
};

type ToastHandle = {
  success: (title: string, message?: string) => number;
  error: (title: string, message?: string) => number;
  info: (title: string, message?: string) => number;
};

export function useConsultationRecord(
  registrationId: () => string,
  registration: () => { registrationId?: number; triageRecordId?: number; patientId?: number } | null,
  triage: () => { chiefComplaint?: string; pastHistory?: string; departmentCode?: string } | null,
  auth: { token: () => string },
  emit: (event: "refresh") => void,
  toastRef: () => ToastHandle | null,
) {
  const medicalForm = reactive({
    registrationId: toNumber(registrationId()),
    chiefComplaint: "",
    presentIllness: "",
    pastHistory: "",
    physicalExam: "",
    diagnosis: "",
    treatmentAdvice: "",
    aiGenerated: true,
  });

  const dialogueText = ref("");
  const streamText = ref("");
  const streamStatus = ref("IDLE");
  const recordAiProvider = ref("Dify");
  const recordAiModel = ref("medical-record-v2");
  const recordMode = ref<"ai" | "manual">("ai");
  const aiDraftMode = ref<"preview" | "auto">("preview");
  const recordLoading = ref(false);
  const lastDraft = ref<StreamPayload | null>(null);

  let recordStream: AbortController | null = null;

  watch(recordMode, (mode) => {
    medicalForm.aiGenerated = mode === "ai";
  });

  const canSaveRecord = computed(
    () =>
      medicalForm.registrationId > 0 &&
      medicalForm.chiefComplaint.trim() &&
      medicalForm.diagnosis.trim(),
  );

  const suggestedChips = computed(() => {
    const chips: string[] = [];
    const chief = triage()?.chiefComplaint || "";
    const history = triage()?.pastHistory || "";
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

  function insertChip(text: string) {
    const sep = dialogueText.value.trim() ? "，" : "";
    dialogueText.value += `${sep}${text}`;
  }

  function applyRegistration() {
    medicalForm.registrationId = toNumber(
      registrationId(),
      toNumber(registration()?.registrationId),
    );
    medicalForm.chiefComplaint = displayText(triage()?.chiefComplaint, "");
    medicalForm.pastHistory = displayText(triage()?.pastHistory, "");
    if (!dialogueText.value.trim() && triage()) {
      const parts: string[] = [];
      const chief = triage()!.chiefComplaint || "";
      const history = triage()!.pastHistory || "";
      if (chief) parts.push(chief);
      if (history) parts.push(`既往${history}`);
      dialogueText.value = parts.join("。");
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
    return nested && typeof nested === "object" && !Array.isArray(nested)
      ? (nested as StreamPayload)
      : payload;
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
    try {
      return JSON.parse(raw) as StreamPayload;
    } catch {
      return { text: raw };
    }
  }

  function handleStreamBlock(block: string) {
    const lines = block.split(/\r?\n/);
    const eventName = lines
      .find((line) => line.startsWith("event:"))
      ?.slice("event:".length)
      .trim();
    const dataText = lines
      .filter((line) => line.startsWith("data:"))
      .map((line) => line.slice("data:".length).trim())
      .join("\n");
    if (!dataText) return;
    if (eventName === "delta") {
      const data = parseEventData(dataText);
      streamText.value += `${displayText(data.text, dataText)}\n`;
    } else if (eventName === "structured") {
      const draft = normalizeDraft(parseEventData(dataText));
      lastDraft.value = draft;
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
    if (!dialogueText.value.trim()) {
      toastRef()?.error("操作失败", "请先填写问诊文本。");
      return;
    }
    recordLoading.value = true;
    streamText.value = "";
    streamStatus.value = "GENERATING";
    try {
      recordStream?.abort();
      recordStream = new AbortController();
      const response = await fetch(
        medicalRecordStreamUrl(
          medicalForm.registrationId,
          dialogueText.value,
          displayText(triage()?.departmentCode, ""),
        ),
        {
          headers: { Authorization: `Bearer ${auth.token()}` },
          signal: recordStream.signal,
        },
      );
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
      toastRef()?.success("操作完成", "病历草稿已生成，请确认后保存。");
    } catch (err) {
      streamText.value = formatAiDraft();
      streamStatus.value = "DRAFT_READY";
      toastRef()?.info(
        "提示",
        `${formatApiError(err, "病历生成服务不可用")}；已保留本地空白草稿，请确认后再保存。`,
      );
    } finally {
      recordLoading.value = false;
      recordStream = null;
    }
  }

  async function saveRecord() {
    if (!canSaveRecord.value) {
      toastRef()?.error("操作失败", "主诉和诊断为必填项。");
      return;
    }
    recordLoading.value = true;
    try {
      const saved = await api.saveMedicalRecord({ ...medicalForm });
      emit("refresh");
      toastRef()?.success("操作完成", "病历已保存，可以继续处方录入和审核。");
      return toNumber(saved.medicalRecordId);
    } catch (err) {
      toastRef()?.error("操作失败", formatApiError(err, "病历保存失败，请稍后重试。"));
      return 0;
    } finally {
      recordLoading.value = false;
    }
  }

  return {
    medicalForm,
    dialogueText,
    streamText,
    streamStatus,
    recordAiProvider,
    recordAiModel,
    recordMode,
    aiDraftMode,
    recordLoading,
    canSaveRecord,
    suggestedChips,
    insertChip,
    applyRegistration,
    applyDraft,
    lastDraft,
    generateRecord,
    saveRecord,
  };
}
