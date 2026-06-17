<script setup lang="ts">
import { storeToRefs } from "pinia";
import { fieldText, useAuthStore, usePatientWorkflowStore } from "@smart-cloud-brain/shared-api";
import { EmptyState } from "@smart-cloud-brain/shared-ui";

const auth = useAuthStore();
const workflow = usePatientWorkflowStore();
const { patient } = storeToRefs(workflow);
</script>

<template>
  <section class="portal-grid">
    <section class="panel">
      <header class="panel-header"><div class="panel-title"><p class="eyebrow">PROFILE</p><h2>患者资料</h2><p>后端患者信息只读展示，修改应由后续资料维护接口承接。</p></div></header>
      <div class="panel-body">
        <dl v-if="patient" class="detail-list">
          <div><dt>姓名</dt><dd>{{ fieldText(patient, "name", auth.session?.name || "-") }}</dd></div>
          <div><dt>手机号</dt><dd>{{ fieldText(patient, "phone", "-") }}</dd></div>
          <div><dt>年龄</dt><dd>{{ fieldText(patient, "age", "-") }}</dd></div>
          <div><dt>过敏史</dt><dd>{{ fieldText(patient, "allergyHistory", "未记录") }}</dd></div>
          <div><dt>既往史</dt><dd>{{ fieldText(patient, "pastHistory", "未记录") }}</dd></div>
        </dl>
        <EmptyState v-else title="暂无患者资料" message="请刷新或重新登录。" />
      </div>
    </section>
    <aside class="panel">
      <header class="panel-header"><div class="panel-title"><h2>账号状态</h2><p>登录态守卫会保护患者门户页面。</p></div></header>
      <div class="panel-body">
        <div class="summary-strip">
          <div class="summary-item"><span>角色</span><strong>{{ auth.session?.role }}</strong></div>
          <div class="summary-item"><span>用户 ID</span><strong>{{ auth.session?.userId }}</strong></div>
          <div class="summary-item"><span>状态</span><strong>已登录</strong></div>
        </div>
      </div>
    </aside>
  </section>
</template>
