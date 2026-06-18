<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { api, formatApiError, useAuthStore } from "@smart-cloud-brain/shared-api";
import { FormField, ErrorState } from "@smart-cloud-brain/shared-ui";

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
      await router.push(String(route.query.redirect || "/"));
    }
  } catch (err) {
    error.value = formatApiError(err, "登录失败");
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <section class="mayo-container auth-grid">
    <div class="auth-copy">
      <p class="eyebrow">患者登录</p>
      <h1>进入患者服务门户</h1>
      <p class="mayo-lead">登录后可以提交分诊、选择号源、管理挂号，并查看诊后病历与处方。</p>
    </div>
    <form class="panel" @submit.prevent="submit">
      <header class="panel-header">
        <div class="panel-title">
          <h2>患者登录</h2>
          <p>使用已注册手机号或账号登录。</p>
        </div>
      </header>
      <div class="panel-body stack">
        <ErrorState v-if="error" :message="error" />
        <FormField label="账号"><input v-model.trim="form.account" autocomplete="username" /></FormField>
        <FormField label="密码"><input v-model="form.password" type="password" autocomplete="current-password" /></FormField>
        <div class="toolbar">
          <button class="primary" type="submit" :disabled="loading">{{ loading ? "登录中" : "登录" }}</button>
          <RouterLink class="button" to="/register">注册新患者</RouterLink>
        </div>
      </div>
    </form>
  </section>
</template>
