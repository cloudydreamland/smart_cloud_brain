<script setup lang="ts">
import { computed, nextTick, ref } from "vue";
import { IconMapPin, IconPencil, IconPlus, IconTrash } from "@tabler/icons-vue";
import type { PatientHospitalInfoConfig, PatientHospitalLocationConfig } from "@smart-cloud-brain/shared-api";
import OssImageUploadField from "./OssImageUploadField.vue";

const props = defineProps<{
  hospitalDraft: PatientHospitalInfoConfig;
  toggleEnabled: (item: { enabled?: boolean }) => void;
}>();

type HospitalModal = "base" | "location";
type HospitalBaseDraft = Pick<PatientHospitalInfoConfig, "name" | "intro" | "address" | "phone" | "workHours" | "website">;

const activeModal = ref<HospitalModal | null>(null);
const editingLocationIndex = ref<number | null>(null);
const modalCard = ref<HTMLElement>();
const baseDraft = ref<HospitalBaseDraft>(emptyBaseDraft());
const locationDraft = ref<PatientHospitalLocationConfig>(emptyLocationDraft());

const enabledLocations = computed(() => props.hospitalDraft.locations.filter((location) => location.enabled !== false).length);
const disabledLocations = computed(() => props.hospitalDraft.locations.length - enabledLocations.value);
const hospitalSummary = computed(() => props.hospitalDraft.intro || "还没有填写医院简介。");
const modalTitle = computed(() => {
  if (activeModal.value === "base") return "编辑医院基础信息";
  return editingLocationIndex.value === null ? "新增院区信息" : "编辑院区信息";
});
const modalDescription = computed(() => {
  if (activeModal.value === "base") return "维护患者端展示的医院名称、简介、联系电话、门诊时间、官网和地址。";
  return "维护单个院区的名称、地址、电话、图片、排序和启用状态。";
});

function emptyBaseDraft(): HospitalBaseDraft {
  return { name: "", intro: "", address: "", phone: "", workHours: "", website: "" };
}

function emptyLocationDraft(): PatientHospitalLocationConfig {
  return {
    name: "新院区",
    address: "",
    phone: "",
    imageUrl: "",
    imageObjectKey: "",
    sort: nextSort(),
    enabled: true,
  };
}

function focusModal() {
  nextTick(() => modalCard.value?.focus());
}

function openBaseEditor() {
  baseDraft.value = {
    name: props.hospitalDraft.name,
    intro: props.hospitalDraft.intro,
    address: props.hospitalDraft.address,
    phone: props.hospitalDraft.phone,
    workHours: props.hospitalDraft.workHours,
    website: props.hospitalDraft.website,
  };
  activeModal.value = "base";
  focusModal();
}

function addLocation() {
  editingLocationIndex.value = null;
  locationDraft.value = emptyLocationDraft();
  activeModal.value = "location";
  focusModal();
}

function openLocationEditor(index: number) {
  const location = props.hospitalDraft.locations[index];
  if (!location) return;
  editingLocationIndex.value = index;
  locationDraft.value = { ...location };
  activeModal.value = "location";
  focusModal();
}

function closeModal() {
  activeModal.value = null;
  editingLocationIndex.value = null;
}

function saveModal() {
  if (activeModal.value === "base") {
    Object.assign(props.hospitalDraft, baseDraft.value);
  } else if (activeModal.value === "location") {
    if (editingLocationIndex.value === null) {
      props.hospitalDraft.locations.push({ ...locationDraft.value });
    } else {
      Object.assign(props.hospitalDraft.locations[editingLocationIndex.value], locationDraft.value);
    }
  }
  closeModal();
}

function removeLocation(index: number) {
  const location = props.hospitalDraft.locations[index];
  if (!location || !window.confirm(`确认删除院区「${location.name || "未命名院区"}」？删除后只会影响当前编辑稿，发布或保存并生效后才会更新正式页面。`)) return;
  location.enabled = false;
}

function nextSort() {
  return props.hospitalDraft.locations.reduce((value, item) => Math.max(value, item.sort || 0), 0) + 10;
}

function configuredText(value?: string) {
  return value ? "已配置" : "未配置";
}
</script>

