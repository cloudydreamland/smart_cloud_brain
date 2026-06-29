<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">资料检索</text>
      <text class="title">搜索科室、医生、症状和服务</text>
      <text class="subtitle">结果聚合后端科室医生数据和本地就医资料。</text>
    </view>
    <view class="panel form">
      <input v-model.trim="keyword" placeholder="输入关键词" />
      <view class="segmented">
        <button v-for="item in typeOptions" :key="item" class="segment" :class="{ active: typeFilter === item }" @click="typeFilter = item">{{ item }}</button>
      </view>
    </view>
    <view class="panel">
      <view class="panel-head">
        <text class="section-title">搜索结果</text>
        <text class="muted">{{ filtered.length }} 条</text>
      </view>
      <view v-for="item in filtered" :key="item.type + item.title" class="record">
        <text class="tag">{{ item.type }}</text>
        <text class="strong">{{ item.title }}</text>
        <text class="muted">{{ item.description }}</text>
        <view class="segmented">
          <text v-for="tag in item.meta" :key="tag" class="tag">{{ tag }}</text>
        </view>
        <button class="ghost" @click="openItem(item)">打开</button>
      </view>
      <text class="empty" v-if="!filtered.length">没有匹配结果。</text>
    </view>
  </view>
</template>

<script>
import { api } from "../../common/api.js";
import { defaultDepartments, defaultDoctors, normalizeDepartment, normalizeDoctor, staticSearchItems } from "../../common/content.js";

export default {
  data() { return { keyword: "", typeFilter: "全部", typeOptions: ["全部", "科室", "医生", "疾病/症状", "服务资料"], departments: [], doctors: [] }; },
  computed: {
    items() {
      const departmentItems = this.departments.map((row) => ({ type: "科室", title: row.name, description: row.description, meta: ["科室资料", "就诊准备"] }));
      const doctorItems = this.doctors.map((row) => ({ type: "医生", title: row.name, description: `${row.department} · ${row.title}。${row.specialty}`, meta: [row.department, "需登录预约"] }));
      return [...departmentItems, ...doctorItems, ...staticSearchItems];
    },
    filtered() {
      const q = this.keyword.trim().toLowerCase();
      return this.items.filter((item) => {
        const byType = this.typeFilter === "全部" || item.type === this.typeFilter;
        const haystack = `${item.title} ${item.description} ${item.type} ${item.meta.join(" ")}`.toLowerCase();
        return byType && (!q || haystack.includes(q));
      });
    }
  },
  onLoad(options) { this.keyword = options && options.q ? decodeURIComponent(options.q) : ""; },
  onShow() { this.refresh(); },
  methods: {
    openItem(item) {
      const q = encodeURIComponent(item.title);
      if (item.type === "科室") uni.navigateTo({ url: "/pages/public/departments?q=" + q });
      else if (item.type === "医生") uni.navigateTo({ url: "/pages/public/doctors?q=" + q });
      else if (item.title.includes("分诊")) uni.switchTab({ url: "/pages/triage/triage" });
      else uni.navigateTo({ url: "/pages/public/updates?q=" + q });
    },
    async refresh() {
      const [departments, doctors] = await Promise.all([api.departments().catch(() => []), api.doctors().catch(() => [])]);
      this.departments = (departments.length ? departments : defaultDepartments).map(normalizeDepartment);
      this.doctors = (doctors.length ? doctors : defaultDoctors).map(normalizeDoctor);
    }
  }
};
</script>
