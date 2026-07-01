<script setup lang="ts">
import type { ImageTextSection, RouteTargetConfig } from "@smart-cloud-brain/shared-api";
import { ScbSelect } from "@smart-cloud-brain/shared-ui";
import OssImageUploadField from "../OssImageUploadField.vue";
import RouteTargetEditor from "../RouteTargetEditor.vue";

defineProps<{
  section: ImageTextSection;
  patientRouteOptions: readonly { name: string; label: string }[];
}>();

const imagePositionOptions = [
  { value: "left", label: "图片在左" },
  { value: "right", label: "图片在右" },
];
const emptyLink = (): RouteTargetConfig => ({ label: "", routeName: "patient-home", enabled: true, sort: 0 });

function clearImage(section: ImageTextSection) {
  if (!section.image || !window.confirm("确认移除图文图片？移除后只会影响当前编辑稿，发布或保存并生效后才会更新正式页面。")) return;
  section.image = undefined;
}

function clearPrimary(section: ImageTextSection) {
  if (!section.primary || !window.confirm(`确认移除行动入口「${section.primary.label || "未命名入口"}」？移除后只会影响当前编辑稿，发布或保存并生效后才会更新正式页面。`)) return;
  section.primary = undefined;
}
</script>

<template>
  <div class="nested-list">
    <div class="config-grid two">
      <label><span>正文</span><textarea v-model.trim="section.text" rows="4"></textarea></label>
      <label>
        <span>图片位置</span>
        <ScbSelect v-model="section.imagePosition" :options="imagePositionOptions" close-on-scroll />
      </label>
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
      label="图文图片"
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
