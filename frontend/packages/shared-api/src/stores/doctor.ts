import { defineStore } from "pinia";
import { ref } from "vue";
import { doctorApi } from "../api";
import type { MedicalRecord, Notification, Prescription, Registration, TriageRecord } from "../types";

export const useDoctorWorkflowStore = defineStore("doctorWorkflow", () => {
  const registrations = ref<Registration[]>([]);
  const triageRecords = ref<TriageRecord[]>([]);
  const records = ref<MedicalRecord[]>([]);
  const prescriptions = ref<Prescription[]>([]);
  const notifications = ref<Notification[]>([]);
  const streamText = ref("");
  const streamStatus = ref("IDLE");

  async function refresh() {
    const [regResult, triageResult, recordResult, prescriptionResult, notifResult] = await Promise.allSettled([
      doctorApi.registrations(), doctorApi.triageList(), doctorApi.medicalRecords(),
      doctorApi.prescriptions(), doctorApi.notifications(),
    ]);
    if (regResult.status === "fulfilled") registrations.value = regResult.value;
    if (triageResult.status === "fulfilled") triageRecords.value = triageResult.value;
    if (recordResult.status === "fulfilled") records.value = recordResult.value;
    if (prescriptionResult.status === "fulfilled") prescriptions.value = prescriptionResult.value;
    if (notifResult.status === "fulfilled") notifications.value = notifResult.value;
  }

  return { registrations, triageRecords, records, prescriptions, notifications, streamText, streamStatus, refresh };
});
