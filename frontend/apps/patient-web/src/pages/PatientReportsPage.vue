<script setup lang="ts">
import { computed, nextTick, ref, watch } from "vue";
import { storeToRefs } from "pinia";
import { fieldText, formatDateTime, usePatientWorkflowStore, type DataRow } from "@smart-cloud-brain/shared-api";
import { EmptyState, SegmentedControl } from "@smart-cloud-brain/shared-ui";

type ReportRow = {
  id: string;
  type: "检验" | "影像" | "病理" | "其他";
  title: string;
  date: string;
  status: "已出具" | "待复诊解读";
  summary: string;
  source?: DataRow;
};

const workflow = usePatientWorkflowStore();
const { records } = storeToRefs(workflow);
const typeFilter = ref("");
const query = ref("");
const selected = ref<ReportRow | null>(null);
const drawerEl = ref<HTMLDivElement | null>(null);
watch(selected, (value: ReportRow | null) => {
  if (value) nextTick(() => drawerEl.value?.focus());
});
const typeOptions = [
  { label: "全部", value: "" },
  { label: "检验", value: "检验" },
  { label: "影像", value: "影像" },
  { label: "病理", value: "病理" },
  { label: "其他", value: "其他" },
];

const reports = computed<ReportRow[]>(() => records.value.map((record, index) => {
  const diagnosis = fieldText(record, "diagnosis", "复诊资料");
  const title = diagnosis.includes("肺") || diagnosis.includes("骨") ? "影像资料复核" : diagnosis.includes("肿瘤") ? "病理与随访资料" : "门诊检查资料";
  const type = title.includes("影像") ? "影像" : title.includes("病理") ? "病理" : "检验";
  return {
    id: fieldText(record, "medicalRecordId", String(index + 1)),
    type,
    title,
    date: formatDateTime(fieldText(record, "createdAt"), "时间待同步"),
    status: "待复诊解读",
    summary: `关联病历：${diagnosis}。请结合医生病历、处方和线下报告原件解读。`,
    source: record,
  };
}));
const filteredReports = computed(() => {
  const text = query.value.trim().toLowerCase();
  return reports.value.filter((item) => {
    const matchType = !typeFilter.value || item.type === typeFilter.value;
    const haystack = `${item.title} ${item.summary} ${item.type} ${item.status}`.toLowerCase();
    return matchType && (!text || haystack.includes(text));
  });
});
const guideCards = [
  { title: "报告查看", text: "检查检验结果生成后会按项目、时间和状态归档。报告结论应结合医生诊断解读。" },
  { title: "复诊准备", text: "复诊时请同时准备病历、处方、检查报告和症状变化记录，不要只提供单项异常指标。" },
  { title: "异常处理", text: "如报告异常并伴随明显症状，应按医生建议尽快复诊或线下急诊。" },
];
</script>

<template>
  <section class="panel">
    <header class="panel-header">
      <div class="panel-title">
        <p class="eyebrow">检查报告</p>
        <h2>报告与复诊资料</h2>
        <p>按检查类型和关键词整理报告线索，帮助复诊前快速回看。</p>
      </div>
    </header>
    <div class="resource-alert subtle" style="margin-bottom: 16px;"><strong>数据说明</strong><p>报告功能正在接入，当前展示的是病历关联数据，非独立检查检验报告。</p></div>
    <div class="panel-body stack">
      <div class="toolbar">
        <SegmentedControl v-model="typeFilter" :options="typeOptions" />
        <input v-model.trim="query" class="portal-search-input" type="search" placeholder="搜索报告、诊断或说明">
      </div>

      <div v-if="filteredReports.length" class="report-grid">
        <article v-for="item in filteredReports" :key="item.id" class="report-card">
          <span>{{ item.type }} · {{ item.date }}</span>
          <h3>{{ item.title }}</h3>
          <p>{{ item.summary }}</p>
          <div class="toolbar">
            <em>{{ item.status }}</em>
            <button type="button" @click="selected = item">查看</button>
          </div>
        </article>
      </div>
      <EmptyState v-else title="暂无可展示报告" message="报告接口尚未接入时，可先通过病历和处方完成复诊资料准备。" />

      <div class="portal-guide-grid">
        <article v-for="item in guideCards" :key="item.title">
          <h3>{{ item.title }}</h3>
          <p>{{ item.text }}</p>
        </article>
      </div>

      <div v-if="selected" ref="drawerEl" class="portal-detail-drawer" role="dialog" aria-modal="true" tabindex="-1" @keydown.escape="selected = null" @click.self="selected = null">
        <button type="button" aria-label="关闭" @click="selected = null">×</button>
        <span>{{ selected.type }} · {{ selected.status }}</span>
        <h2>{{ selected.title }}</h2>
        <p>{{ selected.summary }}</p>
        <ul>
          <li>报告内容需结合病史、体征、医生诊断和既往结果趋势综合判断。</li>
          <li>复诊前请准备原始报告或可读取的电子文件。</li>
          <li>如出现胸痛、呼吸困难、意识改变等红旗症状，请直接线下急诊。</li>
        </ul>
      </div>
    </div>
  </section>
</template>
