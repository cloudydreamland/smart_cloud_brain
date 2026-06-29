<script setup lang="ts">
import { reactive, ref, watch } from "vue";
import { storeToRefs } from "pinia";
import { api, fieldText, formatApiError, statusText, toNumber, useAuthStore, usePatientWorkflowStore, type PatientSaveRequest } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, FormField, LoadingState, Toast } from "@smart-cloud-brain/shared-ui";

const auth = useAuthStore();
const workflow = usePatientWorkflowStore();
const { patient } = storeToRefs(workflow);
const loading = ref(false);
const error = ref("");
const toast = ref<InstanceType<typeof Toast>>();
const form = reactive<PatientSaveRequest>({
  name: "",
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
});

watch(patient, (value) => {
  form.name = fieldText(value, "name", auth.session?.name || "");
  form.gender = fieldText(value, "gender", "");
  form.age = toNumber(value?.age, 0);
  form.address = fieldText(value, "address", "");
  form.emergencyContact = fieldText(value, "emergencyContact", "");
  form.emergencyPhone = fieldText(value, "emergencyPhone", "");
  form.bloodType = fieldText(value, "bloodType", "");
  form.heightCm = toNumber(value?.heightCm, 0);
  form.weightKg = toNumber(value?.weightKg, 0);
  form.allergyHistory = fieldText(value, "allergyHistory", "");
  form.pastHistory = fieldText(value, "pastHistory", "");
}, { immediate: true });

async function save() {
  loading.value = true;
  error.value = "";
  try {
    patient.value = await api.saveProfile({ ...form });
    await workflow.refreshAuthenticated();
    toast.value?.success("保存成功", "资料已保存");
  } catch (err) {
    error.value = formatApiError(err, "保存资料失败");
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <section class="portal-grid">
    <section class="panel">
      <header class="panel-header"><div class="panel-title"><p class="eyebrow">个人信息</p><h2>个人资料</h2><p>维护账户本人档案，供分诊、预约和处方审核使用。</p></div></header>
      <div class="panel-body stack">
        <ErrorState v-if="error" :message="error" />
        <LoadingState v-if="loading" />
        <template v-if="patient">
          <div class="profile-section-title">身份信息</div>
          <div class="form-grid">
            <FormField label="姓名"><input v-model.trim="form.name" /></FormField>
            <FormField label="性别"><select v-model="form.gender"><option value="">未说明</option><option value="MALE">男</option><option value="FEMALE">女</option><option value="UNKNOWN">未说明</option></select></FormField>
            <FormField label="年龄"><input v-model.number="form.age" type="number" min="0" max="130" /></FormField>
          </div>
          <FormField label="地址"><input v-model.trim="form.address" placeholder="常住地址或通信地址" /></FormField>

          <div class="profile-section-title">紧急联系</div>
          <div class="form-grid">
            <FormField label="紧急联系人"><input v-model.trim="form.emergencyContact" /></FormField>
            <FormField label="紧急联系电话"><input v-model.trim="form.emergencyPhone" /></FormField>
          </div>

          <div class="profile-section-title">健康指标</div>
          <div class="form-grid">
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

          <div class="profile-section-title">病史信息</div>
          <FormField label="过敏史"><textarea v-model.trim="form.allergyHistory" /></FormField>
          <FormField label="既往史"><textarea v-model.trim="form.pastHistory" /></FormField>
          <button class="primary" type="button" :disabled="loading" @click="save">保存资料</button>
        </template>
        <EmptyState v-else title="暂无患者资料" message="请刷新页面或重新登录。" />
      </div>
    </section>
    <aside class="panel">
      <header class="panel-header"><div class="panel-title"><h2>账户</h2><p>账户下可维护本人资料和多个就诊人。</p></div></header>
      <div class="panel-body">
        <div class="summary-strip">
          <div class="summary-item"><span>角色</span><strong>{{ statusText(auth.session?.role, "-") }}</strong></div>
          <div class="summary-item"><span>账户 ID</span><strong>{{ auth.session?.userId }}</strong></div>
          <div class="summary-item"><span>手机号</span><strong>{{ fieldText(patient, "phone", "-") }}</strong></div>
          <div class="summary-item"><span>紧急联系人</span><strong>{{ fieldText(patient, "emergencyContact", "-") }}</strong></div>
        </div>
      </div>
    </aside>
    <Toast ref="toast" />
  </section>
</template>
