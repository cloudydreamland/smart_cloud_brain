<script setup lang="ts">
import { computed, inject, ref, type Ref } from "vue";
import { useAuthStore } from "@smart-cloud-brain/shared-api";
import { statusLabel } from "../doctorPresentation";
import { useDoctorSettings } from "../composables/useDoctorSettings";
import type { DoctorSettings } from "../composables/useDoctorSettings";

const auth = useAuthStore();
const toast = inject<Ref<{ success: (t: string, d?: string) => void; error: (t: string, d?: string) => void } | null>>("toast", ref(null));
const { settings, notifyLabel, setAiDraftMode, setHighRiskConfirm, setNotifyMode } = useDoctorSettings();

const aiDraftLabel = computed(() => settings.aiDraftMode === "auto" ? "自动填入" : "预览模式");
const highRiskLabel = computed(() => settings.highRiskConfirm ? "已开启" : "已关闭");
const settingEffectItems = computed(() => [
  {
    title: "病历草稿",
    value: aiDraftLabel.value,
    desc: settings.aiDraftMode === "auto"
      ? "生成后直接写入接诊页病历表单"
      : "生成后先保留草稿，由医生确认后复制",
  },
  {
    title: "处方风险",
    value: highRiskLabel.value,
    desc: settings.highRiskConfirm
      ? "创建高危处方时会弹出二次确认"
      : "高危处方不再额外弹窗确认",
  },
  {
    title: "通知同步",
    value: notifyLabel.value,
    desc: settings.notifyMode === "realtime"
      ? "顶部状态使用实时通知连接"
      : settings.notifyMode === "queue"
        ? "关闭实时连接，仅保留队列刷新"
        : "关闭自动同步，只保留手动同步",
  },
]);

function saved() {
  toast?.value?.success("设置已保存");
}

function updateAiDraftMode(value: DoctorSettings["aiDraftMode"]) {
  setAiDraftMode(value);
  saved();
}

function updateHighRiskConfirm(value: boolean) {
  setHighRiskConfirm(value);
  saved();
}

function updateNotifyMode(value: DoctorSettings["notifyMode"]) {
  setNotifyMode(value);
  saved();
}
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
          <strong>{{ auth.session?.name || "--" }}</strong>
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

      <section class="settings-effect-panel" aria-label="当前已生效设置">
        <div class="settings-effect-title">
          <strong>当前已生效</strong>
          <span>切换后会立即影响医生工作台</span>
        </div>
        <div class="settings-effect-grid">
          <article v-for="item in settingEffectItems" :key="item.title" class="settings-effect-card">
            <span>{{ item.title }}</span>
            <strong>{{ item.value }}</strong>
            <p>{{ item.desc }}</p>
          </article>
        </div>
      </section>

      <!-- 设置区域 -->
      <div class="settings-stack">
        <!-- 1. AI 草稿策略 -->
        <article class="setting-row">
          <div class="setting-row-header">
            <div>
              <strong>AI 草稿策略</strong>
              <p>影响接诊页病历生成后的处理方式</p>
            </div>
          </div>
          <div class="choicebox-group">
            <div
              class="choicebox-item"
              :class="{ selected: settings.aiDraftMode === 'preview' }"
              @click="updateAiDraftMode('preview')"
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
              :class="{ selected: settings.aiDraftMode === 'auto' }"
              @click="updateAiDraftMode('auto')"
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
            <div>
              <strong>高危处方二次确认</strong>
              <p>影响接诊页创建高危处方时是否弹窗</p>
            </div>
            <div class="setting-row-right">
              <span class="toggle-label" :class="{ active: settings.highRiskConfirm }">{{ settings.highRiskConfirm ? "已开启" : "已关闭" }}</span>
              <button
                class="toggle-switch"
                :class="{ on: settings.highRiskConfirm }"
                type="button"
                role="switch"
                :aria-checked="settings.highRiskConfirm"
                @click="updateHighRiskConfirm(!settings.highRiskConfirm)"
              >
                <span class="toggle-knob" />
              </button>
            </div>
          </div>
        </article>

        <!-- 4. 通知方式 -->
        <article class="setting-row">
          <div class="setting-row-header">
            <div>
              <strong>通知方式</strong>
              <p>影响顶部状态 pill 与自动同步策略</p>
            </div>
          </div>
          <div class="segmented-control">
            <button
              :class="{ active: settings.notifyMode === 'realtime' }"
              type="button"
              @click="updateNotifyMode('realtime')"
            >实时推送</button>
            <button
              :class="{ active: settings.notifyMode === 'queue' }"
              type="button"
              @click="updateNotifyMode('queue')"
            >仅队列刷新</button>
            <button
              :class="{ active: settings.notifyMode === 'dnd' }"
              type="button"
              @click="updateNotifyMode('dnd')"
            >免打扰</button>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>
