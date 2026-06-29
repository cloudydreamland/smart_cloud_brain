<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">科室资料</text>
      <text class="title">科室与重点专科</text>
      <text class="subtitle">结合后端科室数据和常见就诊准备，帮助患者选择合适入口。</text>
    </view>
    <view class="error" v-if="error">{{ error }}</view>
    <view class="segmented">
      <button class="segment" :class="{ active: !query }" @click="query = ''">全部</button>
      <button v-for="item in quickNames" :key="item" class="segment" :class="{ active: query === item }" @click="query = item">{{ item }}</button>
    </view>
    <view class="panel">
      <input v-model.trim="query" placeholder="搜索科室、症状或准备事项" />
      <view v-for="item in filtered" :key="item.name" class="record">
        <text class="strong">{{ item.name }}</text>
        <text class="muted">{{ item.description }}</text>
        <text class="tag">常见问题</text>
        <text class="muted">{{ item.symptoms.join("、") }}</text>
        <text class="tag">建议准备</text>
        <text class="muted">{{ item.preparation.join("、") }}</text>
        <button class="ghost" @click="goSlots(item)">查看相关号源</button>
      </view>
      <text class="empty" v-if="!filtered.length">没有匹配科室。</text>
    </view>
  </view>
</template>

<script>
import { api } from "../../common/api.js";
import { defaultDepartments, normalizeDepartment } from "../../common/content.js";

export default {
  data() { return { rows: defaultDepartments.map(normalizeDepartment), query: "", error: "" }; },
  computed: {
    quickNames() { return this.rows.slice(0, 4).map((item) => item.name); },
    filtered() {
      const q = this.query.trim().toLowerCase();
      if (!q) return this.rows;
      return this.rows.filter((item) => `${item.name} ${item.description} ${item.symptoms.join(" ")} ${item.preparation.join(" ")}`.toLowerCase().includes(q));
    }
  },
  onLoad(options) { this.query = options && options.q ? decodeURIComponent(options.q) : ""; },
  onShow() { this.refresh(); },
  methods: {
    goSlots(item) { uni.navigateTo({ url: "/pages/slots/slots?department=" + encodeURIComponent(item.name) }); },
    async refresh() {
      try {
        const [recommendations, departments] = await Promise.all([api.recommendations("DEPARTMENT").catch(() => []), api.departments().catch(() => [])]);
        const rows = recommendations.length ? recommendations : departments;
        this.rows = (rows.length ? rows : defaultDepartments).map(normalizeDepartment);
      } catch (error) {
        this.error = error.message || "科室接口不可用，当前显示默认资料";
      }
    }
  }
};
</script>
