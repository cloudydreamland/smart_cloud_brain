<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">智慧云脑</text>
      <text class="title">{{ heroTitle }}</text>
      <text class="subtitle">{{ heroIntro }}</text>
    </view>
    <view class="error" v-if="error">{{ error }}</view>
    <view class="notice" v-if="noticeText">{{ noticeText }}</view>

    <view class="panel">
      <text class="section-title">患者服务入口</text>
      <view class="quick-grid">
        <button class="quick" @click="goTab('/pages/index/index')">患者服务台</button>
        <button class="quick" @click="goTab('/pages/triage/triage')">AI 智能分诊</button>
        <button class="quick" @click="go('/pages/slots/slots')">在线挂号</button>
        <button class="quick" @click="go('/pages/public/search')">搜索资料</button>
      </view>
    </view>

    <view class="panel">
      <text class="section-title">公开服务</text>
      <view class="quick-grid">
        <button class="quick" @click="go('/pages/public/departments')">科室与重点专科</button>
        <button class="quick" @click="go('/pages/public/doctors')">医生团队</button>
        <button class="quick" @click="go('/pages/public/updates?kind=通知公告')">通知公告</button>
        <button class="quick" @click="go('/pages/public/updates?kind=健康资讯')">健康资讯</button>
      </view>
    </view>

    <view class="panel">
      <view class="panel-head">
        <text class="section-title">热门科室</text>
        <button class="ghost small" @click="go('/pages/public/departments')">全部</button>
      </view>
      <view v-for="item in departments" :key="item.name" class="record" @click="go('/pages/public/search?q=' + encode(item.name))">
        <text class="strong">{{ item.name }}</text>
        <text class="muted">{{ item.description }}</text>
      </view>
    </view>

    <view class="panel">
      <view class="panel-head">
        <text class="section-title">推荐医生</text>
        <button class="ghost small" @click="go('/pages/public/doctors')">全部</button>
      </view>
      <view v-for="item in doctors" :key="item.id" class="record">
        <text class="strong">{{ item.name }} · {{ item.title }}</text>
        <text class="muted">{{ item.department }} · {{ item.specialty }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { api } from "../../common/api.js";
import { goPage } from "../../common/session.js";
import { defaultDepartments, defaultDoctors, normalizeDepartment, normalizeDoctor } from "../../common/content.js";

export default {
  data() {
    return { heroTitle: "从分诊到复诊，保持同一条就医线索", heroIntro: "整合 AI 分诊、医生号源、预约记录、病历处方和患者资料。", noticeText: "", departments: defaultDepartments.slice(0, 4), doctors: defaultDoctors.slice(0, 3), error: "" };
  },
  onShow() { this.refresh(); },
  methods: {
    encode(value) { return encodeURIComponent(value); },
    go(url) { uni.navigateTo({ url }); },
    goTab(url) { goPage(url); },
    async refresh() {
      try {
        const [home, departmentRows, doctorRows] = await Promise.all([
          api.homeConfig().catch(() => null),
          api.departments().catch(() => []),
          api.doctors().catch(() => [])
        ]);
        if (home && home.hero) {
          this.heroTitle = home.hero.title || this.heroTitle;
          this.heroIntro = home.hero.subtitle || home.hero.intro || this.heroIntro;
        }
        const notices = home && Array.isArray(home.notices) ? home.notices : [];
        this.noticeText = notices[0] ? `${notices[0].title || "通知"}：${notices[0].content || notices[0].summary || ""}` : "";
        this.departments = (departmentRows.length ? departmentRows : defaultDepartments).slice(0, 4).map(normalizeDepartment);
        this.doctors = (doctorRows.length ? doctorRows : defaultDoctors).slice(0, 3).map(normalizeDoctor);
      } catch (error) {
        this.error = error.message || "首页数据加载失败，当前显示默认内容";
      }
    }
  }
};
</script>
