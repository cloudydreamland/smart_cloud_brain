<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">患者移动端</text>
      <text class="title">患者移动端</text>
      <text class="subtitle">登录、智能分诊、选择真实号源、预约挂号、查看诊疗记录</text>
    </view>

    <view class="notice" v-if="message">
      <text>{{ message }}</text>
    </view>

    <view class="panel" v-if="!session">
      <text class="section-title">登录</text>
      <input v-model="apiBase" placeholder="接口地址" @blur="saveApiBase" />
      <input v-model="account" placeholder="手机号" />
      <input v-model="password" password placeholder="密码" />
      <button :disabled="busy" @click="login">登录患者端</button>
    </view>

    <template v-else>
      <view class="panel user-card">
        <view>
          <text class="section-title">{{ session.name || "患者" }}</text>
          <text class="muted">{{ session.role }} #{{ session.userId }}</text>
        </view>
        <button class="ghost" :disabled="busy" @click="logout">退出</button>
      </view>

      <view class="panel">
        <text class="section-title">智能分诊</text>
        <textarea v-model="chiefComplaint" placeholder="请描述主要症状、持续时间和就诊诉求" />
        <button :disabled="busy || !chiefComplaint.trim()" @click="triage">提交分诊</button>
        <view v-if="triageResult" class="result">
          <text class="strong">{{ triageResult.recommendedDepartment || "待推荐科室" }}</text>
          <text>{{ triageResult.reason || "已生成分诊建议" }}</text>
          <text class="muted">分诊记录 #{{ triageResult.triageRecordId || "-" }} {{ triageResult.status || "" }}</text>
        </view>
      </view>

      <view class="panel">
        <view class="panel-head">
          <text class="section-title">选择真实号源</text>
          <button class="ghost small" :disabled="busy" @click="reloadSlots">刷新号源</button>
        </view>

        <view v-if="triageResult" class="hint">
          <text>推荐科室：{{ triageResult && triageResult.recommendedDepartment ? triageResult.recommendedDepartment : "暂无" }}</text>
        </view>

        <picker
          mode="selector"
          :range="slotLabels"
          :value="selectedSlotIndex < 0 ? 0 : selectedSlotIndex"
          :disabled="!slotLabels.length"
          @change="onSlotChange"
        >
          <view class="picker">
            <text>{{ selectedSlot ? slotLabel(selectedSlot) : "请选择数据库号源" }}</text>
          </view>
        </picker>

        <text v-if="!slots.length" class="empty">暂无可预约号源，请先由管理端发布排班。</text>
        <text v-else-if="!slotLabels.length" class="empty">暂无匹配推荐科室的号源，可刷新后重试。</text>
        <button :disabled="busy || !selectedSlot" @click="createRegistration">确认预约</button>
      </view>

      <view class="panel">
        <view class="panel-head">
          <text class="section-title">我的挂号</text>
          <button class="ghost small" :disabled="busy" @click="reloadRecords">刷新</button>
        </view>
        <view v-for="item in registrations" :key="String(item.registrationId)" class="record">
          <text class="strong">#{{ item.registrationId }} {{ item.departmentName || "科室" }}</text>
          <text>{{ item.doctorName || "医生" }} · {{ item.appointmentTime || "时间待定" }}</text>
          <text class="muted">状态：{{ item.status }}</text>
          <button
            class="danger"
            :disabled="busy || item.status === 'CANCELLED'"
            @click="cancelRegistration(item.registrationId)"
          >
            取消预约
          </button>
        </view>
        <text v-if="!registrations.length" class="empty">暂无挂号记录。</text>
      </view>

      <view class="panel">
        <text class="section-title">病历</text>
        <view v-for="item in medicalRecords" :key="String(item.medicalRecordId)" class="record">
          <text class="strong">病历 #{{ item.medicalRecordId }}</text>
          <text>{{ item.diagnosis || "暂无诊断" }}</text>
          <text class="muted">{{ item.treatmentAdvice || "暂无治疗建议" }}</text>
        </view>
        <text v-if="!medicalRecords.length" class="empty">暂无病历记录。</text>
      </view>

      <view class="panel">
        <text class="section-title">处方</text>
        <view v-for="item in prescriptions" :key="String(item.prescriptionId)" class="record">
          <text class="strong">处方 #{{ item.prescriptionId }}</text>
          <text>风险：{{ item.riskLevel || "未评估" }}</text>
          <text class="muted">状态：{{ item.status || "暂无" }}</text>
        </view>
        <text v-if="!prescriptions.length" class="empty">暂无处方记录。</text>
      </view>
    </template>
  </view>
