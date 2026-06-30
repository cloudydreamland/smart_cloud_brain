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
  <main class="doctor-login-screen">
    <section class="login-visual">
      <div class="brand-line">
        <div class="brand-mark">SCB</div>
        <span>SMART CLOUD BRAIN CLINICAL</span>
      </div>
      <h1>医生接诊工作台</h1>
      <p>把队列、患者上下文、AI 病历草稿、处方风险审核和通知闭环放在同一条临床流程里，减少切换，强化风险提示。</p>
    </section>

    <section class="login-form-wrap">
      <form class="login-card" @submit.prevent="submit">
        <header>
          <p class="eyebrow">医生登录</p>
          <h2>进入接诊工作台</h2>
          <p>登录后进入完整医生端流程。</p>
        </header>
        <div class="form-body">
          <ErrorState v-if="error" :message="error" />
          <label class="field">
            <span>账号</span>
            <input v-model.trim="form.account" autocomplete="username" />
          </label>
          <label class="field">
            <span>密码</span>
            <input v-model="form.password" type="password" autocomplete="current-password" />
          </label>
          <button class="primary" type="submit" :disabled="loading">{{ loading ? "登录中" : "进入工作台" }}</button>
          <span class="hint">真实前端页面，登录仍会调用后端认证接口。</span>
        </div>
      </form>
    </section>
  </main>
</template>
