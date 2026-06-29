<template>
  <view class="page with-tab">
    <view class="hero">
      <text class="eyebrow">我的预约</text>
      <text class="title">查看和管理挂号记录</text>
      <text class="subtitle">无法按时到院时，请提前取消预约释放号源。</text>
    </view>
    <view class="error" v-if="error">{{ error }}</view>
    <view class="panel">
      <view class="panel-head">
        <text class="section-title">预约记录</text>
        <button class="ghost small" :disabled="busy" @click="refresh">刷新</button>
      </view>
      <view v-for="item in rows" :key="String(item.registrationId)" class="record">
        <text class="strong">{{ item.departmentName || "门诊" }} · {{ item.doctorName || "医生待定" }}</text>
        <text class="muted">{{ dateText(item.appointmentTime || item.createdAt) }}</text>
        <text class="tag">{{ statusText(item.status) }}</text>
        <button v-if="canCancelRegistration(item)" class="danger" :disabled="busy" @click="cancel(item.registrationId)">取消预约</button>
      </view>
      <text class="empty" v-if="!rows.length">暂无预约记录。</text>
    </view>
    <button @click="goSlots">预约医生号源</button>
  </view>
</template>

<script>
import { api } from "../../common/api.js";
import { requireLogin } from "../../common/session.js";
import { canCancelRegistration, dateText, list, num, statusText } from "../../common/formatters.js";

export default {
  data() {
    return { rows: [], busy: false, error: "" };
  },
  onShow() {
    if (requireLogin()) this.refresh();
  },
  methods: {
    canCancelRegistration,
    dateText,
    statusText,
    goSlots() { uni.navigateTo({ url: "/pages/slots/slots" }); },
    async refresh() {
      this.busy = true;
      this.error = "";
      try { this.rows = list(await api.registrations()); }
      catch (error) { this.error = error.message || "预约加载失败"; }
      finally { this.busy = false; }
    },
    cancel(registrationId) {
      uni.showModal({
        title: "取消预约",
        content: "确认取消这条预约吗？",
        success: async (res) => {
          if (!res.confirm) return;
          this.busy = true;
          try {
            await api.cancelRegistration(num(registrationId));
            await this.refresh();
          } catch (error) {
            this.error = error.message || "取消失败";
          } finally {
            this.busy = false;
          }
        }
      });
    }
  }
};
</script>
