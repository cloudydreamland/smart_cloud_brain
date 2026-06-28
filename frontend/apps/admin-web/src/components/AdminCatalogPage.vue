<script setup lang="ts">
import { computed, reactive, ref } from "vue";
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
  type KnowledgeEntry,
  type PromptTemplateSaveRequest,
  type PromptTestRequest,
  type PromptTemplate,
  type SystemDict,
} from "@smart-cloud-brain/shared-api";
import { DataTable, ErrorState, FormField, Modal, PaginationBar, StatusTag } from "@smart-cloud-brain/shared-ui";

type Entity = "department" | "doctor" | "drug" | "knowledge" | "prompt" | "dict";
type FieldType = "text" | "password" | "number" | "textarea" | "checkbox" | "department-select";
type FieldConfig = [key: string, label: string, type: FieldType];
type DictOption = { value: string; label: string };
type CatalogRow = Department | Doctor | Drug | KnowledgeEntry | PromptTemplate | SystemDict;

const props = defineProps<{ entity: Entity }>();
const emit = defineEmits<{ refresh: [] }>();
const workflow = useAdminWorkflowStore();
const { departments, doctors, drugs, knowledge, prompts, dicts } = storeToRefs(workflow);
const keyword = ref("");
const error = ref("");
const notice = ref("");
const saving = ref(false);
const testing = ref(false);
const loading = ref(false);
const editorOpen = ref(false);
const testResult = ref("");
const form = reactive<Record<string, string | number | boolean | undefined>>({});

const promptTasks = [
  {
    value: "TRIAGE",
    label: "智能分诊",
    required: ["recommendedDepartment", "departmentCode", "recommendedDoctorDirection", "urgencyLevel", "confidence", "recommendedDoctorIds", "reason"],
    sample: "胸痛、气短两天，活动后加重",
  },
  {
    value: "MEDICAL_RECORD",
    label: "病历生成",
    required: ["chiefComplaint", "presentIllness", "pastHistory", "physicalExam", "diagnosis", "treatmentAdvice", "soapContent"],
    sample: "患者胸痛、气短两天，活动后加重，休息后稍缓解。",
  },
  {
    value: "PRESCRIPTION_CHECK",
    label: "处方审核",
    required: ["riskLevel", "riskDescription", "suggestions", "interactions", "contraindications", "adjustmentSuggestions"],
    sample: "诊断：胸痛待查，高血压；药品：aspirin 100mg once daily oral",
  },
];

