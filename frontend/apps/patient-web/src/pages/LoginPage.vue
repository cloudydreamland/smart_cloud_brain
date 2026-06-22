<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { api, formatApiError, useAuthStore } from "@smart-cloud-brain/shared-api";
import { ErrorState } from "@smart-cloud-brain/shared-ui";

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const form = reactive({ account: "", password: "" });
const loading = ref(false);
const error = ref("");

async function submit() {
  if (!form.account.trim() || !form.password.trim()) {
    error.value = "请输入账号和密码。";
    return;
  }
  loading.value = true;
  error.value = "";
  try {
    const session = await api.loginPatient(form.account.trim(), form.password);
    auth.save("patient-session", session, "PATIENT");
    if (!auth.permissionError) {
      await router.push(String(route.query.redirect || "/portal"));
    }
  } catch (err) {
    error.value = formatApiError(err, "登录失败");
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
        <h1>预约前，请先确认您的患者身份</h1>
        <p>登录后可继续 AI 分诊、选择医生号源、管理挂号、查看病历和处方。</p>
      </div>
    </div>
    <form class="auth-form" @submit.prevent="submit">
      <div class="auth-tabs">
        <button class="active" type="button">登录</button>
        <RouterLink :to="{ name: 'patient-register' }">注册</RouterLink>
      </div>
      <ErrorState v-if="error" :message="error" />
      <label>
        <span>账号</span>
        <input v-model.trim="form.account" autocomplete="username" placeholder="手机号或账号">
      </label>
      <label>
        <span>密码</span>
        <input v-model="form.password" type="password" autocomplete="current-password" placeholder="请输入密码">
      </label>
      <button class="patient-primary" type="submit" :disabled="loading">{{ loading ? "登录中" : "登录并继续预约" }}</button>
      <p>演示环境使用后端真实登录接口；未注册患者请先创建档案。</p>
    </form>
  </section>
</template>
