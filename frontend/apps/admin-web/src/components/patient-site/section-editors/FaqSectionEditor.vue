<script setup lang="ts">
import type { FaqSection } from "@smart-cloud-brain/shared-api";

defineProps<{ section: FaqSection }>();

function addFaq(section: FaqSection) {
  section.items.push({ question: "新问题", answer: "" });
}
</script>

<template>
  <div class="nested-list">
    <div class="nested-list-head">
      <strong>问答列表</strong>
      <button type="button" class="topbar-refresh" @click="addFaq(section)">新增问答</button>
    </div>
    <div v-for="(item, itemIndex) in section.items" :key="`faq-${itemIndex}`" class="config-row-card">
      <div class="config-grid two">
        <label><span>问题</span><input v-model.trim="item.question" type="text"></label>
        <label><span>答案</span><textarea v-model.trim="item.answer" rows="3"></textarea></label>
      </div>
      <button type="button" class="danger-link" @click="section.items.splice(itemIndex, 1)">删除问答</button>
    </div>
  </div>
</template>
