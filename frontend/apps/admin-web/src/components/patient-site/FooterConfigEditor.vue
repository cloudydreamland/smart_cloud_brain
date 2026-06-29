<script setup lang="ts">
import { computed, ref } from "vue";
import { IconPencil, IconPlus, IconTrash } from "@tabler/icons-vue";
import type { PatientFooterConfig, RouteTargetConfig } from "@smart-cloud-brain/shared-api";
import RouteTargetEditor from "./RouteTargetEditor.vue";

type LinkGroup = "links" | "legalLinks";
type FooterModal = "base" | LinkGroup;
type FooterBaseDraft = Pick<PatientFooterConfig, "brandName" | "description" | "copyright" | "contactPhone" | "contactAddress">;

const props = defineProps<{
  footerDraft: PatientFooterConfig;
  patientRouteOptions: readonly { name: string; label: string }[];
  toggleEnabled: (item: { enabled?: boolean }) => void;
}>();

const activeModal = ref<FooterModal | null>(null);
const baseDraft = ref<FooterBaseDraft>(emptyBaseDraft());
const linkDraft = ref<RouteTargetConfig[]>([]);

const routeLabelMap = computed(() => new Map(props.patientRouteOptions.map((route) => [route.name, route.label])));
const modalTitle = computed(() => {
  if (activeModal.value === "base") return "编辑页脚基础信息";
  if (activeModal.value === "legalLinks") return "编辑法律链接";
  return "编辑常用入口";
});
const modalDescription = computed(() => {
  if (activeModal.value === "base") return "维护页脚品牌、简介、电话、地址和版权信息。";
  if (activeModal.value === "legalLinks") return "维护隐私政策、使用条款等法律相关入口。";
  return "维护页脚展示的常用服务入口。";
});

function emptyBaseDraft(): FooterBaseDraft {
  return { brandName: "", description: "", copyright: "", contactPhone: "", contactAddress: "" };
}

function openBaseEditor() {
  baseDraft.value = {
    brandName: props.footerDraft.brandName,
    description: props.footerDraft.description,
    copyright: props.footerDraft.copyright,
    contactPhone: props.footerDraft.contactPhone,
    contactAddress: props.footerDraft.contactAddress,
  };
  activeModal.value = "base";
}

function openLinkEditor(group: LinkGroup) {
  linkDraft.value = props.footerDraft[group].map((link) => ({ ...link }));
  activeModal.value = group;
}

function closeModal() {
  activeModal.value = null;
  linkDraft.value = [];
}

function saveModal() {
  if (activeModal.value === "base") {
    Object.assign(props.footerDraft, baseDraft.value);
  } else if (activeModal.value) {
    props.footerDraft[activeModal.value].splice(0, props.footerDraft[activeModal.value].length, ...linkDraft.value);
  }
  closeModal();
}

function addDraftLink() {
  const group = activeModal.value === "legalLinks" ? "legalLinks" : "links";
  linkDraft.value.push({
    label: group === "legalLinks" ? "新法律链接" : "新入口",
    routeName: "patient-home",
    sort: nextSort(linkDraft.value),
    enabled: true,
  });
}

function removeDraftLink(index: number) {
  linkDraft.value[index].enabled = false;
}

function toggleDraftEnabled(item: RouteTargetConfig) {
  item.enabled = item.enabled === false;
}

function nextSort(items: RouteTargetConfig[]) {
  return items.reduce((value, item) => Math.max(value, item.sort || 0), 0) + 10;
}

function enabledCount(group: LinkGroup) {
  return props.footerDraft[group].filter((link) => link.enabled !== false).length;
}

function disabledCount(group: LinkGroup) {
  return props.footerDraft[group].filter((link) => link.enabled === false).length;
}

function summaryLinks(group: LinkGroup) {
  return [...props.footerDraft[group]]
    .filter((link) => link.enabled !== false)
    .sort((a, b) => (a.sort || 0) - (b.sort || 0))
    .slice(0, 3);
}

function routeText(link: RouteTargetConfig) {
  if (link.routeName === "cms-page" && link.slug) return `CMS：${link.slug}`;
  return routeLabelMap.value.get(link.routeName || "") || link.routeName || "未选择目标";
}
</script>

