<script setup lang="ts">
import { usePatientNoticeEditor } from "../../composables/usePatientNoticeEditor";

const editor = usePatientNoticeEditor();
const { notices, editing, loading, saving, status, error } = editor;
</script>

<template>
  <section class="config-section">
    <div class="config-section-head">
      <div>
        <h3>公告通知</h3>
        <p>患者端只展示启用、未删除且处于生效时间范围内的公告。</p>
      </div>
      <button type="button" class="topbar-refresh" @click="editor.startCreate">新增公告</button>
    </div>

    <div v-if="status" class="notice success">{{ status }}</div>
    <div v-if="error" class="notice error">{{ error }}</div>

    <article v-if="editing" class="config-card">
      <div class="config-form-grid">
        <label><span>公告标题</span><input v-model.trim="editing.title" type="text"></label>
        <label><span>排序</span><input v-model.number="editing.sort" type="number"></label>
        <label>
          <span>链接类型</span>
          <select v-model="editing.linkType">
            <option value="NONE">无链接</option>
            <option value="INTERNAL">内部链接</option>
            <option value="EXTERNAL">外部链接</option>
          </select>
        </label>
        <label><span>链接地址</span><input v-model.trim="editing.linkUrl" type="text"></label>
        <label><span>生效时间</span><input v-model="editing.startTime" type="datetime-local"></label>
        <label><span>失效时间</span><input v-model="editing.endTime" type="datetime-local"></label>
        <label class="check-field"><input v-model="editing.pinned" type="checkbox"><span>置顶</span></label>
        <label class="check-field"><input v-model="editing.status" true-value="ENABLED" false-value="DISABLED" type="checkbox"><span>启用</span></label>
        <label class="span-2"><span>公告内容</span><textarea v-model.trim="editing.content" rows="5"></textarea></label>
      </div>
      <div class="config-card-actions">
        <button type="button" class="quick-btn publish" :disabled="saving" @click="editor.save">保存公告</button>
        <button type="button" class="topbar-refresh" @click="editor.cancelEdit">取消</button>
      </div>
    </article>

    <div class="config-list">
      <article v-for="notice in notices" :key="notice.id" class="config-row-card config-summary-row">
        <div>
          <strong>{{ notice.title }}</strong>
          <span>{{ notice.content }}</span>
          <small>排序：{{ notice.sort ?? 0 }} · {{ notice.pinned ? "置顶" : "普通" }} · {{ notice.startTime || "立即生效" }} 至 {{ notice.endTime || "长期有效" }}</small>
        </div>
        <div class="config-card-actions">
          <button type="button" class="status-pill" :class="notice.status === 'DISABLED' ? 'disabled' : 'enabled'" @click="editor.toggleStatus(notice)">
            {{ notice.status === "DISABLED" ? "禁用" : "启用" }}
          </button>
          <button type="button" class="topbar-refresh" @click="editor.startEdit(notice)">编辑</button>
          <button type="button" class="danger-link" @click="editor.remove(notice.id)">删除</button>
        </div>
      </article>
    </div>
    <p v-if="!loading && !notices.length" class="muted-hint">暂无公告。</p>
  </section>
</template>
