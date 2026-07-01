<script setup lang="ts">
import type { CardGridSection, RouteTargetConfig } from "@smart-cloud-brain/shared-api";
import OssImageUploadField from "../OssImageUploadField.vue";
import RouteTargetEditor from "../RouteTargetEditor.vue";

defineProps<{
  section: CardGridSection;
  patientRouteOptions: readonly { name: string; label: string }[];
}>();

const emptyLink = (): RouteTargetConfig => ({ label: "", routeName: "patient-home", enabled: true, sort: 0 });

function addCard(section: CardGridSection) {
  section.cards.push({ title: "新卡片", text: "" });
}

function removeCard(section: CardGridSection, index: number) {
  const card = section.cards[index];
  if (!card || !window.confirm(`确认删除卡片「${card.title || "未命名卡片"}」？删除后只会影响当前编辑稿，发布或保存并生效后才会更新正式页面。`)) return;
  section.cards.splice(index, 1);
}

function clearCardImage(section: CardGridSection, index: number) {
  const card = section.cards[index];
  if (!card?.image || !window.confirm(`确认移除卡片「${card.title || "未命名卡片"}」的图片？移除后只会影响当前编辑稿。`)) return;
  card.image = undefined;
}

function clearCardTarget(section: CardGridSection, index: number) {
  const card = section.cards[index];
  if (!card?.target || !window.confirm(`确认移除卡片「${card.title || "未命名卡片"}」的跳转目标？移除后只会影响当前编辑稿。`)) return;
  card.target = undefined;
}
</script>

<template>
  <div class="nested-list">
    <div class="nested-list-head">
      <strong>卡片列表</strong>
      <button type="button" class="topbar-refresh" @click="addCard(section)">新增卡片</button>
    </div>
    <div v-for="(card, cardIndex) in section.cards" :key="`card-${cardIndex}`" class="config-row-card">
      <div class="config-grid two">
        <label><span>标题</span><input v-model.trim="card.title" type="text"></label>
        <label><span>辅助信息</span><input v-model.trim="card.meta" type="text"></label>
        <label><span>正文</span><textarea v-model.trim="card.text" rows="3"></textarea></label>
      </div>
      <div class="nested-list-head">
        <strong>图片</strong>
        <button v-if="!card.image" type="button" class="topbar-refresh" @click="card.image = { url: '', alt: '' }">添加图片</button>
        <button v-else type="button" class="danger-link" @click="clearCardImage(section, cardIndex)">移除图片</button>
      </div>
      <OssImageUploadField
        v-if="card.image"
        :image-url="card.image.url"
        :image-alt="card.image.alt"
        :object-key="card.image.objectKey"
        label="卡片图片"
        @uploaded="({ url, objectKey }) => { if (card.image) { card.image.url = url; card.image.objectKey = objectKey; } }"
        @update:image-alt="(value) => { if (card.image) card.image.alt = value; }"
        @cleared="() => { if (card.image) { card.image.url = ''; card.image.alt = ''; card.image.objectKey = ''; } }"
      />
      <div class="nested-list-head">
        <strong>跳转目标</strong>
        <button v-if="!card.target" type="button" class="topbar-refresh" @click="card.target = emptyLink()">添加目标</button>
        <button v-else type="button" class="danger-link" @click="clearCardTarget(section, cardIndex)">移除目标</button>
      </div>
      <div v-if="card.target" class="config-grid two">
        <RouteTargetEditor :model="card.target" prefix="target" :patient-route-options="patientRouteOptions" />
      </div>
      <button type="button" class="danger-link" @click="removeCard(section, cardIndex)">删除卡片</button>
    </div>
  </div>
</template>
