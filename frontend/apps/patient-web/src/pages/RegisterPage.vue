<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { api, formatApiError } from "@smart-cloud-brain/shared-api";
import { ErrorState } from "@smart-cloud-brain/shared-ui";

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
  <section class="patient-auth-page">
    <RouterLink class="floating-brand" :to="{ name: 'patient-home' }" aria-label="返回智慧云脑首页">
      <span>智慧<br />云脑</span>
      <i></i><i></i>
    </RouterLink>
    <div class="auth-visual">
      <img src="https://images.unsplash.com/photo-1631217872822-1b2a9955d7e5?auto=format&fit=crop&w=1200&q=80" alt="智慧医院接待大厅">
      <div>
        <h1>创建患者档案</h1>
        <p>基础信息将用于分诊、挂号、医生识别和处方审核，请保持真实准确。</p>
      </div>
    </div>
    <form class="auth-form" @submit.prevent="submit">
      <div class="auth-tabs">
        <RouterLink :to="{ name: 'patient-login' }">登录</RouterLink>
        <button class="active" type="button">注册</button>
      </div>
      <ErrorState v-if="error" :message="error" />
      <div v-if="notice" class="portal-message success">{{ notice }}</div>
      <div class="auth-form-grid">
        <label><span>姓名</span><input v-model.trim="form.name"></label>
        <label><span>手机号</span><input v-model.trim="form.phone"></label>
        <label><span>密码</span><input v-model="form.password" type="password" autocomplete="new-password"></label>
        <label><span>年龄</span><input v-model.number="form.age" type="number" min="0" max="120"></label>
        <label>
          <span>性别</span>
          <select v-model="form.gender">
            <option value="FEMALE">女</option>
            <option value="MALE">男</option>
            <option value="UNKNOWN">未说明</option>
          </select>
        </label>
        <label><span>过敏史</span><input v-model.trim="form.allergyHistory" placeholder="如无可留空"></label>
      </div>
      <label><span>既往史</span><textarea v-model.trim="form.pastHistory" rows="3"></textarea></label>
      <button class="patient-primary" type="submit" :disabled="loading">{{ loading ? "注册中" : "创建档案并继续" }}</button>
    </form>
  </section>
</template>
