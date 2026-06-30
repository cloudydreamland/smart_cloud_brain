import { defineStore } from "pinia";
import { ref } from "vue";
import { patientApi } from "../api";
import type { AppointmentSlot, Department, Doctor, Patient, Prescription, Registration, TriageRecord, MedicalRecord } from "../types";

export const usePatientWorkflowStore = defineStore("patientWorkflow", () => {
  const patient = ref<Patient | null>(null);
  const departments = ref<Department[]>([]);
  const doctors = ref<Doctor[]>([]);
  const triage = ref<TriageRecord | null>(null);
  const triageHistory = ref<TriageRecord[]>([]);
  const slots = ref<AppointmentSlot[]>([]);
  const registrations = ref<Registration[]>([]);
  const records = ref<MedicalRecord[]>([]);
  const prescriptions = ref<Prescription[]>([]);

  async function refreshPublicData() {
    departments.value = await patientApi.departments();
    doctors.value = await patientApi.doctors();
  }

  async function refreshAuthenticated() {
    const [info, triages, slotList, registrationList, recordList, prescriptionList] = await Promise.all([
      patientApi.info(), patientApi.triageList(), patientApi.registrationSlots(),
      patientApi.registrations(), patientApi.medicalRecords(), patientApi.prescriptions(),
    ]);
    patient.value = info;
    triageHistory.value = [...triages].sort((left, right) =>
      Number(right.triageRecordId) - Number(left.triageRecordId));
    slots.value = slotList;
    registrations.value = registrationList;
    records.value = recordList;
    prescriptions.value = prescriptionList;
    triage.value = triageHistory.value[0] ?? triage.value;
  }

  return { patient, departments, doctors, triage, triageHistory, slots, registrations, records, prescriptions, refreshPublicData, refreshAuthenticated };
});
