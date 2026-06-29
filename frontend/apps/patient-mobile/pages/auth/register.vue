<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">患者档案</text>
      <text class="title">创建患者档案</text>
      <text class="subtitle">请填写真实就诊资料，便于医生核对身份。</text>
    </view>
    <view class="error" v-if="error">{{ error }}</view>
    <view class="notice" v-if="message">{{ message }}</view>
    <view class="panel form">
      <input v-model.trim="form.phone" placeholder="手机号" />
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
      form: { phone: "", password: "", name: "", gender: "MALE", age: "" },
      genders: ["MALE", "FEMALE"],
      genderLabels: ["男", "女"],
      genderIndex: 0,
      busy: false,
      error: "",
      message: ""
    };
  },
  computed: {
    canSubmit() { return this.form.phone && this.form.password && this.form.name; }
  },
  methods: {
    onGenderChange(event) {
      this.genderIndex = Number(event.detail.value);
      this.form.gender = this.genders[this.genderIndex];
    },
    backLogin() { uni.navigateBack({ fail: () => uni.reLaunch({ url: "/pages/auth/login" }) }); },
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
