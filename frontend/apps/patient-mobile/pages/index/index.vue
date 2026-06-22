<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">Patient mobile</text>
      <text class="title">Smart Cloud Brain</text>
      <text class="subtitle">Login, profile, triage, real slots, appointments, records and prescriptions.</text>
    </view>

    <view class="notice" v-if="message"><text>{{ message }}</text></view>

    <view class="panel" v-if="!session">
      <text class="section-title">Login</text>
      <input v-model="apiBase" placeholder="API base, e.g. http://localhost:8080/api" @blur="saveApiBase" />
      <input v-model="account" placeholder="Phone" />
      <input v-model="password" password placeholder="Password" />
      <button :disabled="busy" @click="login">Login</button>
    </view>

    <template v-else>
      <view class="panel user-card">
        <view>
          <text class="section-title">{{ session.name || "Patient" }}</text>
          <text class="muted">{{ session.role }} #{{ session.userId }}</text>
        </view>
        <button class="ghost" :disabled="busy" @click="logout">Logout</button>
      </view>

      <view class="panel">
        <text class="section-title">Profile</text>
        <input v-model="profile.name" placeholder="Name" />
        <input v-model="profile.gender" placeholder="Gender: MALE/FEMALE" />
        <input v-model.number="profile.age" type="number" placeholder="Age" />
        <textarea v-model="profile.allergyHistory" placeholder="Allergy history" />
        <textarea v-model="profile.pastHistory" placeholder="Past history" />
        <button :disabled="busy || !profile.name.trim()" @click="saveProfile">Save profile</button>
      </view>

      <view class="panel">
        <text class="section-title">AI triage</text>
        <textarea v-model="chiefComplaint" placeholder="Describe symptoms and duration" />
        <button :disabled="busy || !chiefComplaint.trim()" @click="triage">Submit triage</button>
        <view v-if="triageResult" class="result">
          <text class="strong">{{ triageResult.recommendedDepartment || "Pending department" }}</text>
          <text>{{ triageResult.reason || "Triage result generated" }}</text>
          <text class="muted">#{{ triageResult.triageRecordId || "-" }} {{ triageResult.status || "" }}</text>
        </view>
      </view>

      <view class="panel">
        <view class="panel-head">
          <text class="section-title">Real slots</text>
          <button class="ghost small" :disabled="busy" @click="reloadSlots">Refresh</button>
        </view>
        <picker mode="selector" :range="slotLabels" :value="selectedSlotIndex < 0 ? 0 : selectedSlotIndex" :disabled="!slotLabels.length" @change="onSlotChange">
          <view class="picker"><text>{{ selectedSlot ? slotLabel(selectedSlot) : "Select a database slot" }}</text></view>
        </picker>
        <text v-if="!slots.length" class="empty">No slots. Publish schedules in admin console first.</text>
        <button :disabled="busy || !selectedSlot" @click="createRegistration">Confirm appointment</button>
      </view>

      <view class="panel">
        <view class="panel-head">
          <text class="section-title">Appointments</text>
          <button class="ghost small" :disabled="busy" @click="reloadRecords">Refresh</button>
        </view>
        <view v-for="item in pagedRegistrations" :key="String(item.registrationId)" class="record">
          <text class="strong">#{{ item.registrationId }} {{ item.departmentName || "Department" }}</text>
          <text>{{ item.doctorName || "Doctor" }} / {{ item.appointmentTime || "Time pending" }}</text>
          <text class="muted">{{ item.status }}</text>
          <button class="danger" :disabled="busy || item.status === 'CANCELLED'" @click="cancelRegistration(item.registrationId)">Cancel</button>
        </view>
        <view v-if="registrations.length > pageSize" class="pagination">
          <text>{{ registrationsPage }} / {{ pageCount(registrations) }}</text>
          <view class="pagination-actions">
            <button class="ghost small" :disabled="registrationsPage <= 1" @click="changePage('registrationsPage', registrations, -1)">Prev</button>
            <button class="ghost small" :disabled="registrationsPage >= pageCount(registrations)" @click="changePage('registrationsPage', registrations, 1)">Next</button>
          </view>
        </view>
        <text v-if="!registrations.length" class="empty">No appointments.</text>
      </view>

      <view class="panel">
        <text class="section-title">Medical records</text>
        <view v-for="item in pagedMedicalRecords" :key="String(item.medicalRecordId)" class="record">
          <text class="strong">Record #{{ item.medicalRecordId }}</text>
          <text>{{ item.diagnosis || "No diagnosis" }}</text>
          <text class="muted">{{ item.treatmentAdvice || "No treatment advice" }}</text>
        </view>
        <text v-if="!medicalRecords.length" class="empty">No medical records.</text>
      </view>

      <view class="panel">
        <text class="section-title">Prescriptions</text>
        <view v-for="item in pagedPrescriptions" :key="String(item.prescriptionId)" class="record">
          <text class="strong">Prescription #{{ item.prescriptionId }}</text>
          <text>Risk: {{ item.riskLevel || "Unknown" }}</text>
          <text class="muted">{{ item.status || "No status" }}</text>
        </view>
        <text v-if="!prescriptions.length" class="empty">No prescriptions.</text>
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
  const envConfig = typeof process !== "undefined" && process.env ? process.env.UNI_APP_API_BASE || process.env.VUE_APP_API_BASE : "";
  return normalizeApiBase(stored || globalConfig || envConfig || DEFAULT_API_BASE);
}

