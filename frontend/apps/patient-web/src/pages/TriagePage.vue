<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { aiSourceLabel, aiSourceTone, api, formatApiError, statusClass, statusText, usePagination, usePatientWorkflowStore, type TriageRecord } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, FormField, LoadingState, PaginationBar, StatusTag, Toast } from "@smart-cloud-brain/shared-ui";
import TriageResultModal from "../components/TriageResultModal.vue";

const router = useRouter();
const workflow = usePatientWorkflowStore();
const { triageHistory, triage } = storeToRefs(workflow);
const form = reactive({ symptoms: "", duration: "", severity: "MEDIUM", extra: "" });
const loading = ref(false);
const error = ref("");
const toast = ref<InstanceType<typeof Toast>>();
const resultOpen = ref(false);
const canSubmit = computed(() => form.symptoms.trim().length > 0);
const severityLabels: Record<string, string> = { LOW: "轻度", MEDIUM: "中度", HIGH: "重度或明显加重" };
const { currentPage, pageSize, total, pageRows } = usePagination(triageHistory, 5);

function complaint() {
  return [
    `症状：${form.symptoms.trim()}`,
    form.duration.trim() ? `持续时间：${form.duration.trim()}` : "",
    `严重程度：${severityLabels[form.severity] ?? form.severity}`,
    form.extra.trim() ? `补充说明：${form.extra.trim()}` : "",
  ].filter(Boolean).join("；");
}

async function submit() {
  if (!canSubmit.value) {
    error.value = "请先填写主要症状。";
    return;
  }
  loading.value = true;
  error.value = "";
  try {
    triage.value = await api.triage({ chiefComplaint: complaint() });
    await workflow.refreshAuthenticated();
    toast.value?.success("分诊已提交", "请根据推荐科室继续选择号源。");
    resultOpen.value = true;
  } catch (err) {
    error.value = formatApiError(err, "分诊提交失败");
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <section class="portal-grid">
    <form class="panel" @submit.prevent="submit">
      <header class="panel-header"><div class="panel-title"><p class="eyebrow">智能分诊</p><h2>填写本次主要症状</h2><p>请描述症状、持续时间和变化情况。</p></div></header>
      <div class="panel-body stack">
        <ErrorState v-if="error" :message="error" />
        <FormField label="主要症状"><textarea v-model.trim="form.symptoms" rows="5" /></FormField>
        <div class="form-grid">
          <FormField label="持续时间"><input v-model.trim="form.duration" placeholder="例如：2 天" /></FormField>
          <FormField label="严重程度">
            <select v-model="form.severity"><option value="LOW">轻度</option><option value="MEDIUM">中度</option><option value="HIGH">重度或明显加重</option></select>
          </FormField>
        </div>
        <FormField label="补充说明"><textarea v-model.trim="form.extra" rows="3" /></FormField>
        <button class="primary" type="submit" :disabled="loading">{{ loading ? "分析中" : "提交分诊" }}</button>
      </div>
    </form>
    <aside class="stack">
      <section class="panel">
        <header class="panel-header"><div class="panel-title"><h2>最新结果</h2><p>提交后会显示推荐科室和原因。</p></div></header>
        <div class="panel-body">
          <LoadingState v-if="loading" title="正在分析症状" />
          <div v-else-if="triage" class="clinical-note">
            <StatusTag :status="statusText(triage.status)" :tone="statusClass(triage.status)" />
            <span v-if="triage.provider || ''" class="tag" :class="aiSourceTone(triage.provider)">
              {{ aiSourceLabel(triage.provider) }} · {{ triage.provider || "" }}{{ triage.model || "" ? ` / ${triage?.model || ""}` : "" }}
            </span>
            <h3>{{ triage.recommendedDepartment || "待确认" }}</h3>
            <p>{{ triage.reason || "暂无说明" }}</p>
          </div>
          <EmptyState v-else title="暂无分诊结果" />
        </div>
      </section>
      <section class="panel">
        <header class="panel-header"><div class="panel-title"><h2>历史分诊</h2><p>用于回看最近症状和推荐记录。</p></div></header>
        <div class="list">
          <article v-for="item in pageRows" :key="String(item.triageRecordId)" class="list-row">
            <div class="row-main"><strong>{{ item.recommendedDepartment || "待确认" }}</strong><p>{{ item.chiefComplaint || "" }}</p></div>
            <StatusTag :status="statusText(item.status)" :tone="statusClass(item.status)" />
          </article>
          <PaginationBar v-model="currentPage" :total="total" :page-size="pageSize" />
          <EmptyState v-if="!triageHistory.length" title="暂无历史分诊" />
        </div>
      </section>
    </aside>
    <Toast ref="toast" />
    <TriageResultModal :open="resultOpen" :result="triage" @close="resultOpen = false" @doctors="router.push({ name: 'patient-doctors' })" />
  </section>
</template>
