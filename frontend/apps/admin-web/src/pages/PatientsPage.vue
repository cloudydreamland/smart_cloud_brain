<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import {
  api,
  displayText,
  formatApiError,
  formatDateTime,
  toNumber,
  usePagination,
  type Patient,
  type PatientDetail,
  type PatientSaveRequest,
} from "@smart-cloud-brain/shared-api";
import { DataTable, ErrorState, FormField, Modal, PaginationBar, ScbSelect, StatusTag } from "@smart-cloud-brain/shared-ui";

const rows = ref<Patient[]>([]);
const detail = ref<PatientDetail | null>(null);
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

const genderFilterOptions = [
  { value: "", label: "全部性别" },
  { value: "MALE", label: "男" },
  { value: "FEMALE", label: "女" },
  { value: "UNKNOWN", label: "未知" },
];
const genderFormOptions = [
  { value: "", label: "未知" },
  { value: "MALE", label: "男" },
  { value: "FEMALE", label: "女" },
];

watch([keyword, gender, minAge, maxAge], () => {
  currentPage.value = 1;
  refresh();
});

function asRows(value: unknown) {
  return Array.isArray(value) ? value : [];
}

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    rows.value = await api.patients({
      keyword: keyword.value.trim(),
      gender: gender.value,
      minAge: minAge.value,
      maxAge: maxAge.value,
    }) as Patient[];
  } catch (err) {
    error.value = formatApiError(err, "加载患者列表失败");
  } finally {
    loading.value = false;
  }
}

async function openDetail(item: Patient) {
  loading.value = true;
  error.value = "";
  try {
    detail.value = await api.patientDetail(toNumber(item.id)) as PatientDetail;
    detailOpen.value = true;
  } catch (err) {
    error.value = formatApiError(err, "加载患者详情失败");
  } finally {
    loading.value = false;
  }
}

function openEditor(item: Patient) {
  form.id = toNumber(item.id);
  form.name = displayText(item.name);
  form.gender = displayText(item.gender);
  form.age = toNumber(item.age, 0);
  form.allergyHistory = displayText(item.allergyHistory);
  form.pastHistory = displayText(item.pastHistory);
  editorOpen.value = true;
  notice.value = "";
}

async function save() {
  if (!form.id || !form.name.trim()) {
    error.value = "患者 ID 和姓名不能为空";
    return;
  }
  saving.value = true;
  error.value = "";
  notice.value = "";
  try {
    await api.savePatient({
      id: form.id,
      name: form.name.trim(),
      gender: form.gender || undefined,
      age: form.age ? toNumber(form.age) : undefined,
      allergyHistory: form.allergyHistory || undefined,
      pastHistory: form.pastHistory || undefined,
    });
    editorOpen.value = false;
    notice.value = "患者档案已保存";
    await refresh();
  } catch (err) {
    error.value = formatApiError(err, "保存患者档案失败");
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
        <h2>患者管理</h2>
      </div>
      <div class="toolbar">
        <button type="button" :disabled="loading" @click="refresh">刷新</button>
      </div>
    </header>

    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <div v-if="notice" class="notice success">{{ notice }}</div>

      <div class="admin-filter-row">
        <input v-model.trim="keyword" placeholder="搜索姓名或手机号" @keyup.enter="refresh" />
        <ScbSelect v-model="gender" :options="genderFilterOptions" />
        <input v-model.number="minAge" type="number" min="0" placeholder="最小年龄" />
        <input v-model.number="maxAge" type="number" min="0" placeholder="最大年龄" />
        <button type="button" :disabled="loading" @click="refresh">搜索</button>
      </div>

      <DataTable :rows="filtered" :loading="loading" :error="error" :breakout="true" empty-title="暂无患者">
        <thead>
          <tr>
            <th>编号</th>
            <th>姓名</th>
            <th>手机号</th>
            <th>性别</th>
            <th>年龄</th>
            <th>就诊次数</th>
            <th class="actions-cell">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in pageRows" :key="String(item.id)">
            <td>{{ displayText(item.id) }}</td>
            <td>{{ displayText(item.name) }}</td>
            <td>{{ displayText(item.phone) }}</td>
            <td><StatusTag :status="displayText(item.gender, 'UNKNOWN')" /></td>
            <td>{{ displayText(item.age) }}</td>
            <td>{{ displayText(item.registrationCount, "0") }}</td>
            <td class="actions-cell">
              <div class="toolbar">
                <button type="button" class="action-btn" @click="openDetail(item)">详情</button>
                <button type="button" class="action-btn" @click="openEditor(item)">编辑</button>
              </div>
            </td>
          </tr>
        </tbody>
      </DataTable>
      <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
    </div>

    <Modal :open="editorOpen" title="患者档案" description="维护人口统计和临床背景信息。" @close="editorOpen = false">
      <div class="stack">
        <div class="form-grid">
          <FormField label="姓名"><input v-model.trim="form.name" /></FormField>
          <FormField label="性别">
            <ScbSelect v-model="form.gender" :options="genderFormOptions" />
          </FormField>
          <FormField label="年龄"><input v-model.number="form.age" type="number" min="0" /></FormField>
        </div>
        <FormField label="过敏史"><textarea v-model.trim="form.allergyHistory" /></FormField>
        <FormField label="既往史"><textarea v-model.trim="form.pastHistory" /></FormField>
      </div>
      <template #footer>
        <button type="button" @click="editorOpen = false">取消</button>
        <button type="button" class="primary" :disabled="saving" @click="save">保存</button>
      </template>
    </Modal>

    <Modal :open="detailOpen" title="患者详情" description="按业务模块分组的近期临床记录。" @close="detailOpen = false">
      <div class="stack">
        <div class="metrics">
          <div class="metric"><span>就诊记录</span><strong>{{ asRows(detail?.registrations).length }}</strong></div>
          <div class="metric"><span>分诊记录</span><strong>{{ asRows(detail?.triageRecords).length }}</strong></div>
          <div class="metric"><span>病历</span><strong>{{ asRows(detail?.medicalRecords).length }}</strong></div>
          <div class="metric"><span>处方</span><strong>{{ asRows(detail?.prescriptions).length }}</strong></div>
        </div>
        <div class="list">
          <article v-for="item in asRows(detail?.registrations).slice(0, 5)" :key="`registration-${String(item.id)}`" class="list-row">
            <div class="row-main">
              <strong>就诊 #{{ displayText(item.id) }} / {{ displayText(item.status) }}</strong>
              <p>{{ formatDateTime(item.appointment_time || item.appointmentTime) }}</p>
            </div>
          </article>
        </div>
      </div>
    </Modal>
  </section>
</template>
