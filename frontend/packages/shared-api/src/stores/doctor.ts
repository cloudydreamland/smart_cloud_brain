import { defineStore } from "pinia";
import { ref } from "vue";
import { doctorApi } from "../api";
import type { Drug, MedicalRecord, Notification, Prescription, Registration, TriageRecord } from "../types";

export const useDoctorWorkflowStore = defineStore("doctorWorkflow", () => {
  const registrations = ref<Registration[]>([]);
  const triageRecords = ref<TriageRecord[]>([]);
  const records = ref<MedicalRecord[]>([]);
  const prescriptions = ref<Prescription[]>([]);
  const drugs = ref<Drug[]>([]);
  const notifications = ref<Notification[]>([]);
  const streamText = ref("");
  const streamStatus = ref("IDLE");

  async function refresh(token: string) {
    const settled = await Promise.allSettled([
      doctorApi.registrations(token), doctorApi.triageList(token), doctorApi.medicalRecords(token),
      doctorApi.prescriptions(token), doctorApi.searchDrugs(token, ""), doctorApi.notifications(token),
    ]);
    const pick = <T>(r: PromiseSettledResult<unknown>, fallback: T[]): T[] =>
      r.status === "fulfilled" && Array.isArray(r.value) ? r.value as T[] : fallback;
    registrations.value = pick<Registration>(settled[0], registrations.value);
    triageRecords.value = pick<TriageRecord>(settled[1], triageRecords.value);
    records.value = pick<MedicalRecord>(settled[2], records.value);
    prescriptions.value = pick<Prescription>(settled[3], prescriptions.value);
    drugs.value = pick<Drug>(settled[4], drugs.value);
    notifications.value = pick<Notification>(settled[5], notifications.value);
  }

  return { registrations, triageRecords, records, prescriptions, drugs, notifications, streamText, streamStatus, refresh };
});
