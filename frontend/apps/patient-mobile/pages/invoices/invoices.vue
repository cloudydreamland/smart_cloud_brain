<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">电子发票</text>
      <text class="title">费用与票据线索</text>
      <text class="subtitle">当前按预约和处方派生，正式金额和票据以后端财务接口为准。</text>
    </view>
    <view class="error" v-if="error">{{ error }}</view>
    <view class="panel">
      <text class="section-title">票据线索</text>
      <view v-for="item in rows" :key="item.id" class="record">
        <text class="strong">{{ item.title }}</text>
        <text class="muted">{{ item.description }}</text>
        <text class="tag">{{ item.category }} · {{ item.status }}</text>
      </view>
      <text class="empty" v-if="!rows.length">暂无票据记录。完成挂号或处方后，相关费用线索会显示在这里。</text>
    </view>
  </view>
</template>

<script>
import { api } from "../../common/api.js";
import { requireLogin } from "../../common/session.js";
import { dateText, list, num, text } from "../../common/formatters.js";

export default {
  data() { return { registrations: [], prescriptions: [], error: "" }; },
  computed: {
    rows() {
      const registrations = this.registrations.map((item, index) => ({
        id: "registration-" + text(item.registrationId, String(index + 1)),
        category: "门诊",
        title: `${text(item.departmentName, "门诊")} · ${text(item.doctorName, "医生待定")}`,
        status: String(item.status).toUpperCase() === "COMPLETED" ? "可申请" : "待开具",
        description: `${dateText(item.appointmentTime || item.createdAt)}，门诊挂号费用以医院财务系统最终记录为准。`
      }));
      const prescriptions = this.prescriptions.map((item, index) => ({
        id: "prescription-" + text(item.prescriptionId, String(index + 1)),
        category: "处方",
        title: `处方 #${text(item.prescriptionId, String(index + 1))}`,
        status: num(item.prescriptionId) ? "已归档" : "待开具",
        description: text(item.riskLevel, "处方记录已归档，药品费用待同步。")
      }));
      return [...registrations, ...prescriptions];
    }
  },
  onShow() { if (requireLogin()) this.refresh(); },
  methods: {
    async refresh() {
      this.error = "";
      try {
        const [registrations, prescriptions] = await Promise.all([api.registrations().catch(() => []), api.prescriptions().catch(() => [])]);
        this.registrations = list(registrations);
        this.prescriptions = list(prescriptions);
      } catch (error) {
        this.error = error.message || "票据加载失败";
      }
    }
  }
};
</script>
