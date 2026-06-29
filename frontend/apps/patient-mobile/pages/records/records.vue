<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">我的病历</text>
      <text class="title">诊断、主诉和医嘱归档</text>
      <text class="subtitle">医生保存后会同步到患者服务，便于复诊前回看。</text>
    </view>
    <view class="error" v-if="error">{{ error }}</view>
    <view class="panel">
      <view class="panel-head">
        <text class="section-title">病历记录</text>
        <button class="ghost small" :disabled="busy" @click="refresh">刷新</button>
      </view>
      <view v-for="item in rows" :key="String(item.medicalRecordId)" class="record" @click="open(item)">
        <text class="strong">{{ item.diagnosis || "诊断待同步" }}</text>
        <text class="muted">{{ item.chiefComplaint || item.treatmentAdvice || "暂无病历摘要" }}</text>
        <text class="tag">{{ dateText(item.createdAt) }}</text>
      </view>
      <text class="empty" v-if="!rows.length">暂无病历记录。</text>
    </view>
    <view class="panel" v-if="selected">
      <view class="panel-head">
        <text class="section-title">病历详情</text>
        <button class="ghost small" @click="selected = null">关闭</button>
      </view>
      <text class="strong">{{ selected.diagnosis || "诊断待同步" }}</text>
      <text class="muted">主诉：{{ selected.chiefComplaint || "-" }}</text>
      <text class="muted">医嘱：{{ selected.treatmentAdvice || "-" }}</text>
      <text class="muted">时间：{{ dateText(selected.createdAt) }}</text>
    </view>
  </view>
</template>

<script>
import { api } from "../../common/api.js";
import { requireLogin } from "../../common/session.js";
import { dateText, list, num } from "../../common/formatters.js";

export default {
  data() { return { rows: [], selected: null, busy: false, error: "" }; },
  onShow() { if (requireLogin()) this.refresh(); },
  methods: {
    dateText,
    async refresh() {
      this.busy = true;
      this.error = "";
      try { this.rows = list(await api.records()); }
      catch (error) { this.error = error.message || "病历加载失败"; }
      finally { this.busy = false; }
    },
    async open(item) {
      this.selected = item;
      const id = num(item.medicalRecordId || item.id);
      if (!id) return;
      try { this.selected = await api.recordDetail(id); }
      catch (_) { this.selected = item; }
    }
  }
};
</script>
