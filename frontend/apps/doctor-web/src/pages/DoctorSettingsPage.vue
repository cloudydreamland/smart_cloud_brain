<script setup lang="ts">
import { computed, inject, onMounted, ref, watch } from "vue";
import { useAuthStore } from "@smart-cloud-brain/shared-api";
import { statusLabel } from "../doctorPresentation";

const auth = useAuthStore();
const toast = inject<Ref<{ success: (t: string, d?: string) => void; error: (t: string, d?: string) => void }> | null>(null);

/* ── 设置项 ── */
const aiDraftMode = ref<"preview" | "auto">("preview");
const highRiskConfirm = ref(true);
const notifyMode = ref<"realtime" | "queue" | "dnd">("realtime");

/* ── 读取 localStorage ── */
onMounted(() => {
  try {
    const saved = JSON.parse(localStorage.getItem("doctor-settings") || "{}");
    if (saved.aiDraftMode) aiDraftMode.value = saved.aiDraftMode;
    if (saved.highRiskConfirm !== undefined) highRiskConfirm.value = saved.highRiskConfirm;
    if (saved.notifyMode) notifyMode.value = saved.notifyMode;
  } catch { /* ignore */ }
});

/* ── 自动保存：任意设置变化即写入 localStorage ── */
function persist() {
  localStorage.setItem("doctor-settings", JSON.stringify({
    aiDraftMode: aiDraftMode.value,
    highRiskConfirm: highRiskConfirm.value,
    notifyMode: notifyMode.value,
  }));
  toast?.value?.success("设置已保存");
}

watch([aiDraftMode, highRiskConfirm, notifyMode], persist);

/* ── 通知方式标签 ── */
const notifyLabel = computed(() => {
  const map: Record<string, string> = { realtime: "实时推送", queue: "仅队列刷新", dnd: "免打扰" };
  return map[notifyMode.value] || notifyMode.value;
});
</script>

<template>
  <section class="panel settings-page">
    <header>
      <div class="panel-title">
        <h2>工作台偏好</h2>
      </div>
    </header>

    <div class="panel-body stack">
      <!-- 摘要条 -->
      <div class="summary-strip">
        <div class="summary-item">
          <span>医生</span>
          <strong>{{ auth.session?.name || "张医生" }}</strong>
        </div>
        <div class="summary-item">
          <span>角色</span>
          <strong>{{ statusLabel(auth.session?.role, "医生") }}</strong>
        </div>
        <div class="summary-item">
          <span>通知</span>
          <strong>{{ notifyLabel }}</strong>
        </div>
      </div>

      <!-- 设置区域 -->
      <div class="settings-stack">
        <!-- 1. AI 草稿策略 -->
        <article class="setting-row">
          <div class="setting-row-header">
            <strong>AI 草稿策略</strong>
          </div>
          <div class="choicebox-group">
            <div
              class="choicebox-item"
              :class="{ selected: aiDraftMode === 'preview' }"
              @click="aiDraftMode = 'preview'"
            >
              <div class="choicebox-indicator">
                <span class="radio-dot" />
              </div>
              <div class="choicebox-text">
                <span class="choicebox-title">预览模式</span>
                <span class="choicebox-desc">生成后放入侧边栏预览，手动复制需要的部分</span>
              </div>
            </div>
            <div
              class="choicebox-item"
              :class="{ selected: aiDraftMode === 'auto' }"
              @click="aiDraftMode = 'auto'"
            >
              <div class="choicebox-indicator">
                <span class="radio-dot" />
              </div>
              <div class="choicebox-text">
                <span class="choicebox-title">自动填入</span>
                <span class="choicebox-desc">生成后直接填入病历表单，再由医生修改</span>
              </div>
            </div>
          </div>
        </article>

        <!-- 3. 高危处方确认 -->
        <article class="setting-row">
          <div class="setting-row-header">
            <strong>高危处方二次确认</strong>
            <div class="setting-row-right">
              <span class="toggle-label">{{ highRiskConfirm ? "已开启" : "已关闭" }}</span>
              <button
                class="toggle-switch"
                :class="{ on: highRiskConfirm }"
                type="button"
                role="switch"
                :aria-checked="highRiskConfirm"
                @click="highRiskConfirm = !highRiskConfirm"
              >
                <span class="toggle-knob" />
              </button>
            </div>
          </div>
        </article>

        <!-- 4. 通知方式 -->
        <article class="setting-row">
          <div class="setting-row-header">
            <strong>通知方式</strong>
          </div>
          <div class="segmented-control">
            <button
              :class="{ active: notifyMode === 'realtime' }"
              type="button"
              @click="notifyMode = 'realtime'"
            >实时推送</button>
            <button
              :class="{ active: notifyMode === 'queue' }"
              type="button"
              @click="notifyMode = 'queue'"
            >仅队列刷新</button>
            <button
              :class="{ active: notifyMode === 'dnd' }"
              type="button"
              @click="notifyMode = 'dnd'"
            >免打扰</button>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>
