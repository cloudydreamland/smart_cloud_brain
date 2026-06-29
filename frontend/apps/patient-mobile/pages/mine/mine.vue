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
      <input v-model="apiBase" placeholder="API 地址，例如 http://localhost:18080/api" @blur="saveBase" />
      <text class="muted">鸿蒙模拟器无法访问时，可改成 Docker 网关 http://localhost:18080/api。</text>
      <view class="segmented">
        <button v-for="item in apiBaseOptions" :key="item" class="segment" @click="useApiBase(item)">{{ item }}</button>
      </view>
      <button class="ghost" @click="saveBase">保存地址</button>
      <button class="ghost" :disabled="diagnosing" @click="runDiagnostics">{{ diagnosing ? "自检中" : "连接自检" }}</button>
      <button class="danger" @click="logout">退出登录</button>
    </view>

    <view class="panel" v-if="diagnostics.length">
      <view class="panel-head">
        <text class="section-title">连接自检结果</text>
        <text class="muted">{{ diagnosticTime }}</text>
      </view>
      <view v-for="item in diagnostics" :key="item.name" class="record">
        <text class="strong">{{ item.name }}</text>
        <text :class="item.ok ? 'status-ok' : 'status-bad'">{{ item.ok ? "成功" : "失败" }}</text>
        <text class="muted">{{ item.detail }}</text>
      </view>
      <text class="muted">如果接口失败，请优先截图本区域，再尝试切换 API 地址。</text>
    </view>
  </view>
</template>

<script>
import { api, request } from "../../common/api.js";
import { getApiBase, getSession, logoutToLogin, requireLogin, saveApiBase } from "../../common/session.js";
import { genderText, text } from "../../common/formatters.js";

export default {
  data() {
    return {
      patient: {},
      session: {},
      apiBase: getApiBase(),
      apiBaseOptions: ["/api", "http://localhost:18080/api", "http://127.0.0.1:18080/api"],
      diagnostics: [],
      diagnosticTime: "",
      diagnosing: false,
      error: ""
    };
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
    useApiBase(value) { this.apiBase = saveApiBase(value); },
    saveBase() {
      this.apiBase = saveApiBase(this.apiBase);
      uni.showToast({ title: "已保存", icon: "none" });
    },
    logout() { logoutToLogin(); },
    async checkEndpoint(name, path) {
      try {
        const data = await request(path, "GET", null, { apiBase: this.apiBase, skipAuthRedirect: true });
        const size = Array.isArray(data) ? `${data.length} 条` : data ? "有返回" : "空返回";
        return { name, ok: true, detail: size };
      } catch (error) {
        return { name, ok: false, detail: error.message || "请求失败" };
      }
    },
    async runDiagnostics() {
      this.diagnosing = true;
      this.error = "";
      this.apiBase = saveApiBase(this.apiBase);
      this.session = getSession() || {};
      const hasToken = Boolean(this.session && this.session.token);
      const results = [
        { name: "API 地址", ok: Boolean(this.apiBase), detail: this.apiBase || "未配置" },
        { name: "登录态", ok: hasToken, detail: hasToken ? `已登录：${this.session.name || "患者"}` : "未登录或 token 缺失" }
      ];
      if (hasToken) {
        results.push(await this.checkEndpoint("/patient/info", "/patient/info"));
        results.push(await this.checkEndpoint("/registration/slots", "/registration/slots"));
        results.push(await this.checkEndpoint("/triage/list", "/triage/list"));
      }
      this.diagnostics = results;
      this.diagnosticTime = new Date().toLocaleTimeString();
      this.diagnosing = false;
    },
    async refresh() {
      this.error = "";
      this.session = getSession() || {};
      try { this.patient = await api.patientInfo(); }
      catch (error) { this.error = error.message || "资料加载失败"; }
    }
  }
};
</script>
