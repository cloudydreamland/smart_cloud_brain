<script setup lang="ts">
import { computed, inject, reactive, ref, type Ref } from "vue";
import { storeToRefs } from "pinia";
import {
  api,
  displayText,
  formatApiError,
  statusText,
  statusClass,
  toNumber,
  usePagination,
  useAdminWorkflowStore,
  type Department,
  type Doctor,
  type Drug,
} from "@smart-cloud-brain/shared-api";
import { DataTable, ErrorState, FormField, Modal, PaginationBar, ScbSelect, StatusTag, Toast } from "@smart-cloud-brain/shared-ui";

type Entity = "department" | "doctor" | "drug";
type FieldType = "text" | "password" | "number" | "textarea" | "checkbox" | "department-select";
type FieldConfig = [key: string, label: string, type: FieldType];
type DictOption = { value: string; label: string };
type CatalogRow = Department | Doctor | Drug;

const props = defineProps<{ entity: Entity }>();
const emit = defineEmits<{ refresh: [] }>();
const workflow = useAdminWorkflowStore();
const { departments, doctors, drugs } = storeToRefs(workflow);
const keyword = ref("");
const statusFilter = ref("ALL");
const statusOptions = [
  { value: "ALL", label: "全部" },
  { value: "ENABLED", label: "启用" },
  { value: "DISABLED", label: "停用" },
];
const error = ref("");
const toast = inject<Ref<InstanceType<typeof Toast>>>("toast");
const saving = ref(false);
const loading = ref(false);
const loaded = ref(false);
const editorOpen = ref(false);
const form = reactive<Record<string, string | number | boolean | undefined>>({});

const fallbackDicts: Record<string, DictOption[]> = {
  SYSTEM_STATUS: [
    { value: "ENABLED", label: "启用" },
    { value: "DISABLED", label: "停用" },
  ],
  REGISTRATION_STATUS: [
    { value: "CREATED", label: "待接诊" },
    { value: "CONFIRMED", label: "已确认" },
    { value: "COMPLETED", label: "已完成" },
    { value: "CANCELLED", label: "已取消" },
  ],
  TRIAGE_STATUS: [
    { value: "AI_RECOMMENDED", label: "AI 已推荐" },
    { value: "ASSIGNED", label: "已分配医生" },
    { value: "MANUAL_REQUIRED", label: "待人工处理" },
    { value: "CLOSED", label: "已关闭" },
  ],
  RISK_LEVEL: [
    { value: "UNREVIEWED", label: "未审核" },
    { value: "LOW", label: "低风险" },
    { value: "MEDIUM", label: "中风险" },
    { value: "HIGH", label: "高风险" },
  ],
  READ_STATUS: [
    { value: "UNREAD", label: "未读" },
    { value: "READ", label: "已读" },
  ],
  GENDER: [
    { value: "MALE", label: "男" },
    { value: "FEMALE", label: "女" },
    { value: "UNKNOWN", label: "未知" },
  ],
};

const configs: Record<Entity, {
  title: string;
  description: string;
  rows: () => CatalogRow[];
  keys: string[];
  columns: string[];
  columnLabels: Record<string, string>;
  fields: FieldConfig[];
}> = {
  department: {
    title: "科室维护",
    description: "维护科室编码、名称和说明。",
    rows: () => departments.value,
    keys: ["code", "name", "description"],
    columns: ["id", "code", "name", "description"],
    columnLabels: { id: "编号", code: "科室编码", name: "科室名称", description: "说明" },
    fields: [["code", "科室编码", "text"], ["name", "科室名称", "text"], ["description", "说明", "textarea"]],
  },
  doctor: {
    title: "医生维护",
    description: "维护医生账号、科室、职称、专长和状态。",
    rows: () => doctors.value,
    keys: ["name", "phone", "departmentName", "title", "specialty"],
    columns: ["id", "name", "phone", "departmentName", "title", "status"],
    columnLabels: { id: "编号", name: "姓名", phone: "手机号", departmentName: "科室", title: "职称", status: "状态" },
    fields: [["name", "姓名", "text"], ["phone", "手机号", "text"], ["password", "新密码", "password"], ["departmentId", "科室", "department-select"], ["title", "职称", "text"], ["specialty", "专长", "textarea"], ["status", "状态", "text"]],
  },
  drug: {
    title: "药品维护",
    description: "维护药品、规格、禁忌和相互作用规则。",
    rows: () => drugs.value,
    keys: ["name", "specification", "contraindication", "status"],
    columns: ["id", "name", "specification", "status"],
    columnLabels: { id: "编号", name: "药品名称", specification: "规格", status: "状态" },
    fields: [["name", "药品名称", "text"], ["specification", "规格", "text"], ["contraindication", "禁忌", "textarea"], ["interactionRule", "相互作用规则", "textarea"], ["status", "状态", "text"]],
  },
};

