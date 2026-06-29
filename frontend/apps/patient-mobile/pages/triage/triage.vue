<template>
  <view class="page with-tab">
    <view class="hero">
      <text class="eyebrow">AI 智能分诊</text>
      <text class="title">先整理症状，再选择科室</text>
      <text class="subtitle">分诊建议用于就医准备，不替代医生诊断。</text>
    </view>
    <view class="error" v-if="error">{{ error }}</view>
    <view class="notice" v-if="message">{{ message }}</view>

    <view class="panel form">
      <textarea v-model.trim="chiefComplaint" placeholder="请描述症状、持续时间、严重程度和伴随表现" />
      <button :disabled="busy || !chiefComplaint" @click="submit">{{ busy ? "分析中" : "提交分诊" }}</button>
    </view>

    <view class="panel" v-if="result">
      <text class="section-title">本次建议</text>
      <view class="record">
        <text class="strong">{{ result.recommendedDepartment || "科室待推荐" }}</text>
        <text class="muted">{{ result.reason || "系统已生成分诊结果，请结合线下医生判断。" }}</text>
        <text class="tag">{{ statusText(result.status) }}</text>
      </view>
      <button @click="goSlots">查看相关号源</button>
    </view>

    <view class="panel">
      <view class="panel-head">
        <text class="section-title">历史分诊</text>
        <button class="ghost small" :disabled="busy" @click="loadHistory">刷新</button>
      </view>
      <view v-for="item in history" :key="String(item.triageRecordId)" class="record">
        <text class="strong">{{ item.recommendedDepartment || "科室待推荐" }}</text>
        <text class="muted">{{ item.reason || item.chiefComplaint || "暂无说明" }}</text>
        <text class="tag">{{ statusText(item.status) }}</text>
      </view>
      <text class="empty" v-if="!history.length">暂无历史分诊。</text>
    </view>
  </view>
</template>

<script>
import { api } from "../../common/api.js";
import { requireLogin } from "../../common/session.js";
import { list, statusText } from "../../common/formatters.js";

export default {
  data() {
    return { chiefComplaint: "", result: null, history: [], busy: false, error: "", message: "" };
  },
  onShow() {
    if (requireLogin()) this.loadHistory();
  },
  methods: {
    statusText,
    goSlots() { uni.navigateTo({ url: "/pages/slots/slots" }); },
    async loadHistory() {
      this.error = "";
      try { this.history = list(await api.triageList()); }
      catch (error) { this.error = error.message || "历史分诊加载失败"; }
    },
    async submit() {
      this.busy = true;
      this.error = "";
      this.message = "";
      try {
        this.result = await api.triage(this.chiefComplaint);
        this.message = "分诊结果已生成";
        await this.loadHistory();
      } catch (error) {
        this.error = error.message || "分诊失败";
      } finally {
        this.busy = false;
      }
    }
  }
};
</script>