<template>
  <section class="hospital-overview-grid" aria-label="医院信息卡片">
    <article class="config-card hospital-info-card hospital-info-card-main">
      <div class="hospital-card-head">
        <div>
          <span class="hospital-card-kicker">医院基础信息</span>
          <h3>{{ hospitalDraft.name || "未设置医院名称" }}</h3>
          <p>{{ hospitalSummary }}</p>
        </div>
        <button type="button" class="topbar-refresh icon-action" aria-label="编辑医院基础信息" @click="openBaseEditor">
          <IconPencil :size="15" />
          <span>编辑</span>
        </button>
      </div>
      <dl class="hospital-card-meta">
        <div>
          <dt>联系电话</dt>
          <dd>{{ configuredText(hospitalDraft.phone) }}</dd>
        </div>
        <div>
          <dt>门诊时间</dt>
          <dd>{{ configuredText(hospitalDraft.workHours) }}</dd>
        </div>
        <div>
          <dt>官网地址</dt>
          <dd>{{ configuredText(hospitalDraft.website) }}</dd>
        </div>
        <div>
          <dt>医院地址</dt>
          <dd>{{ configuredText(hospitalDraft.address) }}</dd>
        </div>
      </dl>
    </article>

    <article class="config-card hospital-info-card">
      <div class="hospital-card-head">
        <div>
          <span class="hospital-card-kicker">院区概览</span>
          <h3>{{ enabledLocations }} 个启用院区</h3>
          <p v-if="disabledLocations">{{ disabledLocations }} 个已禁用院区保留在当前编辑稿。</p>
          <p v-else>院区信息以卡片摘要展示，详细内容在弹窗中维护。</p>
        </div>
        <button type="button" class="topbar-refresh icon-action" @click="addLocation">
          <IconPlus :size="15" />
          <span>新增院区</span>
        </button>
      </div>
    </article>
  </section>

  <section class="config-section">
    <div class="config-section-head">
      <div>
        <h3>院区卡片</h3>
        <p>页面只展示必要摘要，点击编辑查看和维护详细信息。</p>
      </div>
    </div>
    <div class="hospital-location-grid">
      <article
        v-for="(location, index) in hospitalDraft.locations"
        :key="`${location.name}-${index}`"
        class="config-card hospital-location-card"
        :class="{ disabled: location.enabled === false }"
      >
        <div class="hospital-location-thumb" aria-hidden="true">
          <img v-if="location.imageUrl" :src="location.imageUrl" :alt="location.name || '院区图片'">
          <IconMapPin v-else :size="24" />
        </div>
        <div class="hospital-location-body">
          <div class="hospital-location-title">
            <div>
              <strong>{{ location.name || "未命名院区" }}</strong>
              <span>排序 {{ location.sort ?? "-" }}</span>
            </div>
            <button type="button" class="status-pill" :class="location.enabled === false ? 'disabled' : 'enabled'" @click="toggleEnabled(location)">
              {{ location.enabled === false ? "禁用" : "启用" }}
            </button>
          </div>
          <p>{{ configuredText(location.address) }}院区地址</p>
          <small>{{ configuredText(location.phone) }}院区电话</small>
        </div>
        <div class="config-card-actions">
          <button type="button" class="topbar-refresh icon-action" @click="openLocationEditor(index)">
            <IconPencil :size="15" />
            <span>编辑</span>
          </button>
          <button type="button" class="danger-link icon-action" @click="removeLocation(index)">
            <IconTrash :size="15" />
            <span>删除</span>
          </button>
        </div>
      </article>
      <div v-if="!hospitalDraft.locations.length" class="hospital-empty-card">
        暂无院区信息，点击新增后维护院区名称、地址和图片。
      </div>
    </div>
  </section>

  <div v-if="activeModal" class="patient-config-modal-backdrop" @click.self="closeModal">
    <section
      ref="modalCard"
      class="patient-config-modal-card hospital-editor-modal-card"
      role="dialog"
      aria-modal="true"
      :aria-label="modalTitle"
      tabindex="-1"
      @keydown.escape.prevent="closeModal"
    >
      <button type="button" class="patient-config-modal-close" aria-label="关闭" @click="closeModal">×</button>
      <header class="patient-config-modal-head">
        <h2>{{ modalTitle }}</h2>
        <p>{{ modalDescription }}</p>
      </header>

      <div v-if="activeModal === 'base'" class="patient-config-modal">
        <div class="config-form-grid">
          <label><span>医院名称</span><input v-model.trim="baseDraft.name" type="text"></label>
          <label><span>联系电话</span><input v-model.trim="baseDraft.phone" type="text"></label>
          <label><span>门诊时间</span><input v-model.trim="baseDraft.workHours" type="text"></label>
          <label><span>官网地址</span><input v-model.trim="baseDraft.website" type="text"></label>
          <label class="span-2"><span>医院地址</span><input v-model.trim="baseDraft.address" type="text"></label>
          <label class="span-2"><span>医院简介</span><textarea v-model.trim="baseDraft.intro" rows="5"></textarea></label>
        </div>
      </div>

      <div v-else class="patient-config-modal">
        <div class="config-form-grid">
          <label><span>院区名称</span><input v-model.trim="locationDraft.name" type="text"></label>
          <label><span>排序</span><input v-model.number="locationDraft.sort" type="number"></label>
          <label><span>院区电话</span><input v-model.trim="locationDraft.phone" type="text"></label>
          <label><span>启用状态</span>
            <button type="button" class="status-pill" :class="locationDraft.enabled === false ? 'disabled' : 'enabled'" @click="locationDraft.enabled = locationDraft.enabled === false">
              {{ locationDraft.enabled === false ? "禁用" : "启用" }}
            </button>
          </label>
          <label class="span-2"><span>院区地址</span><input v-model.trim="locationDraft.address" type="text"></label>
          <OssImageUploadField
            v-model:image-url="locationDraft.imageUrl"
            v-model:object-key="locationDraft.imageObjectKey"
            class="span-2"
            label="院区图片"
          />
        </div>
      </div>

      <div class="patient-config-modal-footer">
        <button type="button" class="topbar-refresh" @click="closeModal">取消</button>
        <button type="button" class="quick-btn" @click="saveModal">保存到编辑内容</button>
      </div>
    </section>
  </div>
</template>
