<script setup lang="ts">
import { inject } from "vue";
import type { AdminAnalyticsState } from "../composables/useAdminAnalytics";
import {
  IconDownload,
  IconLoader2,
  IconRefresh,
} from "@tabler/icons-vue";
import { ErrorState } from "@smart-cloud-brain/shared-ui";

const {
  loading,
  exporting,
  loaded,
  error,
  startDate,
  endDate,
  metricCards,
  refresh,
  exportCsv,
} = inject<AdminAnalyticsState>("adminAnalytics")!;
</script>

<template>
  <section
    class="analytics-section"
    aria-labelledby="analytics-title"
    :aria-busy="loading"
  >
    <header class="analytics-header">
      <div class="analytics-heading">
        <h2 id="analytics-title">运营数据概览</h2>
      </div>
      <div class="analytics-toolbar">
        <label class="analytics-date-field">
          <span>开始日期</span>
          <input v-model="startDate" type="date" />
        </label>
        <span class="analytics-date-separator" aria-hidden="true">至</span>
        <label class="analytics-date-field">
          <span>结束日期</span>
          <input v-model="endDate" type="date" />
        </label>
        <button
          class="analytics-button"
          type="button"
          :disabled="loading"
          @click="refresh"
        >
          <IconLoader2
            v-if="loading"
            class="analytics-spin"
            :size="17"
          />
          <IconRefresh v-else :size="17" />
          {{ loading ? "更新中" : "刷新" }}
        </button>
        <button
          class="analytics-button primary"
          type="button"
          :disabled="exporting"
          @click="exportCsv"
        >
          <IconLoader2
            v-if="exporting"
            class="analytics-spin"
            :size="17"
          />
          <IconDownload v-else :size="17" />
          {{ exporting ? "导出中" : "导出 CSV" }}
        </button>
      </div>
    </header>

    <ErrorState
      v-if="error"
      class="analytics-error"
      title="统计数据暂不可用"
      :message="error"
    >
      <button type="button" @click="refresh">重新加载</button>
    </ErrorState>

    <div
      v-if="loading && !loaded"
      class="analytics-loading"
      role="status"
    >
      <IconLoader2 class="analytics-spin" :size="18" />
      正在同步运营数据…
    </div>

    <div class="analytics-metric-grid">
      <article
        v-for="metric in metricCards"
        :key="metric.label"
        class="analytics-metric-card"
      >
        <div class="analytics-metric-head">
          <span
            class="analytics-metric-icon"
            :class="`is-${metric.tone}`"
          >
            <component
              :is="metric.icon"
              :size="22"
              :stroke-width="1.9"
            />
          </span>
          <span>{{ metric.label }}</span>
        </div>
        <div class="analytics-metric-tail">
          <strong>{{ metric.value }}</strong>
          <span v-if="metric.sub" class="analytics-metric-sub">{{
            metric.sub
          }}</span>
        </div>
      </article>
    </div>
  </section>
</template>
