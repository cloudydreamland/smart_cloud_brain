<script setup lang="ts">
import type { StatsSection } from "@smart-cloud-brain/shared-api";
import { usePatientSiteConfirm } from "../../../composables/patientSiteConfirm";

defineProps<{
  section: StatsSection;
}>();
const confirm = usePatientSiteConfirm();

async function removeStat(section: StatsSection, index: number) {
  const item = section.items[index];
  if (!item || !(await confirm({
    title: "确认删除指标",
    message: `将从当前编辑稿中移除指标「${item.label || item.value || "未命名指标"}」。保存草稿不会影响患者端，保存并生效或发布后，对应页面区块才会不再展示该指标。`,
    confirmText: "确认删除",
    tone: "danger",
  }))) return;
  section.items.splice(index, 1);
}
</script>

<template>
  <div class="nested-list">
    <div class="nested-list-head">
      <strong>指标列表</strong>
      <button type="button" class="topbar-refresh" @click="section.items.push({ label: '指标', value: '0' })">新增指标</button>
    </div>
    <div v-for="(item, itemIndex) in section.items" :key="`stat-${itemIndex}`" class="config-row-card">
      <div class="config-grid three">
        <label><span>数值</span><input v-model.trim="item.value" type="text"></label>
        <label><span>名称</span><input v-model.trim="item.label" type="text"></label>
        <label><span>说明</span><input v-model.trim="item.caption" type="text"></label>
      </div>
      <button type="button" class="danger-link" @click="removeStat(section, itemIndex)">删除指标</button>
    </div>
  </div>
</template>
