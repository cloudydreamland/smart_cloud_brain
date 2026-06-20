import { defineStore } from "pinia";
import { ref } from "vue";
import { patientApi } from "../api";
import type { DataRow } from "../types";

export const usePatientWorkflowStore = defineStore("patientWorkflow", () => {
  const patient = ref<DataRow | null>(null);
  const departments = ref<DataRow[]>([]);
  const doctors = ref<DataRow[]>([]);
  const triage = ref<DataRow | null>(null);
  const triageHistory = ref<DataRow[]>([]);
  const slots = ref<DataRow[]>([]);
  const registrations = ref<DataRow[]>([]);
  const records = ref<DataRow[]>([]);
  const prescriptions = ref<DataRow[]>([]);

  async function refreshPublicData() {
    departments.value = await patientApi.departments();
    doctors.value = await patientApi.doctors();
  }

  async function refreshAuthenticated(token: string) {
    const [info, triages, slotList, registrationList, recordList, prescriptionList] = await Promise.all([
      patientApi.info(token), patientApi.triageList(token), patientApi.registrationSlots(token),
      patientApi.registrations(token), patientApi.medicalRecords(token), patientApi.prescriptions(token),
    ]);
    patient.value = info;
    triageHistory.value = triages;
    slots.value = slotList;
    registrations.value = registrationList;
    records.value = recordList;
    prescriptions.value = prescriptionList;
    triage.value = triages[0] ?? triage.value;
  }

  return { patient, departments, doctors, triage, triageHistory, slots, registrations, records, prescriptions, refreshPublicData, refreshAuthenticated };
});
