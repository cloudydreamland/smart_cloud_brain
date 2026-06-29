<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">个人资料</text>
      <text class="title">维护基本信息</text>
      <text class="subtitle">资料会用于患者服务展示和医生接诊前核对。</text>
    </view>
    <view class="error" v-if="error">{{ error }}</view>
    <view class="notice" v-if="message">{{ message }}</view>
    <view class="panel form">
      <input v-model.trim="form.name" placeholder="姓名" />
      <picker mode="selector" :range="genderLabels" :value="genderIndex" @change="onGenderChange">
        <view class="picker">{{ genderLabels[genderIndex] }}</view>
      </picker>
      <input v-model.number="form.age" type="number" placeholder="年龄" />
      <input v-model.trim="form.phone" placeholder="手机号" />
      <textarea v-model="form.allergyHistory" placeholder="过敏史" />
      <textarea v-model="form.pastHistory" placeholder="既往史" />
      <button :disabled="busy || !form.name" @click="saveProfile">保存资料</button>
    </view>
    <view class="panel form">
      <text class="section-title">API 地址</text>
      <input v-model="apiBase" placeholder="API 地址" @blur="saveBase" />
      <button class="ghost" @click="saveBase">保存连接设置</button>
    </view>
  </view>
</template>

<script>
import { api } from "../../common/api.js";
import { getApiBase, requireLogin, saveApiBase } from "../../common/session.js";

export default {
  data() {
    return {
      form: { name: "", gender: "MALE", age: "", phone: "", allergyHistory: "", pastHistory: "" },
      genders: ["MALE", "FEMALE"],
      genderLabels: ["男", "女"],
      genderIndex: 0,
      apiBase: getApiBase(),
      busy: false,
      error: "",
      message: ""
    };
  },
  onShow() { if (requireLogin()) this.refresh(); },
  methods: {
    onGenderChange(event) { this.genderIndex = Number(event.detail.value); this.form.gender = this.genders[this.genderIndex]; },
    saveBase() {
      this.apiBase = saveApiBase(this.apiBase);
      this.message = "连接设置已保存";
    },
    applyProfile(patient) {
      this.form = {
        name: patient.name || "",
        gender: patient.gender || "MALE",
        age: patient.age || "",
        phone: patient.phone || "",
        allergyHistory: patient.allergyHistory || "",
        pastHistory: patient.pastHistory || ""
      };
      this.genderIndex = this.form.gender === "FEMALE" ? 1 : 0;
    },
    async refresh() {
      this.error = "";
      try { this.applyProfile(await api.patientInfo()); }
      catch (error) { this.error = error.message || "资料加载失败"; }
    },
    async saveProfile() {
      this.busy = true;
      this.error = "";
      this.message = "";
      try {
        const saved = await api.saveProfile({ ...this.form, age: Number(this.form.age) || 0 });
        this.applyProfile(saved || this.form);
        this.message = "资料已保存";
      } catch (error) {
        this.error = error.message || "保存失败";
      } finally {
        this.busy = false;
      }
    }
  }
};
</script>
