<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import {
  api,
  fieldText,
  formatApiError,
  toNumber,
  useAuthStore,
  usePagination,
  type DataRow,
  type PatientSaveRequest,
} from "@smart-cloud-brain/shared-api";
import { DataTable, ErrorState, FormField, Modal, PaginationBar, StatusTag } from "@smart-cloud-brain/shared-ui";

const auth = useAuthStore();
const rows = ref<DataRow[]>([]);
const detail = ref<DataRow | null>(null);
const keyword = ref("");
const gender = ref("");
const minAge = ref<number | undefined>();
const maxAge = ref<number | undefined>();
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const notice = ref("");
const editorOpen = ref(false);
const detailOpen = ref(false);

const form = reactive<PatientSaveRequest>({
  id: 0,
  name: "",
  gender: "",
  age: 0,
  allergyHistory: "",
  pastHistory: "",
});

const filtered = computed(() => rows.value);
const { currentPage, pageSize, total, pageRows } = usePagination(filtered, 8);

watch([keyword, gender, minAge, maxAge], () => {
  currentPage.value = 1;
});

function asRows(value: unknown): DataRow[] {
  return Array.isArray(value) ? value.filter((item): item is DataRow => item !== null && typeof item === "object") : [];
}

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    rows.value = await api.patients(auth.token(), {
      keyword: keyword.value.trim(),
      gender: gender.value,
      minAge: minAge.value,
      maxAge: maxAge.value,
    });
  } catch (err) {
    error.value = formatApiError(err, "Patient list failed");
  } finally {
    loading.value = false;
  }
}

async function openDetail(item: DataRow) {
  loading.value = true;
  error.value = "";
  try {
    detail.value = await api.patientDetail(auth.token(), toNumber(item.id));
    detailOpen.value = true;
  } catch (err) {
    error.value = formatApiError(err, "Patient detail failed");
  } finally {
    loading.value = false;
  }
}

function openEditor(item: DataRow) {
  form.id = toNumber(item.id);
  form.name = fieldText(item, "name");
  form.gender = fieldText(item, "gender");
  form.age = toNumber(item.age, 0);
  form.allergyHistory = fieldText(item, "allergyHistory");
  form.pastHistory = fieldText(item, "pastHistory");
  editorOpen.value = true;
  notice.value = "";
}

async function save() {
  if (!form.id || !form.name.trim()) {
    error.value = "Patient ID and name are required";
    return;
  }
  saving.value = true;
  error.value = "";
  notice.value = "";
  try {
    await api.savePatient(auth.token(), {
      id: form.id,
      name: form.name.trim(),
      gender: form.gender || undefined,
      age: form.age ? toNumber(form.age) : undefined,
      allergyHistory: form.allergyHistory || undefined,
      pastHistory: form.pastHistory || undefined,
    });
    editorOpen.value = false;
    notice.value = "Patient profile saved";
    await refresh();
  } catch (err) {
    error.value = formatApiError(err, "Patient save failed");
  } finally {
    saving.value = false;
  }
}

onMounted(refresh);
</script>

<template>
  <section class="panel">
    <header class="panel-header">
      <div class="panel-title">
        <p class="eyebrow">Clinical Master Data</p>
        <h2>Patient Management</h2>
        <p>Maintain patient profiles and inspect registration, triage, record and prescription history.</p>
      </div>
      <div class="toolbar">
        <button type="button" :disabled="loading" @click="refresh">Refresh</button>
      </div>
    </header>

    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <div v-if="notice" class="notice success">{{ notice }}</div>

      <div class="admin-filter-row">
        <input v-model.trim="keyword" placeholder="Search name or phone" @keyup.enter="refresh" />
        <select v-model="gender">
          <option value="">All genders</option>
          <option value="MALE">Male</option>
          <option value="FEMALE">Female</option>
          <option value="UNKNOWN">Unknown</option>
        </select>
        <input v-model.number="minAge" type="number" min="0" placeholder="Min age" />
        <input v-model.number="maxAge" type="number" min="0" placeholder="Max age" />
        <button type="button" :disabled="loading" @click="refresh">Search</button>
      </div>

      <DataTable :rows="filtered" :loading="loading" :error="error" empty-title="No patients">
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Phone</th>
            <th>Gender</th>
            <th>Age</th>
            <th>Registrations</th>
            <th class="actions-cell">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in pageRows" :key="String(item.id)">
            <td>{{ fieldText(item, "id") }}</td>
            <td>{{ fieldText(item, "name") }}</td>
            <td>{{ fieldText(item, "phone") }}</td>
            <td><StatusTag :status="fieldText(item, 'gender', 'UNKNOWN')" /></td>
            <td>{{ fieldText(item, "age", "-") }}</td>
            <td>{{ fieldText(item, "registrationCount", "0") }}</td>
            <td class="toolbar">
              <button type="button" @click="openDetail(item)">Detail</button>
              <button type="button" @click="openEditor(item)">Edit</button>
            </td>
          </tr>
        </tbody>
      </DataTable>
      <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
    </div>

    <Modal :open="editorOpen" title="Patient Profile" description="Update demographic and clinical background fields." @close="editorOpen = false">
      <div class="stack">
        <div class="form-grid">
          <FormField label="Name"><input v-model.trim="form.name" /></FormField>
          <FormField label="Gender">
            <select v-model="form.gender">
              <option value="">Unknown</option>
              <option value="MALE">Male</option>
              <option value="FEMALE">Female</option>
            </select>
          </FormField>
          <FormField label="Age"><input v-model.number="form.age" type="number" min="0" /></FormField>
        </div>
        <FormField label="Allergy history"><textarea v-model.trim="form.allergyHistory" /></FormField>
        <FormField label="Past history"><textarea v-model.trim="form.pastHistory" /></FormField>
      </div>
      <template #footer>
        <button type="button" @click="editorOpen = false">Cancel</button>
        <button type="button" class="primary" :disabled="saving" @click="save">Save</button>
      </template>
    </Modal>

    <Modal :open="detailOpen" title="Patient Detail" description="Recent clinical history grouped by business module." @close="detailOpen = false">
      <div class="stack">
        <div class="metrics">
          <div class="metric"><span>Registrations</span><strong>{{ asRows(detail?.registrations).length }}</strong></div>
          <div class="metric"><span>Triage</span><strong>{{ asRows(detail?.triageRecords).length }}</strong></div>
          <div class="metric"><span>Records</span><strong>{{ asRows(detail?.medicalRecords).length }}</strong></div>
          <div class="metric"><span>Prescriptions</span><strong>{{ asRows(detail?.prescriptions).length }}</strong></div>
        </div>
        <div class="list">
          <article v-for="item in asRows(detail?.registrations).slice(0, 5)" :key="`registration-${String(item.id)}`" class="list-row">
            <div class="row-main">
              <strong>Registration #{{ fieldText(item, "id") }} / {{ fieldText(item, "status") }}</strong>
              <p>{{ fieldText(item, "appointment_time", fieldText(item, "appointmentTime", "")) }}</p>
            </div>
          </article>
        </div>
      </div>
    </Modal>
  </section>
</template>
