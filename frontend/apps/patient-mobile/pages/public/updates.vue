<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">资讯中心</text>
      <text class="title">公告、健康资讯与科研动态</text>
      <text class="subtitle">内容用于就医准备和健康教育，不替代医生诊断。</text>
    </view>
    <view class="panel form">
      <input v-model.trim="query" placeholder="搜索公告、资讯或关键词" />
      <view class="segmented">
        <button v-for="item in kinds" :key="item" class="segment" :class="{ active: kind === item }" @click="kind = item">{{ item }}</button>
      </view>
    </view>
    <view class="panel">
      <view v-for="item in filtered" :key="item.title" class="record" @click="selected = item">
        <text class="tag">{{ item.kind }} · {{ item.date }}</text>
        <text class="strong">{{ item.title }}</text>
        <text class="muted">{{ item.summary }}</text>
      </view>
      <text class="empty" v-if="!filtered.length">暂无匹配资讯。</text>
    </view>
    <view class="panel" v-if="selected">
      <view class="panel-head">
        <text class="section-title">{{ selected.title }}</text>
        <button class="ghost small" @click="selected = null">关闭</button>
      </view>
      <text class="muted">{{ selected.kind }} · {{ selected.source }} · {{ selected.date }}</text>
      <text class="strong">{{ selected.summary }}</text>
      <text v-for="paragraph in selected.body" :key="paragraph" class="muted">{{ paragraph }}</text>
      <view class="segmented">
        <text v-for="tag in selected.tags" :key="tag" class="tag">{{ tag }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { updates } from "../../common/content.js";

export default {
  data() { return { updates, query: "", kind: "全部", kinds: ["全部", "通知公告", "健康资讯", "科研动态"], selected: null }; },
  computed: {
    filtered() {
      const q = this.query.trim().toLowerCase();
      return this.updates.filter((item) => {
        const byKind = this.kind === "全部" || item.kind === this.kind;
        const haystack = `${item.title} ${item.summary} ${item.body.join(" ")} ${item.tags.join(" ")}`.toLowerCase();
        return byKind && (!q || haystack.includes(q));
      });
    }
  },
  onLoad(options) {
    if (options && options.kind) this.kind = decodeURIComponent(options.kind);
    if (options && options.q) this.query = decodeURIComponent(options.q);
  }
};
</script>