const config = computed(() => configs[props.entity]);

/* ---------- 实体保存策略（替代散落的 if 链） ---------- */
const saveHandlers: Record<Entity, () => Promise<void>> = {
  department: () => api.saveDepartment(form as never),
  doctor:     () => api.saveDoctor(form as never),
  drug:       () => api.saveDrug(form as never),
};
const rows = computed(() => {
  const q = keyword.value.trim().toLowerCase();
  const source = config.value.rows();
  let result = source;
  if (props.entity === "drug" && statusFilter.value !== "ALL") {
    result = result.filter((item) => String(getCatalogValue(item, "status") ?? "").toUpperCase() === statusFilter.value);
  }
  if (q) {
    result = result.filter((item) => config.value.keys.some((key) => displayText(getCatalogValue(item, key), "").toLowerCase().includes(q)));
  }
  return result;
});
const { currentPage, pageSize, total, pageRows } = usePagination(rows, 8);

function getCatalogValue(row: CatalogRow, key: string) {
  return (row as Record<string, unknown>)[key];
}

const catalogDeptOptions = computed(() => [
  { value: 0, label: "请选择科室" },
  ...departments.value.map((d) => ({ value: toNumber(d.id), label: displayText(d.name) })),
]);

function dictionaryOptions(dictType: string): DictOption[] {
  return fallbackDicts[dictType] ?? [];
}

function firstDictValue(dictType: string, fallback: string) {
  return dictionaryOptions(dictType)[0]?.value ?? fallback;
}

function fieldDictType(key: string) {
  if (key === "status") return "SYSTEM_STATUS";
  return "";
}

function shouldUseDictionarySelect(key: string) {
  return Boolean(fieldDictType(key));
}

function dictionaryLabel(dictType: string, value: unknown) {
  const raw = String(value ?? "");
  return dictionaryOptions(dictType).find((item) => item.value === raw)?.label ?? statusText(raw, raw);
}

function departmentCodeOptions() {
  const options = departments.value
    .map((department) => ({
      value: displayText(department.code),
      label: `${displayText(department.name)}（${displayText(department.code)}）`,
    }))
    .filter((item) => item.value);
  return [{ value: "GENERAL", label: "通用（GENERAL）" }, ...options];
}

function displayCell(column: string, value: unknown) {
  if (column === "status") return dictionaryLabel("SYSTEM_STATUS", value);
  if (column === "departmentCode") return departmentCodeOptions().find((item) => item.value === String(value ?? ""))?.label ?? String(value ?? "");
  return String(value ?? "");
}

async function refresh(silent = false, showLoading = true) {
  if (showLoading) loading.value = true;
  error.value = "";
  try {
    await workflow.refresh();
    loaded.value = true;
    if (!silent) toast?.value?.success("数据已刷新", `${config.value.title}数据已同步最新状态。`);
  } catch (err) {
    error.value = formatApiError(err, "列表加载失败");
    if (!silent) toast?.value?.error("刷新失败", "请检查网络后重试。");
  } finally {
    if (showLoading) loading.value = false;
  }
}

function openEditor(item?: CatalogRow) {
  error.value = "";
  Object.keys(form).forEach((key) => delete form[key]);
  if (item) {
    form.id = toNumber(getCatalogValue(item, "id")) || undefined;
    config.value.fields.forEach(([key]) => { form[key] = getCatalogValue(item, key) as string | number | boolean | undefined; });
  } else {
    config.value.fields.forEach(([key, , type]) => {
      form[key] = type === "checkbox" ? true : ["number", "department-select"].includes(type) ? 0 : "";
    });
    initEntityDefaults();
  }
  editorOpen.value = true;
}

/** 各实体新增时的默认值填充 */
function initEntityDefaults() {
  if (props.entity === "doctor") form.departmentId = toNumber(departments.value[0]?.id);
  if ("status" in form) form.status = firstDictValue("SYSTEM_STATUS", "ENABLED");
}

function requireFields() {
  const required: Record<Entity, string[]> = {
    department: ["code", "name"],
    doctor: ["name", "phone", "departmentId"],
    drug: ["name"],
  };
  const missing = required[props.entity].find((key) => form[key] === undefined || form[key] === "" || form[key] === 0);
  return missing ? `请填写 ${fieldLabel(missing)}` : "";
}

