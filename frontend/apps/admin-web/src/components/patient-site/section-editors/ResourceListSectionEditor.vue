<script setup lang="ts">
import type { DepartmentListSection, DoctorListSection, RouteTargetConfig } from "@smart-cloud-brain/shared-api";
import { usePatientSiteConfirm } from "../../../composables/patientSiteConfirm";
import RouteTargetEditor from "../RouteTargetEditor.vue";

defineProps<{
  section: DoctorListSection | DepartmentListSection;
  patientRouteOptions: readonly { name: string; label: string }[];
}>();

const emptyLink = (): RouteTargetConfig => ({ label: "", routeName: "patient-home", enabled: true, sort: 0 });
const confirm = usePatientSiteConfirm();

async function removeLink(section: DoctorListSection | DepartmentListSection, index: number) {
  const link = section.links[index];
  if (!link || !(await confirm({
    title: "确认删除固定入口",
    message: `将从当前编辑稿中移除入口「${link.label || "未命名入口"}」。保存草稿不会影响患者端，保存并生效或发布后，对应列表区块才会不再展示该入口。`,
    confirmText: "确认删除",
    tone: "danger",
  }))) return;
  section.links.splice(index, 1);
}

async function removeFallbackName(section: DoctorListSection | DepartmentListSection, index: number) {
  const name = section.fallbackNames[index];
  if (!name || !(await confirm({
    title: "确认删除默认名称",
    message: `将从当前编辑稿中移除默认名称「${name}」。保存草稿不会影响患者端，保存并生效或发布后，对应列表区块才会更新。`,
    confirmText: "确认删除",
    tone: "danger",
  }))) return;
  section.fallbackNames.splice(index, 1);
}
</script>

<template>
  <div class="nested-list">
    <div class="config-grid two">
      <label><span>数量上限</span><input v-model.number="section.limit" type="number"></label>
    </div>
    <div class="nested-list-head">
      <strong>固定入口</strong>
      <button type="button" class="topbar-refresh" @click="section.links.push(emptyLink())">新增入口</button>
    </div>
    <div v-for="(link, linkIndex) in section.links" :key="`resource-link-${linkIndex}`" class="config-row-card">
      <div class="config-grid three">
        <RouteTargetEditor :model="link" prefix="link" :patient-route-options="patientRouteOptions" include-sort include-enabled />
      </div>
      <button type="button" class="danger-link" @click="removeLink(section, linkIndex)">删除入口</button>
    </div>
    <div class="nested-list-head">
      <strong>{{ section.type === "doctor_list" ? "医生姓名" : "科室名称" }}</strong>
      <button type="button" class="topbar-refresh" @click="section.fallbackNames.push(section.type === 'doctor_list' ? '新医生' : '新科室')">新增名称</button>
    </div>
    <div v-for="(_name, nameIndex) in section.fallbackNames" :key="`resource-name-${nameIndex}`" class="config-row-card">
      <label><span>名称</span><input v-model.trim="section.fallbackNames[nameIndex]" type="text"></label>
      <button type="button" class="danger-link" @click="removeFallbackName(section, nameIndex)">删除名称</button>
    </div>
  </div>
</template>