</template>

<script>
const DEFAULT_API_BASE = "/api";
const API_BASE_KEY = "patient-mobile-api-base";
const SESSION_KEY = "patient-session";

function normalizeApiBase(value) {
  return String(value || DEFAULT_API_BASE).replace(/\/+$/, "");
}

function configuredApiBase() {
  const stored = uni.getStorageSync(API_BASE_KEY);
  const globalConfig = typeof globalThis !== "undefined" ? globalThis.SMART_CLOUD_BRAIN_API_BASE : "";
  const envConfig = typeof process !== "undefined" && process.env
    ? process.env.UNI_APP_API_BASE || process.env.VUE_APP_API_BASE
    : "";
  return normalizeApiBase(stored || globalConfig || envConfig || DEFAULT_API_BASE);
}

function request(apiBase, path, method = "GET", data = null, token = "") {
  return new Promise((resolve, reject) => {
    uni.request({
      url: normalizeApiBase(apiBase) + path,
      method,
      data,
      header: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: "Bearer " + token } : {})
      },
      success(response) {
        const body = response.data || {};
        if (response.statusCode >= 200 && response.statusCode < 300 && body.code === 0) {
          resolve(body.data);
          return;
        }
        reject(new Error(body.message || "请求失败"));
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
      apiBase: configuredApiBase(),
      session: uni.getStorageSync(SESSION_KEY) || null,
      account: "",
      password: "",
      chiefComplaint: "",
      triageResult: null,
      slots: [],
      selectedSlotIndex: -1,
      registrations: [],
      medicalRecords: [],
      prescriptions: [],
      message: "",
      busy: false
    };
  },
  computed: {
    visibleSlots() {
      const departmentName = String(this.triageResult && this.triageResult.recommendedDepartment || "");
      const matched = this.slots.filter((slot) => {
        return !departmentName || String(slot.departmentName || "").includes(departmentName);
      });
      return matched.length ? matched : this.slots;
    },
    slotLabels() {
      return this.visibleSlots.map((slot) => this.slotLabel(slot));
    },
    selectedSlot() {
      if (this.selectedSlotIndex < 0) {
        return null;
      }
      return this.visibleSlots[this.selectedSlotIndex] || null;
    }
  },
  async onLoad() {
    if (this.session) {
      try {
        await this.refreshRecords();
      } catch (error) {
        this.message = error && error.message ? error.message : "加载记录失败";
      }
    }
  },
  methods: {
    token() {
      return this.session && this.session.token || "";
    },
    saveApiBase() {
      this.apiBase = normalizeApiBase(this.apiBase);
      uni.setStorageSync(API_BASE_KEY, this.apiBase);
    },
    async run(label, task) {
      this.busy = true;
      this.message = "";
      this.saveApiBase();
      try {
        await task();
        this.message = label + "成功";
      } catch (error) {
        this.message = error && error.message ? error.message : label + "失败";
      } finally {
        this.busy = false;
      }
    },
    api(path, method = "GET", data = null) {
      return request(this.apiBase, path, method, data, this.token());
    },
    async login() {
      await this.run("登录", async () => {
        this.session = await request(this.apiBase, "/patient/login", "POST", {
          account: this.account,
          password: this.password
        });
        uni.setStorageSync(SESSION_KEY, this.session);
        await this.refreshRecords();
      });
    },
    logout() {
      this.session = null;
      this.triageResult = null;
      this.slots = [];
      this.selectedSlotIndex = -1;
      this.registrations = [];
      this.medicalRecords = [];
      this.prescriptions = [];
      uni.removeStorageSync(SESSION_KEY);
      this.message = "已退出";
    },
    async triage() {
      await this.run("分诊", async () => {
        this.triageResult = await this.api("/triage/consult", "POST", {
          chiefComplaint: this.chiefComplaint.trim()
        });
        await this.refreshSlots();
      });
    },
    async reloadSlots() {
      await this.run("刷新号源", async () => {
        await this.refreshSlots();
      });
    },
    async refreshSlots() {
      const nextSlots = await this.api("/registration/slots");
      this.slots = Array.isArray(nextSlots) ? nextSlots : [];
      this.selectedSlotIndex = -1;
    },
    onSlotChange(event) {
      this.selectedSlotIndex = Number(event.detail.value);
    },
    slotLabel(slot) {
      return [
        slot.departmentName || "科室",
        slot.doctorName || "医生",
        slot.startTime || "时间待定",
        "余号 " + (slot.remainingCapacity == null ? 0 : slot.remainingCapacity)
      ].join(" · ");
    },
    async createRegistration() {
      if (!this.selectedSlot) {
        this.message = "请先选择数据库号源";
        return;
      }
      await this.run("预约", async () => {
        await this.api("/registration/create", "POST", {
          doctorId: Number(this.selectedSlot.doctorId),
          departmentId: Number(this.selectedSlot.departmentId),
          appointmentTime: String(this.selectedSlot.startTime),
          triageRecordId: this.triageResult && this.triageResult.triageRecordId ? this.triageResult.triageRecordId : null,
          slotId: Number(this.selectedSlot.slotId)
        });
        await this.refreshRecords();
      });
    },
    async cancelRegistration(registrationId) {
      await this.run("取消预约", async () => {
        await this.api("/registration/cancel", "POST", {
          registrationId: Number(registrationId)
        });
        await this.refreshRecords();
      });
    },
    async reloadRecords() {
      await this.run("刷新记录", async () => {
        await this.refreshRecords();
      });
    },
    async refreshRecords() {
      await this.refreshSlots();
      const [registrations, medicalRecords, prescriptions] = await Promise.all([
        this.api("/registration/list"),
        this.api("/medical-record/list"),
        this.api("/prescription/list")
      ]);
      this.registrations = Array.isArray(registrations) ? registrations : [];
      this.medicalRecords = Array.isArray(medicalRecords) ? medicalRecords : [];
      this.prescriptions = Array.isArray(prescriptions) ? prescriptions : [];
    }
  }
};
</script>