<template>
  <section class="footer-config-grid" aria-label="页脚配置卡片">
    <article class="config-card footer-config-card">
      <div class="footer-card-head">
        <div>
          <span class="footer-card-kicker">基础信息</span>
          <h3>{{ footerDraft.brandName || "未设置品牌名称" }}</h3>
          <p>{{ footerDraft.description || "还没有填写页脚简介。" }}</p>
        </div>
        <button type="button" class="topbar-refresh icon-action" aria-label="编辑页脚基础信息" @click="openBaseEditor">
          <IconPencil :size="15" />
          <span>编辑</span>
        </button>
      </div>
      <dl class="footer-card-meta">
        <div>
          <dt>联系电话</dt>
          <dd>{{ footerDraft.contactPhone || "-" }}</dd>
        </div>
        <div>
          <dt>联系地址</dt>
          <dd>{{ footerDraft.contactAddress || "-" }}</dd>
        </div>
        <div>
          <dt>版权信息</dt>
          <dd>{{ footerDraft.copyright || "-" }}</dd>
        </div>
      </dl>
    </article>

    <article class="config-card footer-config-card">
      <div class="footer-card-head">
        <div>
          <span class="footer-card-kicker">常用入口</span>
          <h3>{{ enabledCount("links") }} 个启用入口</h3>
          <p v-if="disabledCount('links')">{{ disabledCount("links") }} 个已禁用入口保留在草稿中。</p>
          <p v-else>页脚服务入口保持启用状态。</p>
        </div>
        <button type="button" class="topbar-refresh icon-action" aria-label="编辑常用入口" @click="openLinkEditor('links')">
          <IconPencil :size="15" />
          <span>编辑</span>
        </button>
      </div>
      <div class="footer-link-preview">
        <span v-for="link in summaryLinks('links')" :key="`${link.label}-${link.routeName}-${link.sort}`">
          {{ link.label || "未命名入口" }} / {{ routeText(link) }}
        </span>
        <span v-if="!summaryLinks('links').length" class="muted">暂无启用入口</span>
      </div>
    </article>

    <article class="config-card footer-config-card">
      <div class="footer-card-head">
        <div>
          <span class="footer-card-kicker">法律链接</span>
          <h3>{{ enabledCount("legalLinks") }} 个启用链接</h3>
          <p v-if="disabledCount('legalLinks')">{{ disabledCount("legalLinks") }} 个已禁用链接保留在草稿中。</p>
          <p v-else>隐私政策、使用条款等入口集中维护。</p>
        </div>
        <button type="button" class="topbar-refresh icon-action" aria-label="编辑法律链接" @click="openLinkEditor('legalLinks')">
          <IconPencil :size="15" />
          <span>编辑</span>
        </button>
      </div>
      <div class="footer-link-preview">
        <span v-for="link in summaryLinks('legalLinks')" :key="`${link.label}-${link.routeName}-${link.sort}`">
          {{ link.label || "未命名链接" }} / {{ routeText(link) }}
        </span>
        <span v-if="!summaryLinks('legalLinks').length" class="muted">暂无启用链接</span>
      </div>
    </article>
  </section>

  <div v-if="activeModal" class="patient-config-modal-backdrop" @click.self="closeModal">
    <section class="patient-config-modal-card footer-editor-modal-card" role="dialog" aria-modal="true" :aria-label="modalTitle">
      <button type="button" class="patient-config-modal-close" aria-label="关闭" @click="closeModal">×</button>
      <header class="patient-config-modal-head">
        <h2>{{ modalTitle }}</h2>
        <p>{{ modalDescription }}</p>
      </header>

      <div v-if="activeModal === 'base'" class="patient-config-modal">
        <div class="config-form-grid">
          <label><span>品牌名称</span><input v-model.trim="baseDraft.brandName" type="text"></label>
          <label><span>联系电话</span><input v-model.trim="baseDraft.contactPhone" type="text"></label>
          <label><span>联系地址</span><input v-model.trim="baseDraft.contactAddress" type="text"></label>
          <label><span>版权信息</span><input v-model.trim="baseDraft.copyright" type="text"></label>
          <label class="span-2"><span>页脚简介</span><textarea v-model.trim="baseDraft.description" rows="4"></textarea></label>
        </div>
      </div>

      <div v-else class="patient-config-modal">
        <div class="config-section-head">
          <div>
            <h3>{{ activeModal === "legalLinks" ? "法律链接列表" : "常用入口列表" }}</h3>
            <p>列表在此处编辑，外层只保留摘要卡片。</p>
          </div>
          <button type="button" class="topbar-refresh icon-action" @click="addDraftLink">
            <IconPlus :size="15" />
            <span>{{ activeModal === "legalLinks" ? "新增链接" : "新增入口" }}</span>
          </button>
        </div>
        <div class="config-list">
          <article v-for="(link, index) in linkDraft" :key="`footer-link-editor-${index}`" class="config-row-card footer-link-editor-row">
            <div class="config-form-grid">
              <RouteTargetEditor :model="link" prefix="link" :patient-route-options="patientRouteOptions" include-sort include-enabled />
            </div>
            <div class="config-card-actions">
              <button type="button" class="status-pill" :class="link.enabled === false ? 'disabled' : 'enabled'" @click="toggleDraftEnabled(link)">
                {{ link.enabled === false ? "禁用" : "启用" }}
              </button>
              <button type="button" class="danger-link icon-action" @click="removeDraftLink(index)">
                <IconTrash :size="15" />
                <span>删除</span>
              </button>
            </div>
          </article>
          <div v-if="!linkDraft.length" class="footer-empty-state">
            暂无条目，点击新增后维护入口名称和跳转目标。
          </div>
        </div>
      </div>

      <div class="patient-config-modal-footer">
        <button type="button" class="topbar-refresh" @click="closeModal">取消</button>
        <button type="button" class="quick-btn" @click="saveModal">保存到编辑内容</button>
      </div>
    </section>
  </div>
</template>