const fallbackDicts: Record<string, DictOption[]> = {
  DICT_TYPE: [
    { value: "SYSTEM_STATUS", label: "系统状态" },
    { value: "PROMPT_TASK_TYPE", label: "AI 任务类型" },
    { value: "REGISTRATION_STATUS", label: "挂号状态" },
    { value: "TRIAGE_STATUS", label: "分诊状态" },
    { value: "RISK_LEVEL", label: "风险等级" },
    { value: "READ_STATUS", label: "阅读状态" },
    { value: "GENDER", label: "性别" },
  ],
  SYSTEM_STATUS: [
    { value: "ENABLED", label: "启用" },
    { value: "DISABLED", label: "停用" },
  ],
  PROMPT_TASK_TYPE: promptTasks.map((task) => ({ value: task.value, label: task.label })),
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
  fields: FieldConfig[];
}> = {
  department: {
    title: "科室维护",
    description: "维护科室编码、名称和说明。",
    rows: () => departments.value,
    keys: ["code", "name", "description"],
    columns: ["id", "code", "name", "description"],
    fields: [["code", "科室编码", "text"], ["name", "科室名称", "text"], ["description", "说明", "textarea"]],
  },
  doctor: {
    title: "医生维护",
    description: "维护医生账号、科室、职称、专长和状态。",
    rows: () => doctors.value,
    keys: ["name", "phone", "departmentName", "title", "specialty"],
    columns: ["id", "name", "phone", "departmentName", "title", "status"],
    fields: [["name", "姓名", "text"], ["phone", "手机号", "text"], ["password", "新密码", "password"], ["departmentId", "科室", "department-select"], ["title", "职称", "text"], ["specialty", "专长", "textarea"], ["status", "状态", "text"]],
  },
  drug: {
    title: "药品维护",
    description: "维护药品、规格、禁忌和相互作用规则。",
    rows: () => drugs.value,
    keys: ["name", "specification", "contraindication", "status"],
    columns: ["id", "name", "specification", "status"],
    fields: [["name", "药品名称", "text"], ["specification", "规格", "text"], ["contraindication", "禁忌", "textarea"], ["interactionRule", "相互作用规则", "textarea"], ["status", "状态", "text"]],
  },
  knowledge: {
    title: "知识库维护",
    description: "维护症状、风险信号、建议和科室编码。",
    rows: () => knowledge.value,
    keys: ["title", "symptoms", "riskSignals", "departmentCode"],
    columns: ["id", "title", "departmentCode", "status"],
    fields: [["title", "标题", "text"], ["symptoms", "症状", "textarea"], ["riskSignals", "风险信号", "textarea"], ["advice", "建议", "textarea"], ["departmentCode", "科室编码", "text"], ["status", "状态", "text"]],
  },
  prompt: {
    title: "提示词维护",
    description: "维护智能任务模板、输出结构定义和版本。",
    rows: () => prompts.value,
    keys: ["taskType", "templateName", "departmentCode", "version"],
    columns: ["id", "taskType", "templateName", "departmentCode", "version", "enabled"],
    fields: [["taskType", "任务类型", "text"], ["departmentCode", "科室编码", "text"], ["templateName", "模板名称", "text"], ["templateContent", "模板内容", "textarea"], ["outputSchema", "输出结构定义", "textarea"], ["version", "版本", "text"], ["enabled", "启用", "checkbox"]],
  },
  dict: {
    title: "字典维护",
    description: "维护系统字典类型、键值和排序。",
    rows: () => dicts.value,
    keys: ["dictType", "dictKey", "dictValue", "status"],
    columns: ["id", "dictType", "dictKey", "dictValue", "sort", "status"],
    fields: [["dictType", "字典类型", "text"], ["dictKey", "字典键", "text"], ["dictValue", "字典值", "text"], ["sort", "排序", "number"], ["status", "状态", "text"]],
  },
};

const config = computed(() => configs[props.entity]);

/* ---------- 实体保存策略（替代散落的 if 链） ---------- */
const saveHandlers: Record<Entity, () => Promise<void>> = {
  department: () => api.saveDepartment(form as never),
  doctor:     () => api.saveDoctor(form as never),
  drug:       () => api.saveDrug(form as never),
  knowledge:  () => api.saveKnowledgeEntry(form as never),
  prompt:     () => api.savePrompt(promptSaveBody()),
  dict:       () => api.saveDict(form as never),
};
const rows = computed(() => {
  const q = keyword.value.trim().toLowerCase();
  const source = config.value.rows();
  if (!q) return source;
  return source.filter((item) => config.value.keys.some((key) => displayText(getCatalogValue(item, key), "").toLowerCase().includes(q)));
});
const { currentPage, pageSize, total, pageRows } = usePagination(rows, 8);

function getCatalogValue(row: CatalogRow, key: string) {
  return (row as Record<string, unknown>)[key];
}

function dictionaryOptions(dictType: string): DictOption[] {
  const options = dicts.value
    .filter((item) => item.dictType === dictType && displayText(item.status, "ENABLED") !== "DISABLED")
    .sort((left, right) => toNumber(left.sort) - toNumber(right.sort) || toNumber(left.id) - toNumber(right.id))
    .map((item) => ({ value: displayText(item.dictKey), label: displayText(item.dictValue) }))
    .filter((item) => item.value);
  return options.length ? options : fallbackDicts[dictType] ?? [];
}

function firstDictValue(dictType: string, fallback: string) {
  return dictionaryOptions(dictType)[0]?.value ?? fallback;
}

