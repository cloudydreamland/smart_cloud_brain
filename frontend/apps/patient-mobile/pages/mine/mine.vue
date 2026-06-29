<template>
  <view class="page with-tab">
    <view class="hero">
      <text class="eyebrow">我的</text>
      <text class="title">{{ patientName }}</text>
      <text class="subtitle">管理个人资料、家庭成员、消息和移动端连接设置。</text>
    </view>
    <view class="error" v-if="error">{{ error }}</view>
    <view class="panel">
      <view class="record">
        <text class="strong">{{ patientName }}</text>
        <text class="muted">手机号：{{ patient.phone || session.account || "-" }}</text>
        <text class="muted">性别：{{ genderText(patient.gender) }} · 年龄：{{ patient.age || "-" }}</text>
      </view>
      <button @click="go('/pages/profile/profile')">编辑个人资料</button>
    </view>

    <view class="panel">
      <text class="section-title">服务管理</text>
      <view class="quick-grid">
        <button class="quick" @click="go('/pages/visitors/visitors')">家庭成员</button>
        <button class="quick" @click="go('/pages/messages/messages')">消息中心</button>
        <button class="quick" @click="go('/pages/invoices/invoices')">电子发票</button>
        <button class="quick" @click="go('/pages/reports/reports')">检查报告</button>
      </view>
    </view>

    <view class="panel form">
      <text class="section-title">连接设置</text>
      <input v-model="apiBase" placeholder="API 地址" @blur="saveBase" />
      <text class="muted">鸿蒙模拟器无法访问时，可改成 Docker 网关地址。</text>
      <button class="ghost" @click="saveBase">保存地址</button>
      <button class="danger" @click="logout">退出登录</button>
    </view>
  </view>
</template>

<script>
import { api } from "../../common/api.js";
import { getApiBase, getSession, logoutToLogin, requireLogin, saveApiBase } from "../../common/session.js";
import { genderText, text } from "../../common/formatters.js";

export default {
  data() {
    return { patient: {}, session: {}, apiBase: getApiBase(), error: "" };
  },
  computed: {
    patientName() { return text(this.patient.name, this.session.name || "患者"); }
  },
  onShow() {
    if (requireLogin()) this.refresh();
  },
  methods: {
    genderText,
    go(url) { uni.navigateTo({ url }); },
    saveBase() {
      this.apiBase = saveApiBase(this.apiBase);
      uni.showToast({ title: "已保存", icon: "none" });
    },
    logout() { logoutToLogin(); },
    async refresh() {
      this.error = "";
      this.session = getSession() || {};
      try { this.patient = await api.patientInfo(); }
      catch (error) { this.error = error.message || "资料加载失败"; }
    }
  }
};
</script>
