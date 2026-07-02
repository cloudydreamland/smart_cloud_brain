<script setup lang="ts">
import type { FaqSection } from "@smart-cloud-brain/shared-api";
import { usePatientSiteConfirm } from "../../../composables/patientSiteConfirm";

defineProps<{ section: FaqSection }>();
const confirm = usePatientSiteConfirm();

function addFaq(section: FaqSection) {
  section.items.push({ question: "新问题", answer: "" });
}

async function removeFaq(section: FaqSection, index: number) {
  const item = section.items[index];
  if (!item || !(await confirm({
    title: "确认删除问答",
    message: `将从当前编辑稿中移除问答「${item.question || "未命名问题"}」。保存草稿不会影响患者端，保存并生效或发布后，对应页面区块才会不再展示该问答。`,
    confirmText: "确认删除",
    tone: "danger",
  }))) return;
  section.items.splice(index, 1);
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
      <button type="button" class="danger-link" @click="removeFaq(section, itemIndex)">删除问答</button>
    </div>
  </div>
</template>
