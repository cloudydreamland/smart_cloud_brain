<script setup lang="ts">
import type { FaqSection } from "@smart-cloud-brain/shared-api";

defineProps<{ section: FaqSection }>();

function addFaq(section: FaqSection) {
  section.items.push({ question: "New question", answer: "" });
}
</script>

<template>
  <div class="nested-list">
    <div class="nested-list-head">
      <strong>items</strong>
      <button type="button" class="topbar-refresh" @click="addFaq(section)">Add FAQ</button>
    </div>
    <div v-for="(item, itemIndex) in section.items" :key="`faq-${itemIndex}`" class="config-row-card">
      <div class="config-grid two">
        <label><span>question</span><input v-model.trim="item.question" type="text"></label>
        <label><span>answer</span><textarea v-model.trim="item.answer" rows="3"></textarea></label>
      </div>
      <button type="button" class="danger-link" @click="section.items.splice(itemIndex, 1)">Delete FAQ</button>
    </div>
  </div>
</template>
