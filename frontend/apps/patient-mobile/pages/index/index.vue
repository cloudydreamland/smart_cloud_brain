<template>
  <view class="page with-tab">
    <view class="hero">
      <text class="eyebrow">患者服务</text>
      <text class="title">您好，{{ patientName }}</text>
      <text class="subtitle">从分诊、挂号到诊后资料，继续完成本次就医旅程。</text>
    </view>

    <view class="error" v-if="error">{{ error }}</view>
    <view class="notice" v-if="message">{{ message }}</view>

    <view class="panel">
      <view class="metric-grid">
        <view class="metric"><text class="muted">可约号源</text><strong>{{ slots.length }}</strong></view>
        <view class="metric"><text class="muted">预约记录</text><strong>{{ registrations.length }}</strong></view>
        <view class="metric"><text class="muted">病历</text><strong>{{ records.length }}</strong></view>
        <view class="metric"><text class="muted">处方</text><strong>{{ prescriptions.length }}</strong></view>
      </view>
    </view>

    <view class="panel">
      <view class="panel-head">
        <text class="section-title">快捷入口</text>
        <button class="ghost small" :disabled="busy" @click="refresh">刷新</button>
      </view>
      <view class="quick-grid">
        <button class="quick" @click="go('/pages/triage/triage')">AI 智能分诊</button>
        <button class="quick" @click="go('/pages/slots/slots')">医生号源</button>
        <button class="quick" @click="go('/pages/records/records')">我的病历</button>
        <button class="quick" @click="go('/pages/prescriptions/prescriptions')">我的处方</button>
        <button class="quick" @click="go('/pages/reports/reports')">检查报告</button>
        <button class="quick" @click="go('/pages/messages/messages')">消息中心</button>
      </view>
    </view>

    <view class="panel">
      <text class="section-title">最近分诊</text>
      <view class="record" v-if="latestTriage">
        <text class="strong">{{ latestTriage.recommendedDepartment || "科室待推荐" }}</text>
        <text class="muted">{{ latestTriage.reason || latestTriage.chiefComplaint || "暂无分诊说明" }}</text>
        <text class="tag">{{ statusText(latestTriage.status) }}</text>
      </view>
      <text class="empty" v-else>还没有分诊记录，可先描述症状获得科室建议。</text>
    </view>

    <view class="panel">
      <text class="section-title">最近预约</text>
      <view class="record" v-if="latestRegistration">
        <text class="strong">{{ latestRegistration.departmentName || "门诊" }} · {{ latestRegistration.doctorName || "医生待定" }}</text>
        <text class="muted">{{ dateText(latestRegistration.appointmentTime || latestRegistration.createdAt) }}</text>
        <text class="tag">{{ statusText(latestRegistration.status) }}</text>
      </view>
      <text class="empty" v-else>暂无预约记录，完成分诊后可选择医生号源。</text>
    </view>
  </view>
</template>

<script>
import { api } from "../../common/api.js";
import { getSession, requireLogin } from "../../common/session.js";
import { dateText, latestById, list, statusText, text } from "../../common/formatters.js";

export default {
  data() {
    return { patient: null, triages: [], slots: [], registrations: [], records: [], prescriptions: [], busy: false, error: "", message: "" };
  },
  computed: {
    patientName() {
      const session = getSession();
      return text(this.patient && this.patient.name, session && session.name || "患者");
    },
    latestTriage() { return latestById(this.triages, "triageRecordId"); },
    latestRegistration() { return latestById(this.registrations, "registrationId"); }
  },
  onShow() {
    if (requireLogin()) this.refresh();
  },
  methods: {
    dateText,
    statusText,
    go(url) { uni.navigateTo({ url }); },
    async refresh() {
      this.busy = true;
      this.error = "";
      try {
        const [patient, triages, slots, registrations, records, prescriptions] = await Promise.all([
          api.patientInfo().catch(() => null),
          api.triageList().catch(() => []),
          api.slots().catch(() => []),
          api.registrations().catch(() => []),
          api.records().catch(() => []),
          api.prescriptions().catch(() => [])
        ]);
        this.patient = patient;
        this.triages = list(triages);
        this.slots = list(slots);
        this.registrations = list(registrations);
        this.records = list(records);
        this.prescriptions = list(prescriptions);
      } catch (error) {
        this.error = error.message || "服务台加载失败";
      } finally {
        this.busy = false;
      }
    }
  }
};
</script>