<style>
page {
  background: #f4fbf7;
}

.page {
  min-height: 100vh;
  padding: 28rpx;
  color: #172033;
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

.subtitle,
.muted,
.empty,
.hint {
  color: #5d6b66;
  font-size: 26rpx;
  line-height: 1.6;
}

.notice {
  margin-bottom: 20rpx;
  padding: 18rpx 22rpx;
  border: 1rpx solid #c5e4d8;
  border-radius: 10rpx;
  background: #eaf8f2;
  color: #116044;
  font-size: 26rpx;
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

.user-card,
.panel-head {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
}

.section-title {
  font-size: 32rpx;
  font-weight: 700;
}

.strong {
  color: #172033;
  font-size: 28rpx;
  font-weight: 700;
}

input,
textarea,
.picker {
  min-height: 76rpx;
  box-sizing: border-box;
  border: 1rpx solid #cbd8d4;
  border-radius: 10rpx;
  padding: 16rpx;
  background: #fff;
  color: #172033;
  font-size: 28rpx;
}

textarea {
  width: 100%;
  min-height: 180rpx;
}

button {
  min-height: 76rpx;
  border-radius: 10rpx;
  background: #167255;
  color: #ffffff;
  font-size: 28rpx;
}

button[disabled] {
  opacity: 0.55;
}

.ghost {
  border: 1rpx solid #bfd8d0;
  background: #ffffff;
  color: #167255;
}

.small {
  min-height: 60rpx;
  padding: 0 20rpx;
  font-size: 24rpx;
}

.danger {
  margin-top: 8rpx;
  background: #b33a3a;
}

.result,
.record {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  padding: 18rpx;
  border-radius: 10rpx;
  background: #f6faf8;
  color: #31453e;
  font-size: 26rpx;
  line-height: 1.6;
}
</style>
