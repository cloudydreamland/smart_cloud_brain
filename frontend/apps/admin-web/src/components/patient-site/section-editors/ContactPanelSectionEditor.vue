<script setup lang="ts">
import type { ContactPanelSection, RouteTargetConfig } from "@smart-cloud-brain/shared-api";
import { usePatientSiteConfirm } from "../../../composables/patientSiteConfirm";
import OssImageUploadField from "../OssImageUploadField.vue";
import RouteTargetEditor from "../RouteTargetEditor.vue";

defineProps<{
  section: ContactPanelSection;
  patientRouteOptions: readonly { name: string; label: string }[];
}>();

const emptyLink = (): RouteTargetConfig => ({ label: "", routeName: "patient-home", enabled: true, sort: 0 });
const confirm = usePatientSiteConfirm();

async function clearImage(section: ContactPanelSection) {
  if (!section.image || !(await confirm({
    title: "确认移除联系面板图片",
    message: "将从当前编辑稿中移除联系面板的图片配置。保存并生效或发布后，患者端对应区块才会更新展示。",
    confirmText: "确认清空图片",
    tone: "danger",
  }))) return;
  section.image = undefined;
}

async function clearPrimary(section: ContactPanelSection) {
  if (!section.primary || !(await confirm({
    title: "确认移除行动入口",
    message: `将从当前编辑稿中移除行动入口「${section.primary.label || "未命名入口"}」。保存并生效或发布后，患者端对应区块才会不再展示该入口。`,
    confirmText: "确认删除",
    tone: "danger",
  }))) return;
  section.primary = undefined;
}
</script>

<template>
  <div class="nested-list">
    <div class="config-grid two">
      <label><span>说明</span><textarea v-model.trim="section.text" rows="4"></textarea></label>
      <label><span>电话</span><input v-model.trim="section.phone" type="text"></label>
      <label><span>地址</span><input v-model.trim="section.address" type="text"></label>
      <label><span>服务时间</span><input v-model.trim="section.workHours" type="text"></label>
    </div>
    <div class="nested-list-head">
      <strong>图片</strong>
      <button v-if="!section.image" type="button" class="topbar-refresh" @click="section.image = { url: '', alt: '' }">添加图片</button>
      <button v-else type="button" class="danger-link" @click="clearImage(section)">移除图片</button>
    </div>
    <OssImageUploadField
      v-if="section.image"
      :image-url="section.image.url"
      :image-alt="section.image.alt"
      :object-key="section.image.objectKey"
      label="联系面板图片"
      @uploaded="({ url, objectKey }) => { if (section.image) { section.image.url = url; section.image.objectKey = objectKey; } }"
      @update:image-alt="(value) => { if (section.image) section.image.alt = value; }"
      @cleared="() => { if (section.image) { section.image.url = ''; section.image.alt = ''; section.image.objectKey = ''; } }"
    />
    <div class="nested-list-head">
      <strong>行动入口</strong>
      <button v-if="!section.primary" type="button" class="topbar-refresh" @click="section.primary = emptyLink()">添加入口</button>
      <button v-else type="button" class="danger-link" @click="clearPrimary(section)">移除入口</button>
    </div>
    <div v-if="section.primary" class="config-grid two">
      <RouteTargetEditor :model="section.primary" prefix="primary" :patient-route-options="patientRouteOptions" />
    </div>
  </div>
</template>
