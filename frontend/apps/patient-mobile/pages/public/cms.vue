<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">{{ previewToken ? "PREVIEW" : pageLabel }}</text>
      <text class="title">{{ pageTitle }}</text>
      <text class="subtitle">{{ pageIntro }}</text>
    </view>
    <view class="error" v-if="error">{{ error }}</view>
    <view class="panel" v-if="loading">
      <text class="muted">正在读取患者端站点配置。</text>
    </view>
    <view class="panel" v-else-if="!page">
      <text class="section-title">页面未发布</text>
      <text class="muted">该专题页不存在、未启用或尚未发布。</text>
      <button @click="goHome">返回医院首页</button>
    </view>
    <view class="panel" v-else>
      <view v-for="section in sections" :key="section.id || section.title || section.type" class="record">
        <text class="tag">{{ section.type || "内容" }}</text>
        <text class="strong">{{ section.title || "未命名模块" }}</text>
        <text class="muted">{{ sectionText(section) }}</text>
        <view class="segmented" v-if="sectionItems(section).length">
          <text v-for="item in sectionItems(section)" :key="item" class="tag">{{ item }}</text>
        </view>
      </view>
      <text class="empty" v-if="!sections.length">该页面暂未配置正文内容。</text>
    </view>
  </view>
</template>

<script>
import { api } from "../../common/api.js";

export default {
  data() { return { slug: "", previewToken: "", config: null, page: null, loading: true, error: "" }; },
  computed: {
    pageLabel() { return this.page && this.page.label || "专题页"; },
    pageTitle() { return this.page && this.page.title || (this.loading ? "页面加载中" : "页面未发布"); },
    pageIntro() { return this.page && this.page.intro || (this.loading ? "正在读取患者端站点配置。" : "该专题页不存在、未启用或尚未发布。"); },
    sections() { return this.page && Array.isArray(this.page.sections) ? this.page.sections.filter((item) => this.previewToken || item.enabled !== false) : []; }
  },
  onLoad(options) {
    this.slug = String(options && options.slug || "").toLowerCase();
    this.previewToken = String(options && options.previewToken || "");
    this.load();
  },
  methods: {
    goHome() { uni.navigateTo({ url: "/pages/public/home" }); },
    sectionText(section) {
      const content = section && section.content || {};
      return String(section.intro || content.text || content.description || content.summary || content.body || "该模块包含结构化内容，移动端已做简化展示。");
    },
    sectionItems(section) {
      const content = section && section.content || {};
      const source = Array.isArray(content.items) ? content.items : Array.isArray(content.links) ? content.links : Array.isArray(content.cards) ? content.cards : [];
      return source.map((item) => String(item.title || item.label || item.text || item.name || "")).filter(Boolean).slice(0, 8);
    },
    async load() {
      this.loading = true;
      this.error = "";
      try {
        const config = this.previewToken ? await api.sitePreviewConfig(this.previewToken) : await api.siteConfig();
        this.config = config;
        const pages = config && config.pages && Array.isArray(config.pages.pages) ? config.pages.pages : [];
        this.page = pages.find((item) => item.slug === this.slug && (this.previewToken || item.enabled !== false)) || null;
      } catch (error) {
        this.error = error.message || "专题页加载失败";
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>
