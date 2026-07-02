<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { api, formatApiError, useAuthStore } from "@smart-cloud-brain/shared-api";

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();
const form = reactive({ account: "", password: "" });
const loading = ref(false);
const error = ref("");

async function submit() {
  if (!form.account.trim() || !form.password.trim()) {
    error.value = "请输入管理员账号和密码。";
    return;
  }
  loading.value = true;
  error.value = "";
  try {
    const session = await api.loginAdmin(form.account.trim(), form.password);
    auth.save("admin-session", session, "ADMIN");
    if (!auth.permissionError) await router.push(String(route.query.redirect || "/"));
  } catch (err) {
    error.value = formatApiError(err, "管理员登录失败");
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="login-container">
    <!-- Brand -->
    <div class="top-brand">
      <div class="brand-icon">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
        </svg>
      </div>
      <div class="brand-text">
        <strong>智慧云脑</strong>
        <span>管理控制台</span>
      </div>
    </div>

    <!-- Login Card -->
    <div class="login-card">
      <div class="card-header">
        <div class="lock-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
            <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
          </svg>
        </div>
        <h2>管理员登录</h2>
        <p>登录后进入管理控制台，管理科室、医生、排班和系统配置。</p>
      </div>

      <div v-if="error" class="error-alert">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
          <circle cx="8" cy="8" r="7" stroke="currentColor" stroke-width="1.5"/>
          <path d="M8 4.5v4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          <circle cx="8" cy="11" r="0.75" fill="currentColor"/>
        </svg>
        <span>{{ error }}</span>
      </div>

      <form class="form-body" @submit.prevent="submit">
        <label class="field">
          <span class="field-label">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
              <circle cx="12" cy="7" r="4"/>
            </svg>
            账号
          </span>
          <div class="input-wrap">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
              <circle cx="12" cy="7" r="4"/>
            </svg>
            <input v-model.trim="form.account" placeholder="请输入管理员账号" autocomplete="username" />
          </div>
        </label>
        <label class="field">
          <span class="field-label">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
            </svg>
            密码
          </span>
          <div class="input-wrap">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
            </svg>
            <input v-model="form.password" type="password" placeholder="请输入密码" autocomplete="current-password" />
          </div>
        </label>
        <button :class="['submit-btn', { loading }]" type="submit" :disabled="loading">
          {{ loading ? "" : "进入管理端" }}
          <svg v-if="!loading" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M5 12h14"/>
            <path d="m12 5 7 7-7 7"/>
          </svg>
        </button>
      </form>

      <div class="divider">测试凭据</div>

      <div class="quick-hint">
        账号 <code>admin</code> · 密码 <code>123456</code>
      </div>
    </div>

    <div class="login-footer">
      <p>© 2026 智慧云脑 · <a href="#">隐私政策</a> · <a href="#">使用条款</a></p>
    </div>
  </div>
</template>
