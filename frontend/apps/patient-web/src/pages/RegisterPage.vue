<script setup lang="ts">
import { onBeforeUnmount, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { api, formatApiError } from "@smart-cloud-brain/shared-api";
import { Alert, Button, Input } from "@smart-cloud-brain/shared-ui";

const router = useRouter();
const loading = ref(false);
const error = ref("");
const notice = ref("");
const redirectTimer = ref<number | null>(null);

onBeforeUnmount(() => {
  if (redirectTimer.value !== null) clearTimeout(redirectTimer.value);
});
const form = reactive({
  name: "",
  phone: "",
  password: "",
  gender: "FEMALE",
  age: 30,
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
    redirectTimer.value = window.setTimeout(() => router.push({ name: "patient-login" }), 600);
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
      <Alert v-if="error" variant="danger" :description="error" />
      <Alert v-if="notice" variant="success" :description="notice" />
      <div class="auth-form-grid">
        <Input v-model="form.name" label="姓名" placeholder="请输入姓名" />
        <Input v-model="form.phone" label="手机号" placeholder="请输入11位手机号" />
        <Input v-model="form.password" label="密码" type="password" placeholder="至少6位" autocomplete="new-password" />
        <Input v-model.number="form.age" label="年龄" type="number" />
        <FormField label="性别">
            <select v-model="form.gender">
              <option value="FEMALE">女</option>
              <option value="MALE">男</option>
              <option value="UNKNOWN">未说明</option>
            </select>
          </FormField>
      </div>
      <Button type="submit" :loading="loading" class="w-full">
        {{ loading ? "注册中" : "创建档案并继续" }}
      </Button>
    </form>
  </section>
</template>