function request(apiBase, path, method = "GET", data = null, token = "") {
  return new Promise((resolve, reject) => {
    uni.request({
      url: normalizeApiBase(apiBase) + path,
      method,
      data,
      header: { "Content-Type": "application/json", ...(token ? { Authorization: "Bearer " + token } : {}) },
      success(response) {
        const body = response.data || {};
        if (response.statusCode >= 200 && response.statusCode < 300 && body.code === 0) resolve(body.data);
        else reject(new Error(body.message || "Request failed"));
      },
      fail(error) { reject(error); }
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
      profile: { name: "", gender: "", age: 0, allergyHistory: "", pastHistory: "" },
      chiefComplaint: "",
      triageResult: null,
      slots: [],
      selectedSlotIndex: -1,
      registrations: [],
      medicalRecords: [],
      prescriptions: [],
      registrationsPage: 1,
      medicalRecordsPage: 1,
      prescriptionsPage: 1,
      pageSize: 5,
      message: "",
      busy: false
    };
  },
  computed: {
    visibleSlots() {
      const departmentName = String(this.triageResult && this.triageResult.recommendedDepartment || "");
      const matched = this.slots.filter((slot) => !departmentName || String(slot.departmentName || "").includes(departmentName));
      return matched.length ? matched : this.slots;
    },
    slotLabels() { return this.visibleSlots.map((slot) => this.slotLabel(slot)); },
    selectedSlot() { return this.selectedSlotIndex < 0 ? null : this.visibleSlots[this.selectedSlotIndex] || null; },
    pagedRegistrations() { return this.pageItems(this.registrations, this.registrationsPage); },
    pagedMedicalRecords() { return this.pageItems(this.medicalRecords, this.medicalRecordsPage); },
    pagedPrescriptions() { return this.pageItems(this.prescriptions, this.prescriptionsPage); }
  },
  async onLoad() {
    if (this.session) {
      try { await this.refreshRecords(); } catch (error) { this.message = error && error.message ? error.message : "Load failed"; }
    }
  },
  methods: {
    token() { return this.session && this.session.token || ""; },
    saveApiBase() { this.apiBase = normalizeApiBase(this.apiBase); uni.setStorageSync(API_BASE_KEY, this.apiBase); },
    async run(label, task) {
      this.busy = true;
      this.message = "";
      this.saveApiBase();
      try { await task(); this.message = label + " success"; }
      catch (error) { this.message = error && error.message ? error.message : label + " failed"; }
      finally { this.busy = false; }
    },
    api(path, method = "GET", data = null) { return request(this.apiBase, path, method, data, this.token()); },
    pageCount(items) { return Math.max(1, Math.ceil((items || []).length / this.pageSize)); },
    pageItems(items, page) { return (items || []).slice((Math.max(1, page) - 1) * this.pageSize, Math.max(1, page) * this.pageSize); },
    changePage(key, items, delta) { this[key] = Math.min(Math.max(1, this[key] + delta), this.pageCount(items)); },
    applyProfile(patient) {
      this.profile.name = String(patient.name || this.session && this.session.name || "");
      this.profile.gender = String(patient.gender || "");
      this.profile.age = Number(patient.age || 0);
      this.profile.allergyHistory = String(patient.allergyHistory || "");
      this.profile.pastHistory = String(patient.pastHistory || "");
    },
    async login() {
      await this.run("Login", async () => {
        this.session = await request(this.apiBase, "/patient/login", "POST", { account: this.account, password: this.password });
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
      this.message = "Logged out";
    },
    async saveProfile() {
      await this.run("Profile", async () => {
        const saved = await this.api("/patient/profile/save", "POST", {
          name: this.profile.name.trim(),
          gender: this.profile.gender,
          age: Number(this.profile.age) || 0,
          allergyHistory: this.profile.allergyHistory,
          pastHistory: this.profile.pastHistory
        });
        this.applyProfile(saved || {});
      });
    },
    async triage() {
      await this.run("Triage", async () => {
        this.triageResult = await this.api("/triage/consult", "POST", { chiefComplaint: this.chiefComplaint.trim() });
        await this.refreshSlots();
      });
    },
    async reloadSlots() { await this.run("Slots", () => this.refreshSlots()); },
    async refreshSlots() {
      const nextSlots = await this.api("/registration/slots");
      this.slots = Array.isArray(nextSlots) ? nextSlots : [];
      this.selectedSlotIndex = -1;
    },
    onSlotChange(event) { this.selectedSlotIndex = Number(event.detail.value); },
    slotLabel(slot) { return [slot.departmentName || "Department", slot.doctorName || "Doctor", slot.startTime || "Time", "Left " + (slot.remainingCapacity == null ? 0 : slot.remainingCapacity)].join(" / "); },
    async createRegistration() {
      if (!this.selectedSlot) { this.message = "Select a slot first"; return; }
      await this.run("Appointment", async () => {
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
      await this.run("Cancel", async () => {
        await this.api("/registration/cancel", "POST", { registrationId: Number(registrationId) });
        await this.refreshRecords();
      });
    },
    async reloadRecords() { await this.run("Records", () => this.refreshRecords()); },
    async refreshRecords() {
      await this.refreshSlots();
      const [patient, registrations, medicalRecords, prescriptions] = await Promise.all([
        this.api("/patient/info"),
        this.api("/registration/list"),
        this.api("/medical-record/list"),
        this.api("/prescription/list")
      ]);
      this.applyProfile(patient || {});
      this.registrations = Array.isArray(registrations) ? registrations : [];
      this.medicalRecords = Array.isArray(medicalRecords) ? medicalRecords : [];
      this.prescriptions = Array.isArray(prescriptions) ? prescriptions : [];
      this.registrationsPage = 1;
      this.medicalRecordsPage = 1;
      this.prescriptionsPage = 1;
    }
  }
};
</script>

<style>
page { background: #f4fbf7; }
.page { min-height: 100vh; padding: 28rpx; color: #172033; }
.hero { display: flex; flex-direction: column; gap: 12rpx; padding: 28rpx 0; }
.eyebrow { color: #167255; font-size: 24rpx; font-weight: 700; }
.title { color: #172033; font-size: 48rpx; font-weight: 800; }
.subtitle, .muted, .empty { color: #5d6b66; font-size: 26rpx; line-height: 1.6; }
.notice { margin-bottom: 20rpx; padding: 18rpx 22rpx; border: 1rpx solid #c5e4d8; border-radius: 10rpx; background: #eaf8f2; color: #116044; font-size: 26rpx; }
.panel { display: flex; flex-direction: column; gap: 18rpx; margin-bottom: 24rpx; padding: 24rpx; border: 1rpx solid #d8e3df; border-radius: 12rpx; background: #ffffff; }
.user-card, .panel-head, .pagination, .pagination-actions { display: flex; flex-direction: row; align-items: center; justify-content: space-between; }
.section-title { font-size: 32rpx; font-weight: 700; }
.strong { color: #172033; font-size: 28rpx; font-weight: 700; }
input, textarea, .picker { min-height: 76rpx; box-sizing: border-box; border: 1rpx solid #cbd8d4; border-radius: 10rpx; padding: 16rpx; background: #fff; color: #172033; font-size: 28rpx; }
textarea { width: 100%; min-height: 160rpx; }
button { min-height: 76rpx; border-radius: 10rpx; background: #167255; color: #ffffff; font-size: 28rpx; }
button[disabled] { opacity: 0.55; }
.ghost { border: 1rpx solid #bfd8d0; background: #ffffff; color: #167255; }
.small { min-height: 60rpx; padding: 0 20rpx; font-size: 24rpx; }
.danger { margin-top: 8rpx; background: #b33a3a; }
.result, .record { display: flex; flex-direction: column; gap: 8rpx; padding: 18rpx; border-radius: 10rpx; background: #f6faf8; color: #31453e; font-size: 26rpx; line-height: 1.6; }
</style>
