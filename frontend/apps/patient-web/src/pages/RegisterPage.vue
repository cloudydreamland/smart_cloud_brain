<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { api, formatApiError } from "@smart-cloud-brain/shared-api";
import { ErrorState, FormField } from "@smart-cloud-brain/shared-ui";

const router = useRouter();
const loading = ref(false);
const error = ref("");
const notice = ref("");
const form = reactive({
  name: "",
  phone: "",
  password: "",
  gender: "FEMALE",
  age: 30,
  allergyHistory: "",
  pastHistory: "",
});

function validate() {
  if (!form.name.trim()) return "请输入姓名。";
  if (!/^1\d{10}$/.test(form.phone.trim())) return "请输入 11 位手机号。";
  if (form.password.length < 6) return "密码至少 6 位。";
  if (form.age < 0 || form.age > 120) return "请输入有效年龄。";
  return "";
}

async function submit() {
  const invalid = validate();
  if (invalid) {
    error.value = invalid;
    return;
  }
  loading.value = true;
  error.value = "";
  notice.value = "";
  try {
    await api.registerPatient({ ...form, phone: form.phone.trim() });
    notice.value = "注册成功，请登录后继续分诊和挂号。";
    window.setTimeout(() => router.push({ name: "patient-login" }), 600);
  } catch (err) {
    error.value = formatApiError(err, "注册失败");
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <section class="mayo-container auth-grid">
    <div class="auth-copy">
      <p class="eyebrow">PATIENT REGISTER</p>
      <h1>创建患者档案</h1>
      <p class="mayo-lead">基础信息会用于挂号、医生接诊识别和处方审核，请保持真实准确。</p>
    </div>
    <form class="panel" @submit.prevent="submit">
      <header class="panel-header"><div class="panel-title"><h2>患者注册</h2><p>完成后返回登录。</p></div></header>
      <div class="panel-body stack">
        <ErrorState v-if="error" :message="error" />
        <div v-if="notice" class="notice success">{{ notice }}</div>
        <div class="form-grid">
          <FormField label="姓名"><input v-model.trim="form.name" /></FormField>
          <FormField label="手机号"><input v-model.trim="form.phone" /></FormField>
          <FormField label="密码"><input v-model="form.password" type="password" autocomplete="new-password" /></FormField>
          <FormField label="年龄"><input v-model.number="form.age" type="number" min="0" max="120" /></FormField>
          <FormField label="性别">
            <select v-model="form.gender">
              <option value="FEMALE">女</option>
              <option value="MALE">男</option>
              <option value="UNKNOWN">未说明</option>
            </select>
          </FormField>
          <FormField label="过敏史"><input v-model.trim="form.allergyHistory" placeholder="如无可留空" /></FormField>
        </div>
        <FormField label="既往史"><textarea v-model.trim="form.pastHistory" rows="3" /></FormField>
        <div class="toolbar">
          <button class="primary" type="submit" :disabled="loading">{{ loading ? "注册中" : "注册" }}</button>
          <RouterLink class="button" to="/login">已有账号</RouterLink>
        </div>
      </div>
    </form>
  </section>
</template>