function fieldLabel(key: string) {
  return config.value.fields.find(([fieldKey]) => fieldKey === key)?.[1] ?? key;
}

function validateDoctorForm() {
  if (props.entity !== "doctor") return "";
  form.phone = String(form.phone ?? "").trim();
  form.departmentId = toNumber(form.departmentId);
  if (!/^\d{11}$/.test(String(form.phone))) return "手机号必须为11位数字";
  if (!form.departmentId) return "请选择科室";
  return "";
}

function fieldHint(key: string) {
  if (props.entity === "doctor" && key === "phone") return "请输入11位数字";
  return "";
}

async function save() {
  const invalid = requireFields() || validateDoctorForm();
  if (invalid) {
    error.value = invalid;
    return;
  }
  saving.value = true;
  error.value = "";
  try {
    await saveHandlers[props.entity]();
    editorOpen.value = false;
    toast?.value?.success("保存成功", `${config.value.title}已保存`);
    emit("refresh");
    await refresh(true);
  } catch (err) {
    error.value = formatApiError(err, "保存失败");
  } finally {
    saving.value = false;
  }
}

refresh(true, false);
</script>

<template>
  <section class="catalog-layout">
    <section class="panel">
      <header class="panel-header">
        <div class="panel-title"><h2>{{ config.title }}</h2></div>
        <div class="toolbar">
          <button type="button" :disabled="loading" @click="refresh()">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" :class="{ 'spin': loading }"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
            刷新
          </button>
          <button type="button" class="primary" @click="openEditor()">新增</button>
        </div>
      </header>
      <div class="panel-body stack">
        <ErrorState v-if="error" :message="error" />
        <div class="admin-filter-row">
          <input v-model.trim="keyword" placeholder="搜索当前表" />
          <div v-if="props.entity === 'drug'" class="status-segment" role="tablist" aria-label="状态筛选">
            <button v-for="opt in statusOptions" :key="opt.value" type="button" role="tab" :aria-selected="statusFilter === opt.value" :class="{ active: statusFilter === opt.value }" @click="statusFilter = opt.value">{{ opt.label }}</button>
          </div>
        </div>
        <DataTable :rows="rows" :loading="!loaded && loading" :error="error" :breakout="true" empty-title="暂无数据" empty-message="当前筛选条件下没有记录。">
          <thead><tr><th v-for="column in config.columns" :key="column">{{ config.columnLabels[column] ?? column }}</th><th class="actions-cell">操作</th></tr></thead>
          <tbody>
            <tr v-for="item in pageRows" :key="String(item.id)">
              <td v-for="column in config.columns" :key="column">
                <StatusTag v-if="column === 'status'" :status="displayCell(column, getCatalogValue(item, column))" :tone="statusClass(getCatalogValue(item, column))" />
                <StatusTag v-else-if="column === 'enabled'" :status="displayText(getCatalogValue(item, column))" :tone="statusClass(getCatalogValue(item, column))" />
                <span v-else>{{ displayCell(column, getCatalogValue(item, column)) }}</span>
              </td>
              <td><button type="button" class="action-btn" @click="openEditor(item)">编辑</button></td>
            </tr>
          </tbody>
        </DataTable>
        <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
      </div>
    </section>
    <Modal :open="editorOpen" :title="config.title" @close="editorOpen = false">
      <div class="stack">
        <FormField v-for="[key, label, type] in config.fields" :key="key" :label="label" :hint="fieldHint(key)">
          <textarea v-if="type === 'textarea'" v-model="form[key]" />
          <input v-else-if="type === 'checkbox'" v-model="form[key]" type="checkbox" />
          <ScbSelect v-else-if="shouldUseDictionarySelect(key)" v-model="form[key]" :options="dictionaryOptions(fieldDictType(key))" />
          <ScbSelect v-else-if="type === 'department-select'" v-model="form[key]" :options="catalogDeptOptions" />
          <ScbSelect v-else-if="key === 'departmentCode'" v-model="form[key]" :options="departmentCodeOptions()" />
          <input v-else-if="type === 'number'" v-model.number="form[key]" type="number" />
          <input v-else v-model="form[key]" :type="type" />
        </FormField>
      </div>
      <template #footer>
        <button type="button" :disabled="saving" @click="editorOpen = false">取消</button>
        <button type="button" class="primary" :disabled="saving" @click="save">{{ saving ? "保存中..." : "保存" }}</button>
      </template>
    </Modal>
  </section>
</template>
