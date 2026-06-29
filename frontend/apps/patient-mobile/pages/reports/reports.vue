<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">检查报告</text>
      <text class="title">报告与复诊资料</text>
      <text class="subtitle">当前按病历派生报告线索，正式报告以后端接口为准。</text>
    </view>
    <view class="error" v-if="error">{{ error }}</view>
    <view class="panel">
      <text class="section-title">报告线索</text>
      <view v-for="item in reports" :key="item.id" class="record">
        <text class="strong">{{ item.title }}</text>
        <text class="muted">{{ item.summary }}</text>
        <text class="tag">{{ item.type }} · {{ item.date }}</text>
      </view>
      <text class="empty" v-if="!reports.length">暂无可展示报告。报告接口未接入时，可先通过病历和处方准备复诊资料。</text>
    </view>
  </view>
</template>

<script>
import { api } from "../../common/api.js";
import { requireLogin } from "../../common/session.js";
import { dateText, list, text } from "../../common/formatters.js";

export default {
  data() { return { records: [], error: "" }; },
  computed: {
    reports() {
      return this.records.map((record, index) => {
        const diagnosis = text(record.diagnosis, "复诊资料");
        const title = diagnosis.includes("肺") || diagnosis.includes("骨") ? "影像资料复核" : diagnosis.includes("肿瘤") ? "病理与随访资料" : "门诊检查资料";
        const type = title.includes("影像") ? "影像" : title.includes("病理") ? "病理" : "检验";
        return {
          id: text(record.medicalRecordId, String(index + 1)),
          type,
          title,
          date: dateText(record.createdAt),
          summary: `关联病历：${diagnosis}。请结合医生病历、处方和线下报告原件解读。`
        };
      });
    }
  },
  onShow() { if (requireLogin()) this.refresh(); },
  methods: {
    async refresh() {
      this.error = "";
      try { this.records = list(await api.records()); }
      catch (error) { this.error = error.message || "报告加载失败"; }
    }
  }
};
</script>
