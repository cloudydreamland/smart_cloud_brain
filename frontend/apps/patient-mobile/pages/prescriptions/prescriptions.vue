<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">我的处方</text>
      <text class="title">用药与风险审核提示</text>
      <text class="subtitle">处方内容应按医生指导执行，不自行调整用药。</text>
    </view>
    <view class="error" v-if="error">{{ error }}</view>
    <view class="panel">
      <view class="panel-head">
        <text class="section-title">处方记录</text>
        <button class="ghost small" :disabled="busy" @click="refresh">刷新</button>
      </view>
      <view v-for="item in rows" :key="String(item.prescriptionId)" class="record" @click="open(item)">
        <text class="strong">处方 #{{ item.prescriptionId || "-" }}</text>
        <text class="muted">风险：{{ riskText(item.riskLevel) }}</text>
        <text class="tag">{{ statusText(item.status) }}</text>
      </view>
      <text class="empty" v-if="!rows.length">暂无处方记录。</text>
    </view>
    <view class="panel" v-if="selected">
      <view class="panel-head">
        <text class="section-title">处方详情</text>
        <button class="ghost small" @click="selected = null">关闭</button>
      </view>
      <text class="strong">处方 #{{ selected.prescriptionId || "-" }}</text>
      <text class="muted">风险等级：{{ riskText(selected.riskLevel) }}</text>
      <text class="muted">状态：{{ statusText(selected.status) }}</text>
      <text class="muted">审核说明：{{ selected.riskMessage || selected.auditMessage || "暂无说明" }}</text>
    </view>
  </view>
</template>

<script>
import { api } from "../../common/api.js";
import { requireLogin } from "../../common/session.js";
import { list, num, riskText, statusText } from "../../common/formatters.js";

export default {
  data() { return { rows: [], selected: null, busy: false, error: "" }; },
  onShow() { if (requireLogin()) this.refresh(); },
  methods: {
    riskText,
    statusText,
    async refresh() {
      this.busy = true;
      this.error = "";
      try { this.rows = list(await api.prescriptions()); }
      catch (error) { this.error = error.message || "处方加载失败"; }
      finally { this.busy = false; }
    },
    async open(item) {
      this.selected = item;
      const id = num(item.prescriptionId || item.id);
      if (!id) return;
      try { this.selected = await api.prescriptionDetail(id); }
      catch (_) { this.selected = item; }
    }
  }
};
</script>
