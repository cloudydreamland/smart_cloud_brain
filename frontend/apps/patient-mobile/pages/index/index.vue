<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">Patient Mobile</text>
      <text class="title">患者移动端</text>
      <text class="subtitle">轻症问诊、AI 分诊、预约挂号、记录查询。</text>
    </view>

    <view class="panel" v-if="!session">
      <text class="section-title">登录</text>
      <input v-model="account" placeholder="手机号" />
      <input v-model="password" password placeholder="密码" />
      <button @click="login">登录患者端</button>
    </view>

    <template v-else>
      <view class="panel">
        <text class="section-title">AI 智能问诊</text>
        <textarea v-model="chiefComplaint" />
        <button @click="triage">提交分诊</button>
        <text v-if="triageResult" class="result">{{ triageResult.recommendedDepartment }}：{{ triageResult.reason }}</text>
      </view>

      <view class="panel">
        <text class="section-title">快速预约</text>
        <input v-model="doctorId" type="number" placeholder="医生ID" />
        <input v-model="departmentId" type="number" placeholder="科室ID" />
        <input v-model="appointmentTime" placeholder="2026-06-15T09:00:00" />
        <button @click="createRegistration">确认预约</button>
      </view>

      <view class="panel">
        <text class="section-title">我的记录</text>
        <button @click="refreshRecords">刷新记录</button>
        <text v-for="item in registrations" :key="item.registrationId" class="line">挂号 #{{ item.registrationId }} {{ item.status }}</text>
        <text v-for="item in medicalRecords" :key="item.medicalRecordId" class="line">病历 #{{ item.medicalRecordId }} {{ item.diagnosis }}</text>
        <text v-for="item in prescriptions" :key="item.prescriptionId" class="line">处方 #{{ item.prescriptionId }} 风险 {{ item.riskLevel }}</text>
      </view>
    </template>
  </view>
</template>

<script>
const API_BASE = "http://localhost:8080/api";

function request(path, method = "GET", data = null, token = "") {
  return new Promise((resolve, reject) => {
    uni.request({
      url: API_BASE + path,
      method,
      data,
      header: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: "Bearer " + token } : {})
      },
      success(response) {
        const body = response.data;
        if (body && body.code === 0) {
          resolve(body.data);
        } else {
          reject(new Error((body && body.message) || "请求失败"));
        }
      },
      fail(error) {
        reject(error);
      }
    });
  });
}

export default {
  data() {
    return {
      session: uni.getStorageSync("patient-session") || null,
      account: "13800000001",
      password: "123456",
      chiefComplaint: "咽痛、低热、鼻塞两天，想预约轻症门诊。",
      triageResult: null,
      doctorId: 2,
      departmentId: 2,
      appointmentTime: "2026-06-15T09:00:00",
      registrations: [],
      medicalRecords: [],
      prescriptions: []
    };
  },
  methods: {
    async login() {
      this.session = await request("/patient/login", "POST", { account: this.account, password: this.password });
      uni.setStorageSync("patient-session", this.session);
      await this.refreshRecords();
    },
    async triage() {
      this.triageResult = await request("/triage/consult", "POST", { chiefComplaint: this.chiefComplaint }, this.session.token);
    },
    async createRegistration() {
      await request("/registration/create", "POST", {
        doctorId: Number(this.doctorId),
        departmentId: Number(this.departmentId),
        appointmentTime: this.appointmentTime,
        triageRecordId: this.triageResult ? this.triageResult.triageRecordId : null
      }, this.session.token);
      await this.refreshRecords();
    },
    async refreshRecords() {
      this.registrations = await request("/registration/list", "GET", null, this.session.token);
      this.medicalRecords = await request("/medical-record/list", "GET", null, this.session.token);
      this.prescriptions = await request("/prescription/list", "GET", null, this.session.token);
    }
  }
};
</script>

<style>
.page {
  padding: 28rpx;
}

.hero {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  padding: 28rpx 0;
}

.eyebrow {
  color: #167255;
  font-size: 24rpx;
  font-weight: 700;
}

.title {
  color: #172033;
  font-size: 48rpx;
  font-weight: 800;
}

.subtitle {
  color: #5d6b66;
  font-size: 28rpx;
}

.panel {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  margin-bottom: 24rpx;
  padding: 24rpx;
  border: 1rpx solid #d8e3df;
  border-radius: 12rpx;
  background: #ffffff;
}

.section-title {
  font-size: 32rpx;
  font-weight: 700;
}

input,
textarea {
  min-height: 76rpx;
  border: 1rpx solid #cbd8d4;
  border-radius: 10rpx;
  padding: 16rpx;
  background: #fff;
}

button {
  background: #167255;
  color: #ffffff;
}

.result,
.line {
  color: #31453e;
  font-size: 26rpx;
  line-height: 1.7;
}
</style>
