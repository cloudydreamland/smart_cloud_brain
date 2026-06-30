<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">患者档案</text>
      <text class="title">创建患者档案</text>
      <text class="subtitle">请填写真实就诊资料，邮箱验证码用于确认注册身份。</text>
    </view>
    <view class="error" v-if="error">{{ error }}</view>
    <view class="notice" v-if="message">{{ message }}</view>
    <view class="panel form">
      <input v-model.trim="form.phone" placeholder="手机号" />
      <input v-model.trim="form.email" type="text" placeholder="邮箱" />
      <view class="code-row">
        <input v-model.trim="form.emailCode" type="number" maxlength="6" placeholder="邮箱验证码" />
        <button class="code-button" :disabled="codeBusy || countdown > 0 || !emailReady" @click="sendCode">
          {{ sendCodeText }}
        </button>
      </view>
      <input v-model="form.password" password placeholder="密码" />
      <input v-model.trim="form.name" placeholder="姓名" />
      <picker mode="selector" :range="genderLabels" :value="genderIndex" @change="onGenderChange">
        <view class="picker">{{ genderLabels[genderIndex] }}</view>
      </picker>
      <input v-model.number="form.age" type="number" placeholder="年龄" />
      <button :disabled="busy || !canSubmit" @click="register">{{ busy ? "提交中" : "创建档案" }}</button>
      <button class="ghost" @click="backLogin">返回登录</button>
    </view>
  </view>
</template>

<script>
import { api } from "../../common/api.js";

export default {
  data() {
    return {
      form: { phone: "", email: "", emailCode: "", password: "", name: "", gender: "MALE", age: "" },
      genders: ["MALE", "FEMALE"],
      genderLabels: ["男", "女"],
      genderIndex: 0,
      busy: false,
      codeBusy: false,
      countdown: 0,
      countdownTimer: null,
      error: "",
      message: ""
    };
  },
  computed: {
    emailReady() { return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.form.email); },
    canSubmit() {
      return this.form.phone && this.form.password && this.form.name && this.emailReady && /^\d{6}$/.test(this.form.emailCode);
    },
    sendCodeText() {
      if (this.codeBusy) return "发送中";
      if (this.countdown > 0) return this.countdown + "s";
      return "发送验证码";
    }
  },
  beforeUnmount() {
    if (this.countdownTimer) clearInterval(this.countdownTimer);
  },
  methods: {
    onGenderChange(event) {
      this.genderIndex = Number(event.detail.value);
      this.form.gender = this.genders[this.genderIndex];
    },
    backLogin() { uni.navigateBack({ fail: () => uni.reLaunch({ url: "/pages/auth/login" }) }); },
    startCountdown() {
      if (this.countdownTimer) clearInterval(this.countdownTimer);
      this.countdown = 60;
      this.countdownTimer = setInterval(() => {
        this.countdown -= 1;
        if (this.countdown <= 0) {
          clearInterval(this.countdownTimer);
          this.countdownTimer = null;
          this.countdown = 0;
        }
      }, 1000);
    },
    async sendCode() {
      this.error = "";
      this.message = "";
      if (!this.emailReady) {
        this.error = "请输入有效邮箱";
        return;
      }
      if (!/^1\d{10}$/.test(this.form.phone)) {
        this.error = "请先填写 11 位手机号";
        return;
      }
      this.codeBusy = true;
      try {
        await api.sendPatientEmailCode({ email: this.form.email, phone: this.form.phone, purpose: "REGISTER" });
        this.message = "验证码已发送，请查看邮箱";
        this.startCountdown();
      } catch (error) {
        this.error = error.message || "验证码发送失败";
      } finally {
        this.codeBusy = false;
      }
    },
    async register() {
      this.busy = true;
      this.error = "";
      this.message = "";
      try {
        await api.registerPatient({ ...this.form, age: Number(this.form.age) || 0 });
        this.message = "档案已创建，请返回登录";
      } catch (error) {
        this.error = error.message || "注册失败";
      } finally {
        this.busy = false;
      }
    }
  }
};
</script>

<style scoped>
.code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 112px;
  gap: 10px;
}

.code-button {
  min-height: 44px;
  font-size: 13px;
}
</style>
