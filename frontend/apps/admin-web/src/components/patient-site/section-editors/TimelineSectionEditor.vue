<script setup lang="ts">
import type { TimelineSection } from "@smart-cloud-brain/shared-api";
import { usePatientSiteConfirm } from "../../../composables/patientSiteConfirm";

defineProps<{ section: TimelineSection }>();
const confirm = usePatientSiteConfirm();

function addTimeline(section: TimelineSection) {
  section.items.push({ title: "新步骤", text: "" });
}

async function removeTimeline(section: TimelineSection, index: number) {
  const item = section.items[index];
  if (!item || !(await confirm({
    title: "确认删除步骤",
    message: `将从当前编辑稿中移除步骤「${item.title || "未命名步骤"}」。保存草稿不会影响患者端，保存并生效或发布后，对应页面区块才会不再展示该步骤。`,
    confirmText: "确认删除",
    tone: "danger",
  }))) return;
  section.items.splice(index, 1);
}
</script>

<template>
  <div class="nested-list">
    <div class="nested-list-head">
      <strong>步骤列表</strong>
      <button type="button" class="topbar-refresh" @click="addTimeline(section)">新增步骤</button>
    </div>
    <div v-for="(item, itemIndex) in section.items" :key="`timeline-${itemIndex}`" class="config-row-card">
      <div class="config-grid three">
        <label><span>时间</span><input v-model.trim="item.time" type="text"></label>
        <label><span>标题</span><input v-model.trim="item.title" type="text"></label>
        <label><span>说明</span><textarea v-model.trim="item.text" rows="3"></textarea></label>
      </div>
      <button type="button" class="danger-link" @click="removeTimeline(section, itemIndex)">删除步骤</button>
    </div>
  </div>
</template>
