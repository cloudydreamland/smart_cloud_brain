<template>
  <view class="login-page">
    <view class="login-hero">
      <text class="login-brand">智慧云脑</text>
      <text class="login-title">患者登录</text>
      <text class="login-desc">登录后继续分诊、预约和查看诊后资料。</text>
    </view>

    <view class="login-error" v-if="error">{{ error }}</view>

    <view class="login-card">
      <view class="form-section">
        <text class="field-label">接口地址</text>
        <input class="auth-input" v-model="apiBase" placeholder="例如 http://localhost:18080/api" @blur="saveBase" />
        <text class="field-help">模拟器无法访问时，优先尝试你电脑的热点 IP 或 Docker 网关地址。</text>
        <view class="api-options">
          <view
            v-for="item in apiBaseOptions"
            :key="item"
            class="api-option"
            :class="{ active: apiBase === item }"
            @click="useApiBase(item)"
          >
            {{ item }}
          </view>
        </view>
      </view>

      <view class="form-section">
        <input class="auth-input" v-model.trim="account" placeholder="手机号" />
        <input class="auth-input" v-model="password" password placeholder="密码" />
      </view>

      <button class="login-button" :disabled="busy || !account || !password" @click="login">{{ busy ? "登录中" : "登录" }}</button>

      <view class="login-actions">
        <button class="outline-button" @click="fillDemo">填入测试账号</button>
        <button class="outline-button" @click="goRegister">注册</button>
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

<style scoped>
.login-page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 42rpx 30rpx 64rpx;
  background: #f4fbf7;
  color: #172033;
}

.login-hero {
  padding: 18rpx 4rpx 30rpx;
}

.login-brand,
.login-title,
.login-desc,
.field-label,
.field-help {
  display: block;
}

.login-brand {
  color: #167255;
  font-size: 24rpx;
  font-weight: 700;
  line-height: 34rpx;
}

.login-title {
  margin-top: 8rpx;
  color: #172033;
  font-size: 42rpx;
  font-weight: 800;
  line-height: 54rpx;
}

.login-desc {
  margin-top: 12rpx;
  color: #5d6b66;
  font-size: 26rpx;
  line-height: 42rpx;
}

.login-error {
  margin-bottom: 20rpx;
  padding: 18rpx 22rpx;
  border: 1rpx solid #f0c6c6;
  border-radius: 12rpx;
  background: #fff4f4;
  color: #9b2f2f;
  font-size: 26rpx;
  line-height: 38rpx;
}

.login-card {
  box-sizing: border-box;
  padding: 24rpx;
  border: 1rpx solid #d8e3df;
  border-radius: 14rpx;
  background: #ffffff;
}

.form-section {
  margin-bottom: 20rpx;
}

.field-label {
  margin-bottom: 10rpx;
  color: #172033;
  font-size: 26rpx;
  font-weight: 700;
  line-height: 36rpx;
}

.field-help {
  margin-top: 12rpx;
  color: #5d6b66;
  font-size: 24rpx;
  line-height: 38rpx;
}

.auth-input {
  width: 100%;
  height: 82rpx;
  min-height: 82rpx;
  box-sizing: border-box;
  margin-bottom: 14rpx;
  padding: 0 22rpx;
  border: 1rpx solid #cbd8d4;
  border-radius: 12rpx;
  background: #ffffff;
  color: #172033;
  font-size: 28rpx;
  line-height: 82rpx;
}

.api-options {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  margin-top: 14rpx;
}

.api-option {
  box-sizing: border-box;
  min-height: 58rpx;
  margin: 0 12rpx 12rpx 0;
  padding: 12rpx 18rpx;
  border: 1rpx solid #cbd8d4;
  border-radius: 10rpx;
  background: #ffffff;
  color: #51625c;
  font-size: 23rpx;
  line-height: 32rpx;
}

.api-option.active {
  border-color: #167255;
  background: #eaf8f2;
  color: #116044;
  font-weight: 700;
}

.login-button,
.outline-button {
  box-sizing: border-box;
  height: 82rpx;
  min-height: 82rpx;
  margin: 0;
  padding: 0;
  border-radius: 12rpx;
  font-size: 28rpx;
  line-height: 82rpx;
}

.login-button {
  width: 100%;
  background: #167255;
  color: #ffffff;
}

.login-button[disabled] {
  opacity: 0.45;
}

.login-actions {
  display: flex;
  flex-direction: row;
  margin-top: 20rpx;
}

.outline-button {
  flex: 1;
  border: 1rpx solid #bfd8d0;
  background: #ffffff;
  color: #167255;
}

.outline-button + .outline-button {
  margin-left: 16rpx;
}

button::after {
  border: 0;
}
</style>
