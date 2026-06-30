<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { api, formatApiError, statusText, toNumber, type Patient, type PatientVisitorSaveRequest } from "@smart-cloud-brain/shared-api";
import { ConfirmDialog, EmptyState, ErrorState, FormField, LoadingState, Toast } from "@smart-cloud-brain/shared-ui";

const loading = ref(false);
const saving = ref(false);
const error = ref("");
const toast = ref<InstanceType<typeof Toast>>();
const visitors = ref<Patient[]>([]);
const targetToDelete = ref<Patient | null>(null);

const emptyForm: PatientVisitorSaveRequest = {
  name: "",
  relationship: "",
  phone: "",
  gender: "",
  age: 0,
  address: "",
  emergencyContact: "",
  emergencyPhone: "",
  bloodType: "",
  heightCm: 0,
  weightKg: 0,
  allergyHistory: "",
  pastHistory: "",
};

const form = reactive<PatientVisitorSaveRequest>({ ...emptyForm });

function resetForm() {
  delete form.id;
  Object.assign(form, emptyForm);
}

function edit(row: Patient) {
  Object.assign(form, {
    id: toNumber(row.id, 0),
    name: row.name || "",
    relationship: row.relationship || "",
    phone: row.phone || "",
    gender: row.gender || "",
    age: toNumber(row.age, 0),
    address: row.address || "",
    emergencyContact: row.emergencyContact || "",
    emergencyPhone: row.emergencyPhone || "",
    bloodType: row.bloodType || "",
    heightCm: toNumber(row.heightCm, 0),
    weightKg: toNumber(row.weightKg, 0),
    allergyHistory: row.allergyHistory || "",
    pastHistory: row.pastHistory || "",
  });
}

async function loadVisitors() {
  loading.value = true;
  error.value = "";
  try {
    visitors.value = await api.patientVisitors();
  } catch (err) {
    error.value = formatApiError(err, "就诊人加载失败");
  } finally {
    loading.value = false;
  }
}

async function saveVisitor() {
  if (!form.name.trim()) {
    error.value = "请输入就诊人姓名";
    return;
  }
  saving.value = true;
  error.value = "";
  try {
    await api.savePatientVisitor({ ...form, name: form.name.trim() });
    resetForm();
    await loadVisitors();
    toast.value?.success("保存成功", "就诊人已保存");
  } catch (err) {
    error.value = formatApiError(err, "就诊人保存失败");
  } finally {
    saving.value = false;
  }
}

function confirmDeleteVisitor(row: Patient) {
  targetToDelete.value = row;
}


async function deleteVisitor(row: Patient) {
  const id = toNumber(row.id, 0);
  if (!id) return;
  saving.value = true;
  error.value = "";
  try {
    await api.deletePatientVisitor(id);
    if (form.id === id) resetForm();
    await loadVisitors();
    toast.value?.success("操作成功", "就诊人已删除");
  } catch (err) {
    error.value = formatApiError(err, "就诊人删除失败");
  } finally {
    targetToDelete.value = null;
    saving.value = false;
  }
}

onMounted(loadVisitors);
</script>

