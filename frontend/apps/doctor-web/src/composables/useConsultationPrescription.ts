import { computed, reactive, ref } from "vue";
import {
  api,
  displayText,
  formatApiError,
  toNumber,
  type DrugItem,
  type PrescriptionCheckResult,
} from "@smart-cloud-brain/shared-api";

type ToastHandle = {
  success: (title: string, message?: string) => number;
  error: (title: string, message?: string) => number;
};

export function useConsultationPrescription(
  registration: () => { registrationId?: number; patientId?: number } | null,
  medicalForm: { diagnosis: string; pastHistory: string; registrationId: number },
  auth: { session?: { userId?: number } },
  emit: (event: "refresh") => void,
  toastRef: () => ToastHandle | null,
) {
  const prescription = reactive({
    medicalRecordId: 0,
    riskLevel: "UNREVIEWED",
    drugs: [
      { drugName: "阿莫西林胶囊", dosage: "0.5g", frequency: "tid", usageMethod: "口服" },
      { drugName: "氨溴索片", dosage: "30mg", frequency: "tid", usageMethod: "口服" },
    ] as DrugItem[],
  });

  const checkResult = ref<PrescriptionCheckResult | null>(null);
  const prescriptionLoading = ref(false);

  const canCheck = computed(() =>
    prescription.drugs.every(
      (item) =>
        item.drugName.trim() &&
        item.dosage.trim() &&
        item.frequency.trim() &&
        item.usageMethod.trim(),
    ),
  );

  const canCreate = computed(
    () => prescription.medicalRecordId > 0 && canCheck.value,
  );

  function addDrug() {
    prescription.drugs.push({
      drugName: "",
      dosage: "",
      frequency: "",
      usageMethod: "口服",
    });
  }

  function removeDrug(index: number) {
    if (prescription.drugs.length === 1) {
      prescription.drugs[0] = {
        drugName: "",
        dosage: "",
        frequency: "",
        usageMethod: "口服",
      };
      return;
    }
    prescription.drugs.splice(index, 1);
  }

  async function checkPrescription() {
    if (!canCheck.value) {
      toastRef()?.error("操作失败", "请完整填写药品、剂量、频次和用法。");
      return false;
    }
    prescriptionLoading.value = true;
    try {
      checkResult.value = (await api.checkPrescription({
        patientId: toNumber(registration()?.patientId),
        doctorId: auth.session?.userId,
        medicalRecordId: prescription.medicalRecordId || undefined,
        diagnosis: medicalForm.diagnosis,
        pastHistory: medicalForm.pastHistory,
        drugs: prescription.drugs,
      })) as PrescriptionCheckResult;
      prescription.riskLevel = displayText(checkResult.value.riskLevel, "UNREVIEWED");
      return true;
    } catch (err) {
      checkResult.value = null;
      prescription.riskLevel = "UNREVIEWED";
      toastRef()?.error("操作失败", formatApiError(err, "处方风险审核失败，请稍后重试。"));
      return false;
    } finally {
      prescriptionLoading.value = false;
    }
  }

  async function createPrescription() {
    if (!canCreate.value) {
      toastRef()?.error("操作失败", "请先保存病历并完成处方药品录入。");
      return false;
    }
    prescriptionLoading.value = true;
    try {
      await api.createPrescription({
        patientId: toNumber(registration()?.patientId),
        medicalRecordId: prescription.medicalRecordId,
        registrationId: toNumber(registration()?.registrationId),
        riskLevel: prescription.riskLevel,
        drugs: prescription.drugs,
      });
      emit("refresh");
      toastRef()?.success("操作完成", "处方已创建。");
      return true;
    } catch (err) {
      toastRef()?.error("操作失败", formatApiError(err, "处方创建失败，请稍后重试。"));
      return false;
    } finally {
      prescriptionLoading.value = false;
    }
  }

  return {
    prescription,
    checkResult,
    canCheck,
    canCreate,
    prescriptionLoading,
    addDrug,
    removeDrug,
    checkPrescription,
    createPrescription,
  };
}
