<script setup lang="ts">
import { computed, ref } from "vue";
import { formatDateTime, type PatientSiteConfigRecord } from "@smart-cloud-brain/shared-api";

const props = defineProps<{
  records: PatientSiteConfigRecord[];
  saving: boolean;
  saveDraft: () => void;
  publishDraft: () => void;
  rollbackTo: (record: PatientSiteConfigRecord) => void;
}>();

type DiffRow = { path: string; before: string; after: string };

const selectedKey = ref("");
const visibleRecords = computed(() => props.records.slice(0, 6));
const selectedRecord = computed(() => props.records.find((record) => recordKey(record) === selectedKey.value) || visibleRecords.value[0] || null);
const previousRecord = computed(() => {
  const record = selectedRecord.value;
  if (!record) return null;
  const index = props.records.findIndex((item) => recordKey(item) === recordKey(record));
  return index >= 0 ? props.records[index + 1] || null : null;
});
const diffRows = computed(() => buildDiff(previousRecord.value?.configJson, selectedRecord.value?.configJson));

function recordKey(record: PatientSiteConfigRecord) {
  return `${record.status || "-"}-${record.version || "-"}-${record.id || "-"}`;
}

function selectRecord(record: PatientSiteConfigRecord) {
  selectedKey.value = recordKey(record);
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
    </div>

    <div class="patient-site-version-list">
      <article
        v-for="record in visibleRecords"
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
</template>