function fieldDictType(key: string) {
  if (key === "status") return "SYSTEM_STATUS";
  if (key === "taskType") return "PROMPT_TASK_TYPE";
  if (key === "dictType") return "DICT_TYPE";
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
  if (column === "taskType") return dictionaryLabel("PROMPT_TASK_TYPE", value);
  if (column === "dictType") return dictionaryLabel("DICT_TYPE", value);
  if (column === "departmentCode") return departmentCodeOptions().find((item) => item.value === String(value ?? ""))?.label ?? String(value ?? "");
  return String(value ?? "");
}

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    await workflow.refresh();
  } catch (err) {
    error.value = formatApiError(err, "列表加载失败");
  } finally {
    loading.value = false;
  }
}

function openEditor(item?: CatalogRow) {
  notice.value = "";
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
  ensurePromptFields();
  editorOpen.value = true;
}

/** 各实体新增时的默认值填充 */
function initEntityDefaults() {
  if (props.entity === "doctor") form.departmentId = toNumber(departments.value[0]?.id);
  if ("status" in form) form.status = firstDictValue("SYSTEM_STATUS", "ENABLED");
  if (props.entity === "dict") form.dictType = firstDictValue("DICT_TYPE", "SYSTEM_STATUS");
  if (props.entity === "knowledge" || props.entity === "prompt") form.departmentCode = departmentCodeOptions()[0]?.value ?? "GENERAL";
  if (props.entity === "prompt") {
    form.taskType = firstDictValue("PROMPT_TASK_TYPE", "MEDICAL_RECORD");
    form.outputSchema = defaultPromptSchema(String(form.taskType));
    form.version = "v1";
    form.sampleInput = promptTaskConfig(String(form.taskType)).sample;
  }
}

/** prompt 实体编辑器打开后，确保 sampleInput / outputSchema 有值 */
function ensurePromptFields() {
  if (props.entity !== "prompt") return;
  if (!form.sampleInput) form.sampleInput = promptTaskConfig(String(form.taskType)).sample;
  if (!form.outputSchema) form.outputSchema = defaultPromptSchema(String(form.taskType));
}

