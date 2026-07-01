<script setup lang="ts">
import type { PatientHospitalInfoConfig } from "@smart-cloud-brain/shared-api";
import OssImageUploadField from "./OssImageUploadField.vue";

const props = defineProps<{
  hospitalDraft: PatientHospitalInfoConfig;
  toggleEnabled: (item: { enabled?: boolean }) => void;
}>();

function addLocation() {
  props.hospitalDraft.locations.push({
    name: "新院区",
    address: "",
    phone: "",
    imageUrl: "",
    imageObjectKey: "",
    sort: nextSort(),
    enabled: true,
  });
}

function removeLocation(index: number) {
  const location = props.hospitalDraft.locations[index];
  if (!location || !window.confirm(`确认删除院区「${location.name || "未命名院区"}」？删除后只会影响当前编辑稿，发布或保存并生效后才会更新正式页面。`)) return;
  location.enabled = false;
}

function nextSort() {
  return props.hospitalDraft.locations.reduce((value, item) => Math.max(value, item.sort || 0), 0) + 10;
}
</script>

<template>
  <section class="config-section">
    <div class="config-section-head">
      <div>
        <h3>医院基础信息</h3>
      </div>
    </div>
    <div class="config-form-grid">
      <label><span>医院名称</span><input v-model.trim="hospitalDraft.name" type="text"></label>
      <label><span>联系电话</span><input v-model.trim="hospitalDraft.phone" type="text"></label>
      <label><span>门诊时间</span><input v-model.trim="hospitalDraft.workHours" type="text"></label>
      <label><span>官网地址</span><input v-model.trim="hospitalDraft.website" type="text"></label>
      <label class="span-2"><span>医院地址</span><input v-model.trim="hospitalDraft.address" type="text"></label>
      <label class="span-2"><span>医院简介</span><textarea v-model.trim="hospitalDraft.intro" rows="4"></textarea></label>
    </div>
  </section>

  <section class="config-section">
    <div class="config-section-head">
      <div>
        <h3>院区信息</h3>
        <p>院区会用于患者端首页和到院服务展示。</p>
      </div>
      <button type="button" class="topbar-refresh" @click="addLocation">新增院区</button>
    </div>
    <div class="config-list">
      <article v-for="(location, index) in hospitalDraft.locations" :key="`${location.name}-${index}`" class="config-row-card">
        <div class="config-form-grid">
          <label><span>院区名称</span><input v-model.trim="location.name" type="text"></label>
          <label><span>排序</span><input v-model.number="location.sort" type="number"></label>
          <label><span>院区电话</span><input v-model.trim="location.phone" type="text"></label>
          <label><span>院区地址</span><input v-model.trim="location.address" type="text"></label>
          <OssImageUploadField
            v-model:image-url="location.imageUrl"
            v-model:object-key="location.imageObjectKey"
            class="span-2"
            label="院区图片"
          />
        </div>
        <div class="config-card-actions">
          <button type="button" class="status-pill" :class="location.enabled === false ? 'disabled' : 'enabled'" @click="toggleEnabled(location)">
            {{ location.enabled === false ? "禁用" : "启用" }}
          </button>
          <button type="button" class="danger-link" @click="removeLocation(index)">删除</button>
        </div>
      </article>
    </div>
  </section>
</template>
