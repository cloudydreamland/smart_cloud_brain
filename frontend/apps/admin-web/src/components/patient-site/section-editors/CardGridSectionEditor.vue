<script setup lang="ts">
import type { CardGridSection, RouteTargetConfig } from "@smart-cloud-brain/shared-api";
import RouteTargetEditor from "../RouteTargetEditor.vue";

defineProps<{
  section: CardGridSection;
  patientRouteOptions: readonly { name: string; label: string }[];
}>();

const emptyLink = (): RouteTargetConfig => ({ label: "", routeName: "patient-home", enabled: true, sort: 0 });

function addCard(section: CardGridSection) {
  section.cards.push({ title: "New card", text: "" });
}
</script>

<template>
  <div class="nested-list">
    <div class="nested-list-head">
      <strong>cards</strong>
      <button type="button" class="topbar-refresh" @click="addCard(section)">Add card</button>
    </div>
    <div v-for="(card, cardIndex) in section.cards" :key="`card-${cardIndex}`" class="config-row-card">
      <div class="config-grid two">
        <label><span>title</span><input v-model.trim="card.title" type="text"></label>
        <label><span>meta</span><input v-model.trim="card.meta" type="text"></label>
        <label><span>text</span><textarea v-model.trim="card.text" rows="3"></textarea></label>
      </div>
      <div class="nested-list-head">
        <strong>image</strong>
        <button v-if="!card.image" type="button" class="topbar-refresh" @click="card.image = { url: '', alt: '' }">Add image</button>
        <button v-else type="button" class="danger-link" @click="card.image = undefined">Remove image</button>
      </div>
      <div v-if="card.image" class="config-grid two">
        <label><span>image.url</span><input v-model.trim="card.image.url" type="text"></label>
        <label><span>image.alt</span><input v-model.trim="card.image.alt" type="text"></label>
      </div>
      <div class="nested-list-head">
        <strong>target</strong>
        <button v-if="!card.target" type="button" class="topbar-refresh" @click="card.target = emptyLink()">Add target</button>
        <button v-else type="button" class="danger-link" @click="card.target = undefined">Remove target</button>
      </div>
      <div v-if="card.target" class="config-grid two">
        <RouteTargetEditor :model="card.target" prefix="target" :patient-route-options="patientRouteOptions" />
      </div>
      <button type="button" class="danger-link" @click="section.cards.splice(cardIndex, 1)">Delete card</button>
    </div>
  </div>
</template>
