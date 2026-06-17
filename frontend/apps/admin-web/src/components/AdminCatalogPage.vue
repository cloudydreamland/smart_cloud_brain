<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { storeToRefs } from "pinia";
import {
  api,
  fieldText,
  formatApiError,
  statusClass,
  toNumber,
  useAdminWorkflowStore,
  useAuthStore,
  type DataRow,
} from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, FormField, Modal, StatusTag } from "@smart-cloud-brain/shared-ui";

type Entity = "department" | "doctor" | "drug" | "knowledge" | "prompt" | "dict";

const props = defineProps<{ entity: Entity }>();
const emit = defineEmits<{ refresh: [] }>();
const auth = useAuthStore();
const workflow = useAdminWorkflowStore();
const { departments, doctors, drugs, knowledge, prompts, dicts } = storeToRefs(workflow);
const keyword = ref("");
const error = ref("");
const saving = ref(false);
const editorOpen = ref(false);
const form = reactive<Record<string, string | number | boolean | undefined>>({});

const config = computed(() => ({
  department: {
    title: "科室维护",
    description: "维护科室编码、名称和说明。",
    rows: departments.value,
    keys: ["code", "name", "description"],
    columns: ["id", "code", "name", "description"],
    fields: [
      ["code", "科室编码", "text"],
      ["name", "科室名称", "text"],
      ["description", "说明", "textarea"],
    ],
  },
  doctor: {
    title: "医生维护",
    description: "维护医生账号、科室、职称和状态。",
    rows: doctors.value,
    keys: ["name", "phone", "departmentName", "title", "specialty"],
    columns: ["id", "name", "phone", "departmentName", "title", "status"],
    fields: [
      ["name", "姓名", "text"],
      ["phone", "手机号", "text"],
      ["password", "新密码", "password"],
      ["departmentId", "科室 ID", "number"],
      ["title", "职称", "text"],
      ["specialty", "专长", "textarea"],
      ["status", "状态", "text"],
    ],
  },
  drug: {
    title: "药品维护",
    description: "维护药品、规格、禁忌和相互作用规则。",
    rows: drugs.value,
    keys: ["name", "specification", "contraindication", "status"],
    columns: ["id", "name", "specification", "status"],
    fields: [
      ["name", "药品名称", "text"],
      ["specification", "规格", "text"],
      ["contraindication", "禁忌", "textarea"],
      ["interactionRule", "相互作用规则", "textarea"],
      ["status", "状态", "text"],
    ],
  },
  knowledge: {
    title: "知识库维护",
    description: "维护症状、风险信号、建议和科室编码。",
    rows: knowledge.value,
    keys: ["title", "symptoms", "riskSignals", "departmentCode"],
    columns: ["id", "title", "departmentCode", "status"],
    fields: [
      ["title", "标题", "text"],
      ["symptoms", "症状", "textarea"],
      ["riskSignals", "风险信号", "textarea"],
      ["advice", "建议", "textarea"],
      ["departmentCode", "科室编码", "text"],
      ["status", "状态", "text"],
    ],
  },
  prompt: {
    title: "Prompt 维护",
    description: "维护 AI 任务模板、输出 Schema 和版本。",
    rows: prompts.value,
    keys: ["taskType", "templateName", "departmentCode", "version"],
    columns: ["id", "taskType", "templateName", "departmentCode", "version"],
    fields: [
      ["taskType", "任务类型", "text"],
      ["departmentCode", "科室编码", "text"],
      ["templateName", "模板名称", "text"],
      ["templateContent", "模板内容", "textarea"],
      ["outputSchema", "输出 Schema", "textarea"],
      ["version", "版本", "text"],
      ["enabled", "启用", "checkbox"],
    ],
  },
  dict: {
    title: "字典维护",
    description: "维护系统字典类型、键值和排序。",
    rows: dicts.value,
    keys: ["dictType", "dictKey", "dictValue", "status"],
    columns: ["id", "dictType", "dictKey", "dictValue", "sort", "status"],
    fields: [
      ["dictType", "字典类型", "text"],
      ["dictKey", "字典键", "text"],
      ["dictValue", "字典值", "text"],
      ["sort", "排序", "number"],
      ["status", "状态", "text"],
    ],
  },
})[props.entity]);

