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
      <input v-model.trim="form.address" placeholder="常住地址或通信地址" />
      <input v-model.trim="form.emergencyContact" placeholder="紧急联系人" />
      <input v-model.trim="form.emergencyPhone" placeholder="紧急联系电话" />
      <picker mode="selector" :range="bloodTypeLabels" :value="bloodTypeIndex" @change="onBloodTypeChange">
        <view class="picker">{{ bloodTypeLabels[bloodTypeIndex] }}</view>
      </picker>
      <input v-model.number="form.heightCm" type="number" placeholder="身高（cm）" />
      <input v-model.number="form.weightKg" type="number" placeholder="体重（kg）" />
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
      form: { name: "", gender: "MALE", age: "", phone: "", address: "", emergencyContact: "", emergencyPhone: "", bloodType: "", heightCm: "", weightKg: "", allergyHistory: "", pastHistory: "" },
      genders: ["MALE", "FEMALE"],
      genderLabels: ["男", "女"],
      genderIndex: 0,
      bloodTypes: ["", "A", "B", "AB", "O"],
      bloodTypeLabels: ["血型未说明", "A 型", "B 型", "AB 型", "O 型"],
      bloodTypeIndex: 0,
      apiBase: getApiBase(),
      busy: false,
      error: "",
      message: ""
    };
  },
  onShow() { if (requireLogin()) this.refresh(); },
  methods: {
    onGenderChange(event) { this.genderIndex = Number(event.detail.value); this.form.gender = this.genders[this.genderIndex]; },
    onBloodTypeChange(event) { this.bloodTypeIndex = Number(event.detail.value); this.form.bloodType = this.bloodTypes[this.bloodTypeIndex]; },
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
        address: patient.address || "",
        emergencyContact: patient.emergencyContact || "",
        emergencyPhone: patient.emergencyPhone || "",
        bloodType: patient.bloodType || "",
        heightCm: patient.heightCm || "",
        weightKg: patient.weightKg || "",
        allergyHistory: patient.allergyHistory || "",
        pastHistory: patient.pastHistory || ""
      };
      this.genderIndex = this.form.gender === "FEMALE" ? 1 : 0;
      this.bloodTypeIndex = Math.max(0, this.bloodTypes.indexOf(this.form.bloodType));
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
        const saved = await api.saveProfile({ ...this.form, age: Number(this.form.age) || 0, heightCm: Number(this.form.heightCm) || 0, weightKg: Number(this.form.weightKg) || 0 });
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