<template>
  <section class="portal-grid">
    <section class="panel">
      <header class="panel-header">
        <div class="panel-title">
          <p class="eyebrow">就诊人</p>
          <h2>就诊人管理</h2>
          <p>一个患者账户可以维护本人和多个家属就诊人。</p>
        </div>
      </header>
      <div class="panel-body stack">
        <ErrorState v-if="error" :message="error" />
        <LoadingState v-if="loading" />

        <div v-if="visitors.length" class="visitor-list">
          <article v-for="row in visitors" :key="`${row.visitorType || ''}-${String(row.id)}`" class="visitor-row">
            <div>
              <div class="visitor-row-title">
                <strong>{{ row.name || "" }}</strong>
                <span class="portal-status" :class="(row.visitorType || '') === 'ACCOUNT' ? 'online' : ''">
                  {{ (row.visitorType || "") === "ACCOUNT" ? "账户本人" : row.relationship || "就诊人" }}
                </span>
              </div>
              <p>
                {{ statusText(row.gender, "未说明") }} · {{ String(row.age ?? 0) }} 岁 ·
                {{ row.phone || "未填写电话" }}
              </p>
              <p>紧急联系人：{{ row.emergencyContact || "未填写" }} {{ row.emergencyPhone || "" }}</p>
              <p>血型 {{ row.bloodType || "未说明" }} · 身高 {{ String(row.heightCm ?? 0) }} cm · 体重 {{ String(row.weightKg ?? 0) }} kg</p>
            </div>
            <div class="visitor-actions">
              <button v-if="row.editable" class="secondary" type="button" @click="edit(row)">编辑</button>
              <button v-if="row.editable" class="danger-text" type="button" :disabled="saving" @click="confirmDeleteVisitor(row)">删除</button>
            </div>
          </article>
        </div>
        <EmptyState v-else-if="!loading" title="暂无就诊人" message="账户本人会在资料加载后显示，也可以先新增家属就诊人。" />
      </div>
    </section>

    <aside class="panel">
      <header class="panel-header">
        <div class="panel-title">
          <h2>{{ form.id ? "编辑就诊人" : "新增就诊人" }}</h2>
          <p>用于预约和就诊时快速切换实际就诊对象。</p>
        </div>
      </header>
      <div class="panel-body stack">
        <div class="form-grid">
          <FormField label="姓名"><input v-model.trim="form.name" /></FormField>
          <FormField label="关系"><input v-model.trim="form.relationship" placeholder="如 父母、子女、配偶" /></FormField>
          <FormField label="手机号"><input v-model.trim="form.phone" /></FormField>
          <FormField label="性别">
            <select v-model="form.gender">
              <option value="">未说明</option>
              <option value="MALE">男</option>
              <option value="FEMALE">女</option>
              <option value="UNKNOWN">未说明</option>
            </select>
          </FormField>
          <FormField label="年龄"><input v-model.number="form.age" type="number" min="0" max="130" /></FormField>
          <FormField label="血型">
            <select v-model="form.bloodType">
              <option value="">未说明</option>
              <option value="A">A 型</option>
              <option value="B">B 型</option>
              <option value="AB">AB 型</option>
              <option value="O">O 型</option>
              <option value="UNKNOWN">不确定</option>
            </select>
          </FormField>
          <FormField label="身高（cm）"><input v-model.number="form.heightCm" type="number" min="0" max="260" /></FormField>
          <FormField label="体重（kg）"><input v-model.number="form.weightKg" type="number" min="0" max="400" step="0.1" /></FormField>
        </div>
        <FormField label="地址"><input v-model.trim="form.address" /></FormField>
        <div class="form-grid">
          <FormField label="紧急联系人"><input v-model.trim="form.emergencyContact" /></FormField>
          <FormField label="紧急联系电话"><input v-model.trim="form.emergencyPhone" /></FormField>
        </div>
        <FormField label="过敏史"><textarea v-model.trim="form.allergyHistory" /></FormField>
        <FormField label="既往史"><textarea v-model.trim="form.pastHistory" /></FormField>
        <div class="portal-service-actions">
          <button class="primary" type="button" :disabled="saving" @click="saveVisitor">{{ saving ? "保存中..." : "保存就诊人" }}</button>
          <button class="secondary" type="button" :disabled="saving" @click="resetForm">清空</button>
        </div>
      </div>
    </aside>
    <ConfirmDialog
      :open="Boolean(targetToDelete)"
      title="删除就诊人"
      :message="`确认删除 ${targetToDelete ? targetToDelete.name || '就诊人' : ''}？删除后不可恢复。`"
      confirm-text="确认删除"
      tone="danger"
      :busy="saving"
      @close="targetToDelete = null"
      @confirm="targetToDelete && deleteVisitor(targetToDelete)"
    />
    <Toast ref="toast" />
  </section>
</template>
