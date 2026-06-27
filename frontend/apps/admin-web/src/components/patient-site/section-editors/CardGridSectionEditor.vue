<script setup lang="ts">
import type { CardGridSection, RouteTargetConfig } from "@smart-cloud-brain/shared-api";
import RouteTargetEditor from "../RouteTargetEditor.vue";

defineProps<{
  section: CardGridSection;
  patientRouteOptions: readonly { name: string; label: string }[];
}>();

const emptyLink = (): RouteTargetConfig => ({ label: "", routeName: "patient-home", enabled: true, sort: 0 });

function addCard(section: CardGridSection) {
  section.cards.push({ title: "新卡片", text: "" });
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
        <button v-else type="button" class="danger-link" @click="card.image = undefined">移除图片</button>
      </div>
      <div v-if="card.image" class="config-grid two">
        <label><span>图片 URL</span><input v-model.trim="card.image.url" type="text"></label>
        <label><span>图片说明</span><input v-model.trim="card.image.alt" type="text"></label>
      </div>
      <div class="nested-list-head">
        <strong>跳转目标</strong>
        <button v-if="!card.target" type="button" class="topbar-refresh" @click="card.target = emptyLink()">添加目标</button>
        <button v-else type="button" class="danger-link" @click="card.target = undefined">移除目标</button>
      </div>
      <div v-if="card.target" class="config-grid two">
        <RouteTargetEditor :model="card.target" prefix="target" :patient-route-options="patientRouteOptions" />
      </div>
      <button type="button" class="danger-link" @click="section.cards.splice(cardIndex, 1)">删除卡片</button>
    </div>
  </div>
</template>
