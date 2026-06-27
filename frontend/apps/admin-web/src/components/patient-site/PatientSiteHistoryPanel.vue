<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { formatDateTime, type PatientSiteConfigRecord } from "@smart-cloud-brain/shared-api";

const props = defineProps<{
  records: PatientSiteConfigRecord[];
  saving: boolean;
  saveDraft: () => void;
  publishDraft: () => void;
  rollbackTo: (record: PatientSiteConfigRecord) => void;
}>();

type DiffRow = { path: string; before: string; after: string };

const pageSize = 5;
const modalOpen = ref(false);
const selectedKey = ref("");
const currentPage = ref(1);
const totalPages = computed(() => Math.max(1, Math.ceil(props.records.length / pageSize)));
const pagedRecords = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return props.records.slice(start, start + pageSize);
});
const selectedRecord = computed(() => props.records.find((record) => recordKey(record) === selectedKey.value) || pagedRecords.value[0] || null);
const previousRecord = computed(() => {
  const record = selectedRecord.value;
  if (!record) return null;
  const index = props.records.findIndex((item) => recordKey(item) === recordKey(record));
  return index >= 0 ? props.records[index + 1] || null : null;
});
const diffRows = computed(() => buildDiff(previousRecord.value?.configJson, selectedRecord.value?.configJson));

watch(() => props.records.length, () => {
  currentPage.value = 1;
  selectedKey.value = "";
});

function recordKey(record: PatientSiteConfigRecord) {
  return `${record.status || "-"}-${record.version || "-"}-${record.id || "-"}`;
}

function openHistoryModal() {
  modalOpen.value = true;
  if (!selectedKey.value && pagedRecords.value[0]) selectRecord(pagedRecords.value[0]);
}

function selectRecord(record: PatientSiteConfigRecord) {
  selectedKey.value = recordKey(record);
}

function goPage(page: number) {
  currentPage.value = Math.min(Math.max(page, 1), totalPages.value);
  if (pagedRecords.value[0]) selectRecord(pagedRecords.value[0]);
}

function buildDiff(beforeJson?: string, afterJson?: string): DiffRow[] {
  const before = flattenJson(parseJson(beforeJson));
  const after = flattenJson(parseJson(afterJson));
  return Array.from(new Set([...Object.keys(before), ...Object.keys(after)]))
    .sort()
    .filter((path) => before[path] !== after[path])
    .slice(0, 80)
    .map((path) => ({ path, before: displayValue(before[path]), after: displayValue(after[path]) }));
}

function parseJson(json?: string) {
  if (!json) return null;
  try {
    return JSON.parse(json) as unknown;
  } catch {
    return null;
  }
}

function flattenJson(value: unknown, path = "$", output: Record<string, string> = {}) {
  if (Array.isArray(value)) {
    if (!value.length) output[path] = "[]";
    value.forEach((item, index) => flattenJson(item, `${path}[${index}]`, output));
    return output;
  }
  if (value && typeof value === "object") {
    const entries = Object.entries(value);
    if (!entries.length) output[path] = "{}";
    entries.forEach(([key, child]) => flattenJson(child, `${path}.${key}`, output));
    return output;
  }
  output[path] = value === undefined ? "" : JSON.stringify(value);
  return output;
}

function displayValue(value?: string) {
  return value === undefined || value === "" ? "未设置" : value;
}
</script>

<template>
  <section class="patient-site-history">
    <div>
      <strong>发布工作流</strong>
      <p>草稿不会影响患者端；发布和回滚都会生成新的已发布版本。</p>
    </div>
    <div class="patient-site-history-actions">
      <button type="button" class="topbar-refresh" :disabled="saving" @click="saveDraft">保存草稿</button>
      <button type="button" class="topbar-refresh" :disabled="saving" @click="publishDraft">发布最新草稿</button>
      <button type="button" class="topbar-refresh" :disabled="!records.length" @click="openHistoryModal">查看草稿版本</button>
    </div>
    <p class="patient-site-history-summary">当前共 {{ records.length }} 个版本，点击按钮在弹窗中分页查看。</p>

    <div v-if="modalOpen" class="patient-config-modal-backdrop" @click.self="modalOpen = false">
      <section class="patient-config-modal-card patient-site-history-modal-card" role="dialog" aria-modal="true" aria-label="草稿版本记录">
        <button type="button" class="patient-config-modal-close" aria-label="关闭" @click="modalOpen = false">×</button>
        <header class="patient-config-modal-head">
          <h2>草稿版本记录</h2>
          <p>按版本倒序查看配置历史、字段差异，并可回滚到非发布版本。</p>
        </header>

        <div class="patient-site-history-modal-body">
          <div class="patient-site-version-toolbar">
            <strong>版本列表</strong>
            <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
          </div>

          <div class="patient-site-version-list">
            <article
              v-for="record in pagedRecords"
              :key="recordKey(record)"
              class="patient-site-version"
              :class="{ active: selectedRecord && recordKey(record) === recordKey(selectedRecord) }"
            >
              <div>
                <span class="status-pill" :class="record.status === 'PUBLISHED' ? 'enabled' : 'disabled'">{{ record.status }}</span>
                <strong>v{{ record.version || "-" }}</strong>
              </div>
              <p>{{ record.remark || "无备注" }}</p>
              <small>创建 {{ record.createdBy || "-" }} / 更新 {{ record.updatedBy || "-" }}</small>
              <small>{{ formatDateTime(record.updatedAt) || "-" }}</small>
              <div class="patient-site-version-actions">
                <button type="button" class="plain-link" @click="selectRecord(record)">查看 diff</button>
                <button v-if="record.status !== 'PUBLISHED'" type="button" class="danger-link" :disabled="saving" @click="rollbackTo(record)">回滚到此版本</button>
              </div>
            </article>
          </div>

          <div class="patient-site-pagination">
            <button type="button" class="topbar-refresh" :disabled="currentPage <= 1" @click="goPage(currentPage - 1)">上一页</button>
            <button type="button" class="topbar-refresh" :disabled="currentPage >= totalPages" @click="goPage(currentPage + 1)">下一页</button>
          </div>
        </div>

        <div v-if="selectedRecord" class="patient-site-diff">
          <div>
            <strong>v{{ selectedRecord.version || "-" }} 变更内容</strong>
            <p>对比 {{ previousRecord ? `v${previousRecord.version || "-"}` : "空版本" }}</p>
          </div>
          <div v-if="diffRows.length" class="patient-site-diff-table">
            <div class="patient-site-diff-head">
              <span>字段</span>
              <span>之前</span>
              <span>之后</span>
            </div>
            <div v-for="row in diffRows" :key="row.path" class="patient-site-diff-row">
              <code>{{ row.path }}</code>
              <span>{{ row.before }}</span>
              <span>{{ row.after }}</span>
            </div>
          </div>
          <p v-else>和上一版本无字段差异。</p>
        </div>
      </section>
    </div>
  </section>
</template>
