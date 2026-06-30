<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { api, formatApiError } from "@smart-cloud-brain/shared-api";
import { Alert, Button, Input } from "@smart-cloud-brain/shared-ui";

const router = useRouter();
const loading = ref(false);
const codeLoading = ref(false);
const countdown = ref(0);
const error = ref("");
const notice = ref("");
let countdownTimer: number | undefined;

const form = reactive({
  name: "",
  phone: "",
  email: "",
  emailCode: "",
  password: "",
  gender: "FEMALE",
  age: 30,
});

const emailReady = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim()));
const sendCodeText = computed(() => {
  if (codeLoading.value) return "发送中";
  if (countdown.value > 0) return `${countdown.value}s`;
  return "发送验证码";
});
const ageModel = computed({
  get: () => String(form.age),
  set: (value: string) => {
    form.age = Number(value) || 0;
  },
});

function validate() {
  if (!form.name.trim()) return "请输入姓名。";
  if (!/^1\d{10}$/.test(form.phone.trim())) return "请输入 11 位手机号。";
  if (!emailReady.value) return "请输入有效邮箱。";
  if (!/^\d{6}$/.test(form.emailCode.trim())) return "请输入 6 位邮箱验证码。";
  if (form.password.length < 6) return "密码至少 6 位。";
  if (form.age < 0 || form.age > 120) return "请输入有效年龄。";
  return "";
}

function startCountdown(seconds = 60) {
  window.clearInterval(countdownTimer);
  countdown.value = seconds;
  countdownTimer = window.setInterval(() => {
    countdown.value -= 1;
    if (countdown.value <= 0) {
      window.clearInterval(countdownTimer);
      countdownTimer = undefined;
      countdown.value = 0;
    }
  }, 1000);
}

async function sendEmailCode() {
  error.value = "";
  notice.value = "";
  if (!emailReady.value) {
    error.value = "请输入有效邮箱。";
    return;
  }
  if (!/^1\d{10}$/.test(form.phone.trim())) {
    error.value = "请先填写 11 位手机号。";
    return;
  }
  codeLoading.value = true;
  try {
    await api.sendPatientEmailCode({ email: form.email.trim(), phone: form.phone.trim(), purpose: "REGISTER" });
    notice.value = "验证码已发送，请查看邮箱。";
    startCountdown(60);
  } catch (err) {
    error.value = formatApiError(err, "验证码发送失败");
  } finally {
    codeLoading.value = false;
  }
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
    await api.registerPatient({
      ...form,
      phone: form.phone.trim(),
      email: form.email.trim(),
      emailCode: form.emailCode.trim(),
    });
    notice.value = "注册成功，请登录后继续分诊和挂号。";
    window.setTimeout(() => router.push({ name: "patient-login" }), 600);
  } catch (err) {
    error.value = formatApiError(err, "注册失败");
  } finally {
    loading.value = false;
  }
}

onBeforeUnmount(() => window.clearInterval(countdownTimer));
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
      <Alert v-if="error" variant="danger" :description="error" />
      <Alert v-if="notice" variant="success" :description="notice" />
      <div class="auth-form-grid">
        <Input v-model="form.name" label="姓名" placeholder="请输入姓名" />
        <Input v-model="form.phone" label="手机号" placeholder="请输入 11 位手机号" />
        <Input v-model="form.email" label="邮箱" type="email" placeholder="请输入接收验证码的邮箱" />
        <div class="auth-code-field">
          <Input v-model="form.emailCode" label="邮箱验证码" inputmode="numeric" maxlength="6" placeholder="6 位验证码" />
          <button
            type="button"
            class="email-code-action"
            :disabled="codeLoading || countdown > 0 || !emailReady"
            @click="sendEmailCode"
          >
            {{ sendCodeText }}
          </button>
        </div>
        <Input v-model="form.password" label="密码" type="password" placeholder="至少 6 位" autocomplete="new-password" />
        <Input v-model="ageModel" label="年龄" type="number" />
        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-[var(--ink)]">性别</label>
          <select v-model="form.gender" class="h-10 rounded-md border border-[var(--line)] bg-white px-3 text-sm focus-visible:outline-none focus-visible:border-[var(--primary)] focus-visible:ring-2 focus-visible:ring-[var(--focus)]">
            <option value="FEMALE">女</option>
            <option value="MALE">男</option>
            <option value="UNKNOWN">未说明</option>
          </select>
        </div>
      </div>
      <Button type="submit" :loading="loading" class="w-full">
        {{ loading ? "注册中" : "创建档案并继续" }}
      </Button>
    </form>
  </section>
</template>
