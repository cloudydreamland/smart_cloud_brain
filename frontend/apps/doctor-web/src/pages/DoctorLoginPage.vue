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
    error.value = "请输入医生账号和密码。";
    return;
  }
  loading.value = true;
  error.value = "";
  try {
    const session = await api.loginDoctor(form.account.trim(), form.password);
    auth.save("doctor-session", session, "DOCTOR");
    if (!auth.permissionError) await router.push(String(route.query.redirect || "/"));
  } catch (err) {
    error.value = formatApiError(err, "医生登录失败，请确认网关和认证服务已启动。");
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="login-screen">
    <!-- Left Visual Panel -->
    <section class="login-visual">
      <div class="brand-line">
        <div class="brand-mark">SCB</div>
        <span>Smart Cloud Brain Clinical</span>
      </div>

      <h1>医生接诊工作台</h1>
      <p>把队列、患者上下文、AI 病历草稿、处方风险审核和通知闭环放在同一条临床流程里，减少切换，强化风险提示。</p>

      <div class="signal-panel">
        <div class="feature-card">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 2a4 4 0 0 0-4 4v2H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V10a2 2 0 0 0-2-2h-2V6a4 4 0 0 0-4-4z"/>
            <circle cx="12" cy="15" r="2"/>
          </svg>
          <strong>AI 辅助诊断</strong>
          <span>基于患者主诉自动生成病历草稿，辅助医生快速完成诊断记录。</span>
        </div>
        <div class="feature-card">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M22 21v-2a4 4 0 0 0-3-3.87"/>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
          <strong>智能分诊</strong>
          <span>根据症状和体征自动推荐科室与医生，缩短患者候诊时间。</span>
        </div>
        <div class="feature-card">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 12l2 2 4-4"/>
            <path d="M21 12c0 4.97-4.03 9-9 9s-9-4.03-9-9 4.03-9 9-9c1.5 0 2.91.37 4.15 1.02"/>
          </svg>
          <strong>处方风险审核</strong>
          <span>AI 自动审核药物相互作用与禁忌，保障用药安全。</span>
        </div>
      </div>

      <div class="visual-bottom-accent"></div>
    </section>

    <!-- Right Form Panel -->
    <section class="login-form-area">
      <div class="login-card">
        <p class="eyebrow">医生登录</p>
        <h2>进入接诊工作台</h2>
        <p class="subtitle">登录后进入完整医生端流程。</p>

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
            <span>账号</span>
            <input v-model.trim="form.account" placeholder="请输入手机号" autocomplete="username" />
          </label>
          <label class="field">
            <span>密码</span>
            <input v-model="form.password" type="password" placeholder="请输入密码" autocomplete="current-password" />
          </label>
          <button :class="['submit-btn', { loading }]" type="submit" :disabled="loading">
            {{ loading ? "" : "进入工作台" }}
          </button>
        </form>

        <div class="form-footer">
          <p>测试账号：13900000001 / 123456</p>
        </div>
      </div>
    </section>
  </div>
</template>
