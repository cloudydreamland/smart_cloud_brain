<script setup lang="ts">
import { reactive, ref } from "vue";
import { api, fieldText, formatApiError, useAuthStore, type DataRow } from "@smart-cloud-brain/shared-api";
import { EmptyState, ErrorState, FormField, LoadingState } from "@smart-cloud-brain/shared-ui";

const auth = useAuthStore();
const loading = ref(false);
const error = ref("");
const form = reactive({ q: "", departmentCode: "" });
const results = reactive({ knowledge: [] as DataRow[], drugs: [] as DataRow[], prompts: [] as DataRow[] });

async function search() {
  if (!form.q.trim()) {
    error.value = "请输入检索关键词。";
    return;
  }
  loading.value = true;
  error.value = "";
  try {
    const [knowledge, drugs, prompts] = await Promise.all([
      api.searchKnowledge(auth.token(), form.q.trim(), form.departmentCode.trim()),
      api.searchDrugs(auth.token(), form.q.trim()),
      api.searchPrompts(auth.token(), form.q.trim()),
    ]);
    results.knowledge = knowledge;
    results.drugs = drugs;
    results.prompts = prompts;
  } catch (err) {
    error.value = formatApiError(err, "检索失败");
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <section class="panel">
    <header class="panel-header"><div class="panel-title"><p class="eyebrow">综合检索</p><h2>知识、药品和提示词检索</h2><p>统一调用后端检索接口核对配置内容。</p></div></header>
    <div class="panel-body stack">
      <ErrorState v-if="error" :message="error" />
      <div class="form-grid">
        <FormField label="关键词"><input v-model.trim="form.q" /></FormField>
        <FormField label="科室编码"><input v-model.trim="form.departmentCode" /></FormField>
      </div>
      <div class="toolbar"><button type="button" class="primary" :disabled="loading" @click="search">检索</button></div>
      <LoadingState v-if="loading" title="正在检索" />
      <div v-else class="search-results">
        <section class="panel">
          <header class="panel-header"><div class="panel-title"><h3>知识库</h3><p>{{ results.knowledge.length }} 条</p></div></header>
          <div class="list"><article v-for="item in results.knowledge" :key="String(item.id)" class="list-row"><div class="row-main"><strong>{{ fieldText(item, "title") }}</strong><p>{{ fieldText(item, "advice") }}</p></div></article><EmptyState v-if="!results.knowledge.length" title="暂无知识结果" /></div>
        </section>
        <section class="panel">
          <header class="panel-header"><div class="panel-title"><h3>药品</h3><p>{{ results.drugs.length }} 条</p></div></header>
          <div class="list"><article v-for="item in results.drugs" :key="String(item.id)" class="list-row"><div class="row-main"><strong>{{ fieldText(item, "name") }}</strong><p>{{ fieldText(item, "specification") }}</p></div></article><EmptyState v-if="!results.drugs.length" title="暂无药品结果" /></div>
        </section>
        <section class="panel">
          <header class="panel-header"><div class="panel-title"><h3>提示词</h3><p>{{ results.prompts.length }} 条</p></div></header>
          <div class="list"><article v-for="item in results.prompts" :key="String(item.id)" class="list-row"><div class="row-main"><strong>{{ fieldText(item, "templateName") }}</strong><p>{{ fieldText(item, "taskType") }}</p></div></article><EmptyState v-if="!results.prompts.length" title="暂无提示词结果" /></div>
        </section>
      </div>
    </div>
  </section>
</template>
