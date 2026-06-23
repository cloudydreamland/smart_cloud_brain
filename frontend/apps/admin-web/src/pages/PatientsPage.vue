<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { api, fieldText, formatApiError, toNumber, useAuthStore, usePagination, type DataRow, type PatientSaveRequest } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, FormField, Modal, PaginationBar } from "@smart-cloud-brain/shared-ui";

const auth = useAuthStore();
const rows = ref<DataRow[]>([]);
const selected = ref<DataRow | null>(null);
const editorOpen = ref(false);
const detailOpen = ref(false);
const loading = ref(false);
const error = ref("");
const filter = reactive({ keyword: "", gender: "", minAge: "", maxAge: "" });
const form = reactive<PatientSaveRequest>({ id: 0, name: "", gender: "", age: 0, allergyHistory: "", pastHistory: "" });
const { currentPage, pageSize, total, pageRows } = usePagination(rows, 8);
const selectedRegistrations = computed(() => (selected.value?.registrations as DataRow[] | undefined) ?? []);
const selectedTriage = computed(() => (selected.value?.triageRecords as DataRow[] | undefined) ?? []);
const selectedRecords = computed(() => (selected.value?.medicalRecords as DataRow[] | undefined) ?? []);
const selectedPrescriptions = computed(() => (selected.value?.prescriptions as DataRow[] | undefined) ?? []);

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    rows.value = await api.patients(auth.token(), {
      keyword: filter.keyword,
      gender: filter.gender,
      minAge: filter.minAge ? Number(filter.minAge) : undefined,
      maxAge: filter.maxAge ? Number(filter.maxAge) : undefined,
    });
  } catch (err) {
    error.value = formatApiError(err, "Patient list failed");
  } finally {
    loading.value = false;
  }
}

async function detail(item: DataRow) {
  loading.value = true;
  error.value = "";
  try {
    selected.value = await api.patientDetail(auth.token(), toNumber(item.id));
    detailOpen.value = true;
  } catch (err) {
    error.value = formatApiError(err, "Patient detail failed");
  } finally {
    loading.value = false;
  }
}

function openEditor(item: DataRow) {
  form.id = toNumber(item.id);
  form.name = fieldText(item, "name", "");
  form.gender = fieldText(item, "gender", "");
  form.age = toNumber(item.age, 0);
  form.allergyHistory = fieldText(item, "allergyHistory", "");
  form.pastHistory = fieldText(item, "pastHistory", "");
  editorOpen.value = true;
}

async function save() {
  loading.value = true;
  error.value = "";
  try {
    await api.savePatient(auth.token(), { ...form, id: toNumber(form.id) });
    editorOpen.value = false;
    await refresh();
  } catch (err) {
    error.value = formatApiError(err, "Patient save failed");
  } finally {
    loading.value = false;
  }
}

refresh();
</script>

<template>
  <section class="panel">
    <header class="panel-header"><div class="panel-title"><p class="eyebrow">Patient archive</p><h2>Patient Information Management</h2><p>Search profiles and inspect real triage, registration, record and prescription history.</p></div></header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <div class="admin-filter-row">
        <input v-model.trim="filter.keyword" placeholder="Name or phone" />
        <select v-model="filter.gender"><option value="">All gender</option><option value="MALE">Male</option><option value="FEMALE">Female</option></select>
        <input v-model.trim="filter.minAge" type="number" placeholder="Min age" />
        <input v-model.trim="filter.maxAge" type="number" placeholder="Max age" />
        <button class="primary" type="button" :disabled="loading" @click="refresh">Search</button>
      </div>
      <div v-if="rows.length" class="table-scroll">
        <table class="data-table">
          <thead><tr><th>ID</th><th>Name</th><th>Phone</th><th>Gender</th><th>Age</th><th>Visits</th><th class="actions-cell">Actions</th></tr></thead>
          <tbody>
            <tr v-for="item in pageRows" :key="String(item.id)">
              <td>#{{ fieldText(item, "id") }}</td>
              <td>{{ fieldText(item, "name") }}</td>
              <td>{{ fieldText(item, "phone") }}</td>
              <td>{{ fieldText(item, "gender") }}</td>
              <td>{{ fieldText(item, "age") }}</td>
              <td>{{ fieldText(item, "registrationCount", "0") }}</td>
              <td class="toolbar"><button type="button" @click="detail(item)">Detail</button><button type="button" @click="openEditor(item)">Edit</button></td>
            </tr>
          </tbody>
        </table>
        <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
      </div>
      <EmptyState v-else title="No patients" />
    </div>
    <Modal :open="editorOpen" title="Patient profile" description="Update non-account clinical profile fields." @close="editorOpen = false">
      <div class="stack">
        <div class="form-grid">
          <FormField label="Name"><input v-model.trim="form.name" /></FormField>
          <FormField label="Gender"><select v-model="form.gender"><option value="">Unknown</option><option value="MALE">Male</option><option value="FEMALE">Female</option></select></FormField>
          <FormField label="Age"><input v-model.number="form.age" type="number" min="0" max="130" /></FormField>
        </div>
        <FormField label="Allergy history"><textarea v-model.trim="form.allergyHistory" /></FormField>
        <FormField label="Past history"><textarea v-model.trim="form.pastHistory" /></FormField>
      </div>
      <template #footer><button type="button" @click="editorOpen = false">Cancel</button><button class="primary" type="button" :disabled="loading" @click="save">Save</button></template>
    </Modal>
    <Modal :open="detailOpen" title="Patient history" description="Real records from triage, registration, medical record and prescription tables." @close="detailOpen = false">
      <div class="stack">
        <strong>{{ fieldText(selected, "name") }} / {{ fieldText(selected, "phone") }}</strong>
        <section class="panel">
          <header class="panel-header"><div class="panel-title"><h3>Registrations</h3></div></header>
          <div class="list"><article v-for="item in selectedRegistrations.slice(0, 5)" :key="String(item.id)" class="list-row"><div class="row-main"><strong>#{{ fieldText(item, "id") }} {{ fieldText(item, "status") }}</strong><p>{{ fieldText(item, "appointment_time") }}</p></div></article></div>
        </section>
        <section class="panel">
          <header class="panel-header"><div class="panel-title"><h3>Triage</h3></div></header>
          <div class="list"><article v-for="item in selectedTriage.slice(0, 5)" :key="String(item.id)" class="list-row"><div class="row-main"><strong>{{ fieldText(item, "recommended_department") }}</strong><p>{{ fieldText(item, "chief_complaint") }}</p></div></article></div>
        </section>
        <section class="panel">
          <header class="panel-header"><div class="panel-title"><h3>Medical records / prescriptions</h3></div></header>
          <div class="summary-strip">
            <div class="summary-item"><span>Records</span><strong>{{ selectedRecords.length }}</strong></div>
            <div class="summary-item"><span>Prescriptions</span><strong>{{ selectedPrescriptions.length }}</strong></div>
          </div>
        </section>
      </div>
    </Modal>
  </section>
</template>
