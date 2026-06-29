<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">智慧云脑</text>
      <text class="title">患者登录</text>
      <text class="subtitle">登录后继续分诊、预约和查看诊后资料。</text>
    </view>
    <view class="error" v-if="error">{{ error }}</view>
    <view class="panel form">
      <input v-model="apiBase" placeholder="API 地址，例如 http://localhost:18080/api" @blur="saveBase" />
      <text class="muted">DevEco 模拟器无法访问时，优先尝试 Docker 网关 http://localhost:18080/api。</text>
      <view class="segmented">
        <button v-for="item in apiBaseOptions" :key="item" class="segment" @click="useApiBase(item)">{{ item }}</button>
      </view>
      <input v-model.trim="account" placeholder="手机号" />
      <input v-model="password" password placeholder="密码" />
      <button :disabled="busy || !account || !password" @click="login">{{ busy ? "登录中" : "登录" }}</button>
      <view class="actions">
        <button class="ghost" @click="fillDemo">填入测试账号</button>
        <button class="ghost" @click="goRegister">注册</button>
      </view>
    </view>
  </view>
</template>

<script>
import { api } from "../../common/api.js";
import { getApiBase, isLoggedIn, saveApiBase, saveSession } from "../../common/session.js";

export default {
  data() {
    return { apiBase: getApiBase(), apiBaseOptions: ["/api", "http://localhost:18080/api", "http://127.0.0.1:18080/api"], account: "", password: "", busy: false, error: "" };
  },
  onShow() {
    if (isLoggedIn()) uni.switchTab({ url: "/pages/index/index" });
  },
  methods: {
    saveBase() { this.apiBase = saveApiBase(this.apiBase); },
    useApiBase(value) { this.apiBase = saveApiBase(value); },
    fillDemo() { this.account = "13800000001"; this.password = "123456"; },
    goRegister() { uni.navigateTo({ url: "/pages/auth/register" }); },
    async login() {
      this.busy = true;
      this.error = "";
      this.saveBase();
      try {
        const session = await api.loginPatient(this.account, this.password, this.apiBase);
        saveSession(session);
        uni.switchTab({ url: "/pages/index/index" });
      } catch (error) {
        this.error = error.message || "登录失败";
      } finally {
        this.busy = false;
      }
    }
  }
};
</script>