const rows = computed(() => {
  const q = keyword.value.trim().toLowerCase();
  if (!q) return config.value.rows;
  return config.value.rows.filter((item) => config.value.keys.some((key) => fieldText(item, key, "").toLowerCase().includes(q)));
});

function openEditor(item?: DataRow) {
  Object.keys(form).forEach((key) => delete form[key]);
  if (item) {
    form.id = toNumber(item.id) || undefined;
    config.value.fields.forEach(([key]) => { form[key] = item[key] as string | number | boolean | undefined; });
  } else {
    config.value.fields.forEach(([key, , type]) => { form[key] = type === "checkbox" ? true : type === "number" ? 0 : ""; });
    if (props.entity === "doctor") form.departmentId = toNumber(departments.value[0]?.id);
    if ("status" in form) form.status = "ENABLED";
    if (props.entity === "prompt") {
      form.taskType = "MEDICAL_RECORD";
      form.outputSchema = "{\"type\":\"object\"}";
      form.version = "v1";
    }
  }
  editorOpen.value = true;
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
  return missing ? `请填写 ${missing}。` : "";
}

async function save() {
  const invalid = requireFields();
  if (invalid) {
    error.value = invalid;
    return;
  }
  saving.value = true;
  error.value = "";
  try {
    if (props.entity === "department") await api.saveDepartment(auth.token(), form as never);
    if (props.entity === "doctor") await api.saveDoctor(auth.token(), form as never);
    if (props.entity === "drug") await api.saveDrug(auth.token(), form as never);
    if (props.entity === "knowledge") await api.saveKnowledgeEntry(auth.token(), form as never);
    if (props.entity === "prompt") await api.savePrompt(auth.token(), form as never);
    if (props.entity === "dict") await api.saveDict(auth.token(), form as never);
    editorOpen.value = false;
    emit("refresh");
  } catch (err) {
    error.value = formatApiError(err, "保存失败");
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <section class="catalog-layout">
    <section class="panel">
      <header class="panel-header">
        <div class="panel-title"><p class="eyebrow">CATALOG</p><h2>{{ config.title }}</h2><p>{{ config.description }}</p></div>
        <button type="button" class="primary" @click="openEditor()">新增</button>
      </header>
      <div class="panel-body stack">
        <ErrorState v-if="error" :message="error" />
        <div class="admin-filter-row"><input v-model.trim="keyword" placeholder="搜索当前表" /></div>
        <div v-if="rows.length" class="table-scroll">
          <table class="data-table">
            <thead><tr><th v-for="column in config.columns" :key="column">{{ column }}</th><th class="actions-cell">操作</th></tr></thead>
            <tbody>
              <tr v-for="item in rows" :key="String(item.id)">
                <td v-for="column in config.columns" :key="column">
                  <StatusTag v-if="column === 'status'" :status="fieldText(item, column)" :tone="statusClass(item[column])" />
                  <span v-else>{{ fieldText(item, column) }}</span>
                </td>
                <td><button type="button" @click="openEditor(item)">编辑</button></td>
              </tr>
            </tbody>
          </table>
        </div>
        <EmptyState v-else title="暂无数据" message="当前筛选条件下没有记录。" />
      </div>
    </section>
    <aside class="panel">
      <header class="panel-header"><div class="panel-title"><h2>维护说明</h2><p>所有保存动作直接调用后端 API。</p></div></header>
      <div class="panel-body">
        <div class="notice warning">请确认字段含义后保存。当前页不伪造接口，也不修改后端路径。</div>
      </div>
    </aside>
    <Modal :open="editorOpen" :title="config.title" description="新增或编辑当前维护对象。" @close="editorOpen = false">
      <div class="stack">
        <FormField v-for="[key, label, type] in config.fields" :key="key" :label="label">
          <textarea v-if="type === 'textarea'" v-model="form[key]" />
          <input v-else-if="type === 'checkbox'" v-model="form[key]" type="checkbox" />
          <input v-else v-model="form[key]" :type="type" />
        </FormField>
      </div>
      <template #footer>
        <button type="button" :disabled="saving" @click="editorOpen = false">取消</button>
        <button type="button" class="primary" :disabled="saving" @click="save">{{ saving ? "保存中" : "保存" }}</button>
      </template>
    </Modal>
  </section>
</template>
