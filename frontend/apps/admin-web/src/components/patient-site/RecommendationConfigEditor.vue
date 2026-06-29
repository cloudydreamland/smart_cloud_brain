<script setup lang="ts">
import OssImageUploadField from "./OssImageUploadField.vue";
import { usePatientRecommendationEditor } from "../../composables/usePatientRecommendationEditor";

const editor = usePatientRecommendationEditor();
const { activeType, recommendations, editing, targetOptions, loading, saving, status, error } = editor;
</script>

<template>
  <section class="config-section">
    <div class="config-section-head">
      <div>
        <h3>推荐内容</h3>
        <p>患者端热门科室和推荐医生优先读取这里启用的内容。</p>
      </div>
      <button type="button" class="topbar-refresh" @click="editor.startCreate">新增推荐</button>
    </div>

    <div class="inline-actions">
      <button type="button" class="status-pill" :class="activeType === 'DEPARTMENT' ? 'enabled' : 'disabled'" @click="editor.switchType('DEPARTMENT')">热门科室</button>
      <button type="button" class="status-pill" :class="activeType === 'DOCTOR' ? 'enabled' : 'disabled'" @click="editor.switchType('DOCTOR')">推荐医生</button>
      <button type="button" class="topbar-refresh" :disabled="loading" @click="editor.load">刷新</button>
    </div>

    <div v-if="status" class="notice success">{{ status }}</div>
    <div v-if="error" class="notice error">{{ error }}</div>

    <article v-if="editing" class="config-card">
      <div class="config-form-grid">
        <label>
          <span>{{ activeType === "DEPARTMENT" ? "选择科室" : "选择医生" }}</span>
          <select v-model.number="editing.targetId">
            <option :value="0">请选择</option>
            <option v-for="target in targetOptions" :key="target.id" :value="target.id">{{ target.label }}</option>
          </select>
        </label>
        <label><span>排序</span><input v-model.number="editing.sort" type="number"></label>
        <label><span>展示标题</span><input v-model.trim="editing.title" type="text" placeholder="可留空，默认使用目标名称"></label>
        <label class="check-field"><input v-model="editing.status" true-value="ENABLED" false-value="DISABLED" type="checkbox"><span>启用</span></label>
        <label class="span-2"><span>展示说明</span><textarea v-model.trim="editing.description" rows="4"></textarea></label>
        <OssImageUploadField
          v-model:image-url="editing.imageUrl"
          v-model:object-key="editing.imageObjectKey"
          class="span-2"
          label="展示图片"
        />
      </div>
      <div class="config-card-actions">
        <button type="button" class="quick-btn publish" :disabled="saving" @click="editor.save">保存推荐</button>
        <button type="button" class="topbar-refresh" @click="editor.cancelEdit">取消</button>
      </div>
    </article>

    <div class="config-list">
      <article v-for="row in recommendations" :key="row.id" class="config-row-card config-summary-row">
        <div>
          <strong>{{ row.title || row.targetName || "未命名推荐" }}</strong>
          <span>{{ row.description || row.departmentName || row.specialty || "未填写说明" }}</span>
          <small>排序：{{ row.sort ?? 0 }} · 目标：{{ row.targetName || row.targetId }}</small>
        </div>
        <div class="config-card-actions">
          <button type="button" class="status-pill" :class="row.status === 'DISABLED' ? 'disabled' : 'enabled'" @click="editor.toggleStatus(row)">
            {{ row.status === "DISABLED" ? "禁用" : "启用" }}
          </button>
          <button type="button" class="topbar-refresh" @click="editor.startEdit(row)">编辑</button>
          <button type="button" class="danger-link" @click="editor.remove(row.id)">删除</button>
        </div>
      </article>
    </div>
    <p v-if="!loading && !recommendations.length" class="muted-hint">暂无推荐内容。</p>
  </section>
</template>
