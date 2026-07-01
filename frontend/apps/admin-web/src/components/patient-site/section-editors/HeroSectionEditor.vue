<script setup lang="ts">
import type { HeroSection, RouteTargetConfig } from "@smart-cloud-brain/shared-api";
import OssImageUploadField from "../OssImageUploadField.vue";
import RouteTargetEditor from "../RouteTargetEditor.vue";

defineProps<{
  section: HeroSection;
  patientRouteOptions: readonly { name: string; label: string }[];
}>();

const emptyLink = (): RouteTargetConfig => ({ label: "", routeName: "patient-home", enabled: true, sort: 0 });

function clearImage(section: HeroSection) {
  if (!section.image || !window.confirm("确认移除首屏图片？移除后只会影响当前编辑稿，发布或保存并生效后才会更新正式页面。")) return;
  section.image = undefined;
}
</script>

<template>
  <div class="nested-list">
    <div class="config-grid two">
      <label><span>眉题</span><input v-model.trim="section.eyebrow" type="text"></label>
      <label><span>说明</span><textarea v-model.trim="section.text" rows="4"></textarea></label>
    </div>
    <div class="nested-list-head">
      <strong>首屏图片</strong>
      <button v-if="!section.image" type="button" class="topbar-refresh" @click="section.image = { url: '', alt: '' }">添加图片</button>
      <button v-else type="button" class="danger-link" @click="clearImage(section)">移除图片</button>
    </div>
    <OssImageUploadField
      v-if="section.image"
      :image-url="section.image.url"
      :image-alt="section.image.alt"
      :object-key="section.image.objectKey"
      label="首屏图片"
      @uploaded="({ url, objectKey }) => { if (section.image) { section.image.url = url; section.image.objectKey = objectKey; } }"
      @update:image-alt="(value) => { if (section.image) section.image.alt = value; }"
      @cleared="() => { if (section.image) { section.image.url = ''; section.image.alt = ''; section.image.objectKey = ''; } }"
    />
    <div class="config-grid two">
      <div>
        <div class="nested-list-head">
          <strong>主按钮</strong>
          <button v-if="!section.primary" type="button" class="topbar-refresh" @click="section.primary = emptyLink()">添加</button>
        </div>
        <RouteTargetEditor v-if="section.primary" :model="section.primary" prefix="primary" :patient-route-options="patientRouteOptions" />
      </div>
      <div>
        <div class="nested-list-head">
          <strong>次按钮</strong>
          <button v-if="!section.secondary" type="button" class="topbar-refresh" @click="section.secondary = emptyLink()">添加</button>
        </div>
        <RouteTargetEditor v-if="section.secondary" :model="section.secondary" prefix="secondary" :patient-route-options="patientRouteOptions" />
      </div>
    </div>
  </div>
</template>
