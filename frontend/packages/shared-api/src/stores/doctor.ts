import { defineStore } from "pinia";
import { ref } from "vue";
import { doctorApi } from "../api";
import type { DataRow } from "../types";

export const useDoctorWorkflowStore = defineStore("doctorWorkflow", () => {
  const registrations = ref<DataRow[]>([]);
  const triageRecords = ref<DataRow[]>([]);
  const records = ref<DataRow[]>([]);
  const prescriptions = ref<DataRow[]>([]);
  const drugs = ref<DataRow[]>([]);
  const notifications = ref<DataRow[]>([]);
  const streamText = ref("");
  const streamStatus = ref("IDLE");

  async function refresh(token: string) {
    const [registrationList, triageList, recordList, prescriptionList, drugList, notificationList] = await Promise.all([
      doctorApi.registrations(token), doctorApi.triageList(token), doctorApi.medicalRecords(token),
      doctorApi.prescriptions(token), doctorApi.searchDrugs(token, ""), doctorApi.notifications(token),
    ]);
    registrations.value = registrationList;
    triageRecords.value = triageList;
    records.value = recordList;
    prescriptions.value = prescriptionList;
    drugs.value = drugList;
    notifications.value = notificationList;
  }

  return { registrations, triageRecords, records, prescriptions, drugs, notifications, streamText, streamStatus, refresh };
});
