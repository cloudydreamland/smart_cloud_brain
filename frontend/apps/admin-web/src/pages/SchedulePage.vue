<script setup lang="ts">
import { reactive, ref } from "vue";
import { storeToRefs } from "pinia";
import { api, fieldText, formatApiError, toNumber, useAdminWorkflowStore, useAuthStore, type DataRow } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, FormField, LoadingState } from "@smart-cloud-brain/shared-ui";
import ScheduleSuggestionDetailModal from "../components/ScheduleSuggestionDetailModal.vue";
import PublishScheduleConfirmModal from "../components/PublishScheduleConfirmModal.vue";

const emit = defineEmits<{ refresh: [] }>();
const auth = useAuthStore();
const workflow = useAdminWorkflowStore();
const { suggestions, schedules } = storeToRefs(workflow);
const form = reactive({ startDate: new Date(Date.now() + 86400000).toISOString().slice(0, 10), days: 3 });
const loading = ref(false);
const error = ref("");
const notice = ref("");
const selected = ref<DataRow | null>(null);
const publishOpen = ref(false);

async function generate() {
  loading.value = true;
  error.value = "";
  notice.value = "";
  try {
    suggestions.value = await api.generateSchedule(auth.token(), { ...form });
    notice.value = `已生成 ${suggestions.value.length} 条排班建议。`;
  } catch (err) {
    error.value = formatApiError(err, "排班建议生成失败");
  } finally {
    loading.value = false;
  }
}

async function publish() {
  loading.value = true;
  error.value = "";
  notice.value = "";
  try {
    const ids = suggestions.value.map((item) => toNumber(item.id)).filter(Boolean);
    schedules.value = await api.publishSchedule(auth.token(), { suggestionIds: ids });
    suggestions.value = [];
    publishOpen.value = false;
    emit("refresh");
    notice.value = "号源已发布。";
  } catch (err) {
    error.value = formatApiError(err, "号源发布失败");
  } finally {
    loading.value = false;
  }
}

async function openDetail(item: DataRow) {
  loading.value = true;
  error.value = "";
  try {
    selected.value = await api.scheduleSuggestionDetail(auth.token(), toNumber(item.id));
  } catch (err) {
    error.value = formatApiError(err, "排班建议详情加载失败");
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <section class="schedule-layout">
    <section class="panel">
      <header class="panel-header"><div class="panel-title"><p class="eyebrow">SCHEDULE</p><h2>AI 排班与号源发布</h2><p>发布前管理员必须确认。</p></div></header>
      <div class="panel-body stack">
        <ErrorState v-if="error" :message="error" />
        <div v-if="notice" class="notice success">{{ notice }}</div>
        <div class="form-grid">
          <FormField label="开始日期"><input v-model="form.startDate" type="date" /></FormField>
          <FormField label="生成天数"><input v-model.number="form.days" type="number" min="1" max="14" /></FormField>
        </div>
        <div class="toolbar"><button type="button" :disabled="loading" @click="generate">生成建议</button><button class="primary" type="button" :disabled="loading || !suggestions.length" @click="publishOpen = true">发布号源</button></div>
        <LoadingState v-if="loading" />
        <div v-if="suggestions.length" class="list">
          <article v-for="item in suggestions" :key="String(item.id)" class="list-row">
            <div class="row-main"><strong>{{ fieldText(item, "workDate") }} {{ fieldText(item, "timeRange") }}</strong><p>{{ fieldText(item, "doctorName") }} · 容量 {{ fieldText(item, "capacity") }}</p></div>
            <button type="button" @click="openDetail(item)">详情</button>
          </article>
        </div>
        <EmptyState v-else title="暂无待发布建议" />
      </div>
    </section>
    <aside class="panel">
      <header class="panel-header"><div class="panel-title"><h2>已发布排班</h2><p>后端排班列表。</p></div></header>
      <div class="list">
        <article v-for="item in schedules" :key="String(item.id)" class="list-row"><div class="row-main"><strong>{{ fieldText(item, "workDate") }} {{ fieldText(item, "timeRange") }}</strong><p>{{ fieldText(item, "doctorName") }} · 容量 {{ fieldText(item, "capacity") }}</p></div></article>
        <EmptyState v-if="!schedules.length" title="暂无已发布排班" />
      </div>
    </aside>
    <ScheduleSuggestionDetailModal :open="Boolean(selected)" :suggestion="selected" @close="selected = null" />
    <PublishScheduleConfirmModal :open="publishOpen" :busy="loading" @close="publishOpen = false" @confirm="publish" />
  </section>
</template>
