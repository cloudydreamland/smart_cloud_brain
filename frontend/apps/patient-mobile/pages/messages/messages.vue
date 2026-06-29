<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">消息中心</text>
      <text class="title">通知与提醒</text>
      <text class="subtitle">集中查看预约提醒、诊后通知和系统服务公告。</text>
    </view>
    <view class="error" v-if="error">{{ error }}</view>
    <view class="segmented">
      <button v-for="item in filters" :key="item.value" class="segment" :class="{ active: filter === item.value }" @click="selectFilter(item.value)">{{ item.label }}</button>
    </view>
    <view class="panel">
      <view class="panel-head">
        <text class="section-title">消息列表</text>
        <button class="ghost small" :disabled="busy" @click="refresh">刷新</button>
      </view>
      <view v-for="item in messages" :key="String(item.id) + item.title" class="record" @click="markRead(item)">
        <text class="strong">{{ item.title }}</text>
        <text class="muted">{{ item.content }}</text>
        <text class="tag">{{ item.type }} · {{ item.read ? "已读" : "未读" }}</text>
      </view>
      <text class="empty" v-if="!messages.length">暂无消息。</text>
    </view>
  </view>
</template>

<script>
import { api } from "../../common/api.js";
import { requireLogin } from "../../common/session.js";
import { list, num, text } from "../../common/formatters.js";

const fallbackRows = [
  { id: 0, type: "系统通知", title: "患者服务已整合分诊、挂号和诊后资料", content: "你可以从患者服务台查看当前就诊旅程。", read: false },
  { id: -1, type: "预约提醒", title: "预约后请核对就诊人和到院时间", content: "无法按时到院时，请提前取消预约。", read: false },
  { id: -2, type: "诊后通知", title: "病历和处方生成后将归档", content: "复诊前建议回看诊断、医嘱和处方风险提示。", read: true }
];

export default {
  data() {
    return { rows: [], filter: "", filters: [{ label: "全部", value: "" }, { label: "未读", value: "UNREAD" }, { label: "已读", value: "READ" }], busy: false, error: "" };
  },
  computed: {
    messages() {
      const rows = list(this.rows).map((row, index) => ({
        id: num(row.notificationId || row.id, index + 1),
        type: this.normalizeType(text(row.type || row.category, "")),
        title: text(row.title, "系统通知"),
        content: text(row.content || row.message, "暂无通知正文"),
        read: String(row.readStatus || row.status).toUpperCase() === "READ" || row.read === true
      }));
      const allRows = rows.length ? rows : fallbackRows;
      if (this.filter === "READ") return allRows.filter((item) => item.read);
      if (this.filter === "UNREAD") return allRows.filter((item) => !item.read);
      return allRows;
    }
  },
  onShow() { if (requireLogin()) this.refresh(); },
  methods: {
    normalizeType(value) {
      const next = String(value).toUpperCase();
      if (value.includes("预约") || next.includes("REGISTRATION")) return "预约提醒";
      if (value.includes("病历") || value.includes("处方") || next.includes("MEDICAL")) return "诊后通知";
      return "系统通知";
    },
    selectFilter(value) { this.filter = value; this.refresh(); },
    async refresh() {
      this.busy = true;
      this.error = "";
      try { this.rows = list(await api.notifications(this.filter)); }
      catch (error) { this.error = error.message || "消息加载失败，当前显示本地服务提醒"; this.rows = []; }
      finally { this.busy = false; }
    },
    async markRead(item) {
      if (item.read || item.id <= 0) return;
      try { await api.markNotificationRead(item.id); await this.refresh(); }
      catch (error) { this.error = error.message || "标记已读失败"; }
    }
  }
};
</script>
