<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { api, formatApiError, useAuthStore } from "@smart-cloud-brain/shared-api";
import { ErrorState, FormField } from "@smart-cloud-brain/shared-ui";

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
  <main class="workspace">
    <form class="panel" style="max-width: 520px; margin: 12vh auto 0" @submit.prevent="submit">
      <header class="panel-header"><div class="panel-title"><p class="eyebrow">ADMIN LOGIN</p><h2>进入管理端</h2><p>登录后可维护基础数据、号源、分诊和知识配置。</p></div></header>
      <div class="panel-body stack">
        <ErrorState v-if="error" :message="error" />
        <FormField label="账号"><input v-model.trim="form.account" autocomplete="username" /></FormField>
        <FormField label="密码"><input v-model="form.password" type="password" autocomplete="current-password" /></FormField>
        <button class="primary" type="submit" :disabled="loading">{{ loading ? "登录中" : "进入管理端" }}</button>
      </div>
    </form>
  </main>
</template>
