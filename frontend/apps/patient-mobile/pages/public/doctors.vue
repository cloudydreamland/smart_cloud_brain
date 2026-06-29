<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">医生团队</text>
      <text class="title">按科室和专长查找医生</text>
      <text class="subtitle">医生介绍用于就诊方向参考，具体号源以在线挂号为准。</text>
    </view>
    <view class="error" v-if="error">{{ error }}</view>
    <view class="panel form">
      <input v-model.trim="query" placeholder="搜索医生、科室、疾病方向" />
      <picker mode="selector" :range="departmentOptions" :value="departmentIndex" @change="onDepartmentChange">
        <view class="picker">{{ departmentOptions[departmentIndex] }}</view>
      </picker>
    </view>
    <view class="panel">
      <view v-for="doctor in filtered" :key="doctor.id" class="record">
        <text class="strong">{{ doctor.name }}</text>
        <text class="muted">{{ doctor.title }} · {{ doctor.department }}</text>
        <text class="muted">{{ doctor.specialty }}</text>
        <view class="segmented">
          <text v-for="tag in doctor.tags" :key="tag" class="tag">{{ tag }}</text>
        </view>
        <view class="actions">
          <button class="ghost" @click="selected = doctor">详情</button>
          <button @click="goSlots(doctor)">看号源</button>
        </view>
      </view>
      <text class="empty" v-if="!filtered.length">没有匹配医生。</text>
    </view>
    <view class="panel" v-if="selected">
      <view class="panel-head">
        <text class="section-title">{{ selected.name }}</text>
        <button class="ghost small" @click="selected = null">关闭</button>
      </view>
      <text class="strong">{{ selected.title }} · {{ selected.department }}</text>
      <text class="muted">{{ selected.profile }}</text>
      <button @click="goSlots(selected)">查看可预约号源</button>
    </view>
  </view>
</template>

<script>
import { api } from "../../common/api.js";
import { defaultDoctors, normalizeDoctor } from "../../common/content.js";

export default {
  data() { return { rows: defaultDoctors, query: "", department: "全部科室", departmentIndex: 0, selected: null, error: "" }; },
  computed: {
    departmentOptions() { return ["全部科室", ...new Set(this.rows.map((item) => item.department).filter(Boolean))]; },
    filtered() {
      const q = this.query.trim().toLowerCase();
      return this.rows.filter((item) => {
        const byDepartment = this.department === "全部科室" || item.department === this.department;
        const haystack = `${item.name} ${item.title} ${item.department} ${item.specialty} ${item.profile} ${item.tags.join(" ")}`.toLowerCase();
        return byDepartment && (!q || haystack.includes(q));
      });
    }
  },
  onLoad(options) { this.query = options && options.q ? decodeURIComponent(options.q) : ""; },
  onShow() { this.refresh(); },
  methods: {
    onDepartmentChange(event) { this.departmentIndex = Number(event.detail.value); this.department = this.departmentOptions[this.departmentIndex]; },
    goSlots(doctor) { uni.navigateTo({ url: "/pages/slots/slots?doctor=" + encodeURIComponent(doctor.id) + "&department=" + encodeURIComponent(doctor.department) }); },
    async refresh() {
      try {
        const [recommendations, doctors] = await Promise.all([api.recommendations("DOCTOR").catch(() => []), api.doctors().catch(() => [])]);
        const rows = recommendations.length ? recommendations : doctors;
        this.rows = (rows.length ? rows : defaultDoctors).map(normalizeDoctor);
      } catch (error) {
        this.error = error.message || "医生接口不可用，当前显示默认专家资料";
      }
    }
  }
};
</script>