function requireFields() {
  const required: Record<Entity, string[]> = {
    department: ["code", "name"],
    doctor: ["name", "phone", "departmentId"],
    drug: ["name"],
    knowledge: ["title", "symptoms", "advice"],
    prompt: ["taskType", "templateName", "templateContent"],
    dict: ["dictType", "dictKey", "dictValue"],
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
  const invalid = requireFields() || validateDoctorForm() || validatePromptForm();
  if (invalid) {
    error.value = invalid;
    return;
  }
  saving.value = true;
  error.value = "";
  notice.value = "";
  try {
    await saveHandlers[props.entity]();
    editorOpen.value = false;
    notice.value = `${config.value.title}已保存。`;
    emit("refresh");
    await refresh();
  } catch (err) {
    error.value = formatApiError(err, "保存失败");
  } finally {
    saving.value = false;
  }
}

function promptTaskConfig(taskType: string) {
  return promptTasks.find((item) => item.value === String(taskType || "").toUpperCase()) ?? promptTasks[1];
}

function defaultPromptSchema(taskType: string) {
  const required = promptTaskConfig(taskType).required;
  return JSON.stringify({ type: "object", required }, null, 2);
}

function onPromptTaskChange() {
  if (props.entity !== "prompt") return;
  const task = promptTaskConfig(String(form.taskType));
  if (!form.templateName) form.templateName = `${task.value}_v1`;
  form.outputSchema = defaultPromptSchema(task.value);
  form.sampleInput = task.sample;
  testResult.value = "";
}

function validatePromptForm() {
  if (props.entity !== "prompt") return "";
  const task = promptTaskConfig(String(form.taskType));
  let schema: { required?: unknown; properties?: Record<string, unknown> };
  try {
    schema = JSON.parse(String(form.outputSchema || "{}"));
  } catch {
    return "输出结构定义必须是合法 JSON。";
  }
  const required = Array.isArray(schema.required) ? schema.required.map(String) : [];
  const properties = schema.properties && typeof schema.properties === "object" ? Object.keys(schema.properties) : [];
  const missing = task.required.filter((field) => !required.includes(field) && !properties.includes(field));
  return missing.length ? `输出结构缺少字段：${missing.join(", ")}` : "";
}

function promptSaveBody(): PromptTemplateSaveRequest {
  return {
    id: typeof form.id === "number" ? form.id : undefined,
    taskType: String(form.taskType || ""),
    departmentCode: String(form.departmentCode || ""),
    templateName: String(form.templateName || ""),
    templateContent: String(form.templateContent || ""),
    outputSchema: String(form.outputSchema || ""),
    version: String(form.version || ""),
    enabled: Boolean(form.enabled),
  };
}

function promptTestBody(): PromptTestRequest {
  return {
    ...promptSaveBody(),
    sampleInput: String(form.sampleInput || ""),
  };
}

async function testPrompt() {
  const invalid = requireFields() || validatePromptForm();
  if (invalid) {
    error.value = invalid;
    return;
  }
  testing.value = true;
  error.value = "";
  testResult.value = "";
  try {
    const result = await api.testPrompt(promptTestBody());
    testResult.value = JSON.stringify(result, null, 2);
  } catch (err) {
    error.value = formatApiError(err, "提示词试运行失败");
  } finally {
    testing.value = false;
  }
}

refresh();
</script>

<template>
  <section class="catalog-layout">
    <section class="panel">
      <header class="panel-header">
        <div class="panel-title"><h2>{{ config.title }}</h2></div>
        <div class="toolbar">
          <button type="button" :disabled="loading" @click="refresh">刷新</button>
          <button type="button" class="primary" @click="openEditor()">新增</button>
        </div>
      </header>
      <div class="panel-body stack">
        <ErrorState v-if="error" :message="error" />
        <div v-if="notice" class="notice success">{{ notice }}</div>
        <div class="admin-filter-row"><input v-model.trim="keyword" placeholder="搜索当前表" /></div>
        <DataTable :rows="rows" :loading="loading" :error="error" :breakout="true" empty-title="暂无数据" empty-message="当前筛选条件下没有记录。">
          <thead><tr><th v-for="column in config.columns" :key="column">{{ column }}</th><th class="actions-cell">操作</th></tr></thead>
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
          <select v-else-if="shouldUseDictionarySelect(key)" v-model="form[key]" @change="key === 'taskType' && onPromptTaskChange()">
            <option v-for="option in dictionaryOptions(fieldDictType(key))" :key="option.value" :value="option.value">{{ option.label }}（{{ option.value }}）</option>
          </select>
          <select v-else-if="type === 'department-select'" v-model.number="form[key]">
            <option :value="0" disabled>请选择科室</option>
            <option v-for="department in departments" :key="String(department.id)" :value="toNumber(department.id)">
              {{ displayText(department.name) }}
            </option>
          </select>
          <select v-else-if="key === 'departmentCode'" v-model="form[key]">
            <option v-for="department in departmentCodeOptions()" :key="department.value" :value="department.value">{{ department.label }}</option>
          </select>
          <input v-else-if="type === 'number'" v-model.number="form[key]" type="number" />
          <input v-else v-model="form[key]" :type="type" />
        </FormField>
        <FormField v-if="props.entity === 'prompt'" label="试运行样例">
          <textarea v-model="form.sampleInput" />
        </FormField>
        <pre v-if="testResult" class="prompt-test-result">{{ testResult }}</pre>
      </div>
      <template #footer>
        <button type="button" :disabled="saving" @click="editorOpen = false">取消</button>
        <button v-if="props.entity === 'prompt'" type="button" :disabled="saving || testing" @click="testPrompt">{{ testing ? "试运行中..." : "试运行" }}</button>
        <button type="button" class="primary" :disabled="saving || testing" @click="save">{{ saving ? "保存中..." : "保存" }}</button>
      </template>
    </Modal>
  </section>
</template>
