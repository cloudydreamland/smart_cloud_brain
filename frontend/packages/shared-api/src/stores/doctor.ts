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
    const settled = await Promise.allSettled([
      doctorApi.registrations(), doctorApi.triageList(), doctorApi.medicalRecords(),
      doctorApi.prescriptions(), doctorApi.notifications(),
    ]);
    const pick = <T>(r: PromiseSettledResult<unknown>, fallback: T[]): T[] =>
      r.status === "fulfilled" && Array.isArray(r.value) ? r.value as T[] : fallback;
    registrations.value = pick<Registration>(settled[0], registrations.value);
    triageRecords.value = pick<TriageRecord>(settled[1], triageRecords.value);
    records.value = pick<MedicalRecord>(settled[2], records.value);
    prescriptions.value = pick<Prescription>(settled[3], prescriptions.value);
    notifications.value = pick<Notification>(settled[4], notifications.value);
  }

  return { registrations, triageRecords, records, prescriptions, notifications, streamText, streamStatus, refresh };
});
