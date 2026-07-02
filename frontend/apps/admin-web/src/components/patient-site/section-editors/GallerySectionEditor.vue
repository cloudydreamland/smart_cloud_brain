<script setup lang="ts">
import type { GallerySection } from "@smart-cloud-brain/shared-api";
import { usePatientSiteConfirm } from "../../../composables/patientSiteConfirm";
import OssImageUploadField from "../OssImageUploadField.vue";

defineProps<{
  section: GallerySection;
}>();
const confirm = usePatientSiteConfirm();

async function removeImage(section: GallerySection, index: number) {
  const image = section.images[index];
  if (!image || !(await confirm({
    title: "确认删除图库图片",
    message: `将从当前编辑稿中移除图片「${image.alt || image.objectKey || image.url || "未命名图片"}」。保存草稿不会影响患者端，保存并生效或发布后，对应页面区块才会不再展示该图片。`,
    confirmText: "确认删除",
    tone: "danger",
  }))) return;
  section.images.splice(index, 1);
}
</script>

<template>
  <div class="nested-list">
    <div class="nested-list-head">
      <strong>图片列表</strong>
      <button type="button" class="topbar-refresh" @click="section.images.push({ url: '', alt: '' })">新增图片</button>
    </div>
    <div v-for="(image, imageIndex) in section.images" :key="`gallery-image-${imageIndex}`" class="config-row-card">
      <OssImageUploadField
        :image-url="image.url"
        :image-alt="image.alt"
        :object-key="image.objectKey"
        label="图库图片"
        @uploaded="({ url, objectKey }) => { image.url = url; image.objectKey = objectKey; }"
        @update:image-alt="image.alt = $event"
        @cleared="() => { image.url = ''; image.alt = ''; image.objectKey = ''; }"
      />
      <button type="button" class="danger-link" @click="removeImage(section, imageIndex)">删除图片</button>
    </div>
  </div>
</template>
