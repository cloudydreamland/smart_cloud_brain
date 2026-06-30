<script setup lang="ts">
import { inject, onMounted, reactive, ref, type Ref } from "vue";
import { api, formatApiError, type EmailConfigSaveRequest } from "@smart-cloud-brain/shared-api";
import { Alert, Toast } from "@smart-cloud-brain/shared-ui";

const toast = inject<Ref<InstanceType<typeof Toast>>>("toast");
const loading = ref(false);
const saving = ref(false);
const testing = ref(false);
const error = ref("");
const passwordSet = ref(false);
const testAddress = ref("");

const form = reactive<EmailConfigSaveRequest>({
  host: "",
  port: 465,
  username: "",
  password: "",
  fromAddress: "",
  fromName: "",
  sslEnabled: true,
  starttlsEnabled: false,
  enabled: false,
});

async function refresh() {
  loading.value = true;
  error.value = "";
  try {
    const config = await api.emailConfig();
    form.host = config.host || "";
    form.port = config.port || 465;
    form.username = config.username || "";
    form.password = "";
    form.fromAddress = config.fromAddress || "";
    form.fromName = config.fromName || "";
    form.sslEnabled = Boolean(config.sslEnabled);
    form.starttlsEnabled = Boolean(config.starttlsEnabled);
    form.enabled = Boolean(config.enabled);
    passwordSet.value = Boolean(config.passwordSet);
  } catch (err) {
    error.value = formatApiError(err, "邮箱配置加载失败");
  } finally {
    loading.value = false;
  }
}

function payload() {
  const body: EmailConfigSaveRequest = {
    host: form.host.trim(),
    port: Number(form.port) || 465,
    username: form.username.trim(),
    fromAddress: form.fromAddress.trim(),
    fromName: form.fromName?.trim() || "",
    sslEnabled: Boolean(form.sslEnabled),
    starttlsEnabled: Boolean(form.starttlsEnabled),
    enabled: Boolean(form.enabled),
  };
  if (form.password?.trim()) {
    body.password = form.password.trim();
  }
  return body;
}

async function save() {
  saving.value = true;
  error.value = "";
  try {
    const config = await api.saveEmailConfig(payload());
    form.password = "";
    passwordSet.value = Boolean(config.passwordSet);
    toast?.value?.success("保存成功", "SMTP 邮箱配置已更新。");
  } catch (err) {
    error.value = formatApiError(err, "邮箱配置保存失败");
  } finally {
    saving.value = false;
  }
}

async function sendTest() {
  testing.value = true;
  error.value = "";
  try {
    await api.testEmailConfig({ toAddress: testAddress.value.trim() });
    toast?.value?.success("测试邮件已发送", "请检查收件箱或垃圾邮件。");
  } catch (err) {
    error.value = formatApiError(err, "测试邮件发送失败");
  } finally {
    testing.value = false;
  }
}

onMounted(refresh);
</script>

<template>
  <section class="catalog-layout">
    <section class="panel email-config-page">
      <header class="panel-header">
        <div class="panel-title">
          <h2>邮箱配置</h2>
          <p>配置患者注册验证码使用的 SMTP 发件邮箱。</p>
        </div>
        <div class="toolbar">
          <button type="button" :disabled="loading" @click="refresh">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" :class="{ spin: loading }"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
            刷新
          </button>
          <button type="button" class="primary" :disabled="saving || loading" @click="save">保存</button>
        </div>
      </header>

      <div class="panel-body email-config-page__body">
        <Alert v-if="error" variant="danger" :description="error" />
        <div class="email-config-page__grid">
          <label>
            <span>SMTP Host</span>
            <input v-model.trim="form.host" placeholder="smtp.example.com" />
          </label>
          <label>
            <span>端口</span>
            <input v-model.number="form.port" type="number" min="1" max="65535" />
          </label>
          <label>
            <span>用户名</span>
            <input v-model.trim="form.username" autocomplete="username" />
          </label>
          <label>
            <span>密码</span>
            <input v-model="form.password" type="password" autocomplete="new-password" :placeholder="passwordSet ? '留空则保留原密码' : '请输入 SMTP 密码'" />
          </label>
          <label>
            <span>发件地址</span>
            <input v-model.trim="form.fromAddress" type="email" placeholder="no-reply@example.com" />
          </label>
          <label>
            <span>发件名称</span>
            <input v-model.trim="form.fromName" placeholder="智慧云脑诊疗平台" />
          </label>
        </div>

        <div class="email-config-page__switches">
          <label><input v-model="form.sslEnabled" type="checkbox" /> SSL</label>
          <label><input v-model="form.starttlsEnabled" type="checkbox" /> STARTTLS</label>
          <label><input v-model="form.enabled" type="checkbox" /> 启用邮件发送</label>
        </div>

        <div class="email-config-page__test">
          <label>
            <span>测试收件邮箱</span>
            <input v-model.trim="testAddress" type="email" placeholder="receiver@example.com" />
          </label>
          <button type="button" class="primary" :disabled="testing || !testAddress" @click="sendTest">
            {{ testing ? "发送中" : "发送测试邮件" }}
          </button>
        </div>
      </div>
    </section>
  </section>
</template>

<style scoped>
.email-config-page__body {
  display: grid;
  gap: 20px;
}

.email-config-page__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.email-config-page label {
  display: grid;
  gap: 8px;
  color: var(--color-slate-600);
  font-size: 13px;
  font-weight: 700;
}

.email-config-page input {
  width: 100%;
  min-height: 42px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--color-white);
  color: var(--ink);
  padding: 10px 12px;
  font-size: 14px;
}

.email-config-page__switches {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.email-config-page__switches label {
  display: inline-flex;
  grid-template-columns: none;
  align-items: center;
  gap: 8px;
}

.email-config-page__switches input {
  width: 16px;
  min-height: 16px;
}

.email-config-page__test {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 150px;
  align-items: end;
  gap: 12px;
  max-width: 640px;
}

@media (max-width: 720px) {
  .email-config-page__grid,
  .email-config-page__test {
    grid-template-columns: 1fr;
  }
}
</style>
