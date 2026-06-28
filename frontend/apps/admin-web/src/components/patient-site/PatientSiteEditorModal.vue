<script setup lang="ts">
import { computed } from "vue";
import type { RouteTargetConfig } from "@smart-cloud-brain/shared-api";
import type { EditingTarget, homeModuleTypeOptions } from "../../composables/usePatientSiteConfigEditor";
import { ScbSelect } from "@smart-cloud-brain/shared-ui";
import { patientSiteFieldLabel } from "../../patientSitePresentation";
import OssImageUploadField from "./OssImageUploadField.vue";
import RouteTargetEditor from "./RouteTargetEditor.vue";

type Draft = any;

const props = defineProps<{
  title: string;
  description: string;
  editingTarget: EditingTarget;
  editingDraft: Draft;
  patientRouteOptions: readonly { name: string; label: string }[];
  homeModuleTypeOptions: typeof homeModuleTypeOptions;
  closeEditor: () => void;
  applyEditor: () => void;
  hydrateEditingHomeModuleContent: () => void;
  editingContentItems: () => RouteTargetConfig[];
  addEditingLink: () => void;
  addEditingQuickAction: () => void;
  addEditingPoint: () => void;
  addEditingLocationItem: () => void;
  addEditingDepartmentLink: () => void;
  addEditingFallbackName: () => void;
}>();

const routeSelectOptions = computed(() =>
  props.patientRouteOptions.map((r) => ({ value: r.name, label: r.label }))
);
const homeModuleTypeSelectOptions = computed(() =>
  props.homeModuleTypeOptions.map((t) => ({ value: t.value, label: t.label }))
);
</script>

<template>
  <div class="patient-config-modal-backdrop" @click.self="closeEditor">
    <section class="patient-config-modal-card" role="dialog" aria-modal="true" :aria-label="title">
      <button type="button" class="patient-config-modal-close" aria-label="关闭" @click="closeEditor">×</button>
      <header class="patient-config-modal-head">
        <h2>{{ title }}</h2>
        <p>{{ description }}</p>
      </header>

      <div class="patient-config-modal">
        <template v-if="editingTarget.type === 'brand'">
          <div class="config-grid two">
            <label><span>品牌名称</span><input v-model.trim="editingDraft.name" type="text"></label>
            <label>
              <span>首页入口</span>
              <ScbSelect v-model="editingDraft.homeRoute" :options="routeSelectOptions" close-on-scroll />
            </label>
          </div>
          <OssImageUploadField
            :image-url="editingDraft.logoUrl"
            :image-alt="editingDraft.logoAlt"
            :object-key="editingDraft.logoObjectKey"
            label="品牌 Logo"
            alt-label="Logo 说明"
            @uploaded="({ url, objectKey }) => { editingDraft.logoUrl = url; editingDraft.logoObjectKey = objectKey; }"
            @update:image-alt="editingDraft.logoAlt = $event"
            @cleared="() => { editingDraft.logoUrl = ''; editingDraft.logoObjectKey = ''; editingDraft.logoAlt = ''; }"
          />
        </template>

        <template v-else-if="editingTarget.type === 'nav-menu'">
          <div class="config-grid four">
            <label><span>{{ patientSiteFieldLabel("key") }}</span><input v-model.trim="editingDraft.key" type="text"></label>
            <label><span>{{ patientSiteFieldLabel("label") }}</span><input v-model.trim="editingDraft.label" type="text"></label>
            <label><span>{{ patientSiteFieldLabel("sort") }}</span><input v-model.number="editingDraft.sort" type="number"></label>
            <label class="check-field"><input v-model="editingDraft.enabled" type="checkbox"><span>{{ patientSiteFieldLabel("enabled") }}</span></label>
          </div>
          <div class="config-grid two">
            <label><span>导语</span><input v-model.trim="editingDraft.lead" type="text"></label>
            <label><span>说明</span><input v-model.trim="editingDraft.description" type="text"></label>
          </div>
          <div class="nested-list">
            <div class="nested-list-head">
              <strong>菜单链接</strong>
              <button type="button" class="topbar-refresh" @click="addEditingLink">新增链接</button>
            </div>
            <div v-for="(link, linkIndex) in editingDraft.links" :key="`editing-link-${linkIndex}`" class="config-row-card">
              <div class="config-grid five">
                <RouteTargetEditor :model="link" prefix="link" :patient-route-options="patientRouteOptions" include-sort include-enabled />
                <label><span>说明</span><input v-model.trim="link.description" type="text"></label>
              </div>
              <button type="button" class="danger-link" @click="editingDraft.links[linkIndex].enabled = false">删除</button>
            </div>
          </div>
          <div class="nested-list">
            <div class="nested-list-head">
              <strong>特色入口</strong>
              <button v-if="!editingDraft.feature" type="button" class="topbar-refresh" @click="editingDraft.feature = { label: '特色入口', routeName: 'patient-home', enabled: true, sort: 0 }">添加入口</button>
              <button v-else type="button" class="danger-link" @click="editingDraft.feature.enabled = false">删除入口</button>
            </div>
            <div v-if="editingDraft.feature" class="config-grid three">
              <RouteTargetEditor :model="editingDraft.feature" prefix="feature" :patient-route-options="patientRouteOptions" include-sort />
            </div>
          </div>
        </template>

        <template v-else-if="editingTarget.type === 'user-link'">
          <div class="config-grid four">
            <RouteTargetEditor :model="editingDraft" prefix="userLink" :patient-route-options="patientRouteOptions" include-sort include-enabled />
          </div>
        </template>

        <template v-else-if="editingTarget.type === 'home-hero'">
          <div class="config-grid two">
            <label><span>横幅眉题</span><input v-model.trim="editingDraft.eyebrow" type="text"></label>
            <label><span>横幅标题</span><input v-model.trim="editingDraft.title" type="text"></label>
            <label class="check-field"><input v-model="editingDraft.enabled" type="checkbox"><span>{{ patientSiteFieldLabel("enabled") }}</span></label>
          </div>
          <div class="config-grid two">
            <RouteTargetEditor :model="editingDraft.primaryAction" prefix="primaryAction" :patient-route-options="patientRouteOptions" />
            <RouteTargetEditor :model="editingDraft.secondaryAction" prefix="secondaryAction" :patient-route-options="patientRouteOptions" />
          </div>
          <OssImageUploadField
            :image-url="editingDraft.backgroundImageUrl"
            :image-alt="editingDraft.backgroundImageAlt"
            :object-key="editingDraft.backgroundObjectKey"
            label="首页 Hero 背景图"
            alt-label="背景图说明"
            @uploaded="({ url, objectKey }) => { editingDraft.backgroundImageUrl = url; editingDraft.backgroundObjectKey = objectKey; }"
            @update:image-alt="editingDraft.backgroundImageAlt = $event"
            @cleared="() => { editingDraft.backgroundImageUrl = ''; editingDraft.backgroundObjectKey = ''; editingDraft.backgroundImageAlt = ''; }"
          />
        </template>

        <template v-else-if="editingTarget.type === 'home-module'">
          <div class="config-grid four">
            <label>
              <span>{{ patientSiteFieldLabel("type") }}</span>
              <ScbSelect v-model="editingDraft.type" :options="homeModuleTypeSelectOptions" close-on-scroll @update:modelValue="hydrateEditingHomeModuleContent" />
            </label>
            <label><span>{{ patientSiteFieldLabel("key") }}</span><input v-model.trim="editingDraft.key" type="text"></label>
            <label><span>{{ patientSiteFieldLabel("sort") }}</span><input v-model.number="editingDraft.sort" type="number"></label>
            <label class="check-field"><input v-model="editingDraft.enabled" type="checkbox"><span>{{ patientSiteFieldLabel("enabled") }}</span></label>
          </div>
          <div v-if="editingDraft.type === 'notice'" class="config-grid two">
            <label><span>提示级别</span><input v-model.trim="editingDraft.content.level" type="text"></label>
            <label><span>提示正文</span><input v-model.trim="editingDraft.content.text" type="text"></label>
          </div>
          <div v-else-if="editingDraft.type === 'quick_actions'" class="nested-list">
            <div class="nested-list-head">
              <strong>快捷入口</strong>
              <button type="button" class="topbar-refresh" @click="addEditingQuickAction">新增快捷入口</button>
            </div>
            <div v-for="(item, itemIndex) in editingContentItems()" :key="`editing-action-${itemIndex}`" class="config-row-card">
              <div class="config-grid four">
                <RouteTargetEditor :model="item" prefix="item" :patient-route-options="patientRouteOptions" include-sort include-enabled />
              </div>
              <button type="button" class="danger-link" @click="editingContentItems()[itemIndex].enabled = false">删除</button>
            </div>
          </div>
          <div v-else class="nested-list">
            <div class="config-grid two">
              <label><span>内容标题</span><input v-model.trim="editingDraft.content.title" type="text"></label>
              <label><span>内容说明</span><input v-model.trim="editingDraft.content.description" type="text"></label>
              <label><span>正文内容</span><input v-model.trim="editingDraft.content.text" type="text"></label>
            </div>
            <OssImageUploadField
              v-if="editingDraft.type === 'intro' || editingDraft.type === 'static_content'"
              :image-url="editingDraft.content.imageUrl"
              :image-alt="editingDraft.content.imageAlt"
              :object-key="editingDraft.content.imageObjectKey"
              label="模块图片"
              @uploaded="({ url, objectKey }) => { editingDraft.content.imageUrl = url; editingDraft.content.imageObjectKey = objectKey; }"
              @update:image-alt="editingDraft.content.imageAlt = $event"
              @cleared="() => { editingDraft.content.imageUrl = ''; editingDraft.content.imageObjectKey = ''; editingDraft.content.imageAlt = ''; }"
            />
            <div v-if="editingDraft.content.action" class="nested-list">
              <div class="nested-list-head"><strong>操作入口</strong></div>
              <div class="config-grid two">
                <RouteTargetEditor :model="editingDraft.content.action" prefix="action" :patient-route-options="patientRouteOptions" />
              </div>
            </div>
            <div v-if="editingDraft.type === 'locations'" class="nested-list">
              <div class="nested-list-head">
                <strong>院区列表</strong>
                <button type="button" class="topbar-refresh" @click="addEditingLocationItem">新增院区</button>
              </div>
              <div v-for="(item, itemIndex) in editingDraft.content.items" :key="`editing-location-${itemIndex}`" class="config-row-card">
                <div class="config-grid two">
                  <label><span>院区名称</span><input v-model.trim="item.title" type="text"></label>
                  <label><span>辅助信息</span><input v-model.trim="item.meta" type="text"></label>
                </div>
                <OssImageUploadField
                  :image-url="item.imageUrl"
                  :image-alt="item.alt"
                  :object-key="item.imageObjectKey"
                  label="院区图片"
                  @uploaded="({ url, objectKey }) => { item.imageUrl = url; item.imageObjectKey = objectKey; }"
                  @update:image-alt="item.alt = $event"
                  @cleared="() => { item.imageUrl = ''; item.imageObjectKey = ''; item.alt = ''; }"
                />
                <button type="button" class="danger-link" @click="editingDraft.content.items.splice(itemIndex, 1)">删除</button>
              </div>
            </div>
            <div v-if="editingDraft.type === 'featured_departments'" class="nested-list">
              <div class="nested-list-head">
                <strong>备用入口</strong>
                <button type="button" class="topbar-refresh" @click="addEditingDepartmentLink">新增入口</button>
              </div>
              <div v-for="(item, itemIndex) in editingDraft.content.items" :key="`editing-dept-${itemIndex}`" class="config-row-card">
                <div class="config-grid four">
                  <RouteTargetEditor :model="item" prefix="item" :patient-route-options="patientRouteOptions" include-sort include-enabled />
                </div>
                <button type="button" class="danger-link" @click="editingDraft.content.items.splice(itemIndex, 1)">删除</button>
              </div>
              <div class="nested-list-head">
                <strong>默认科室名</strong>
                <button type="button" class="topbar-refresh" @click="addEditingFallbackName">新增科室名</button>
              </div>
              <div v-for="(_name, nameIndex) in editingDraft.content.fallbackNames" :key="`editing-fallback-${nameIndex}`" class="config-row-card">
                <label><span>科室名</span><input v-model.trim="editingDraft.content.fallbackNames[nameIndex]" type="text"></label>
                <button type="button" class="danger-link" @click="editingDraft.content.fallbackNames.splice(nameIndex, 1)">删除</button>
              </div>
            </div>
          </div>
        </template>

        <template v-else-if="editingTarget.type === 'static-page'">
          <div class="config-grid four">
            <label>
              <span>{{ patientSiteFieldLabel("routeName") }}</span>
              <ScbSelect v-model="editingDraft.routeName" :options="routeSelectOptions" close-on-scroll />
            </label>
            <label><span>{{ patientSiteFieldLabel("sort") }}</span><input v-model.number="editingDraft.sort" type="number"></label>
            <label><span>{{ patientSiteFieldLabel("label") }}</span><input v-model.trim="editingDraft.label" type="text"></label>
            <label class="check-field"><input v-model="editingDraft.enabled" type="checkbox"><span>{{ patientSiteFieldLabel("enabled") }}</span></label>
          </div>
          <div class="config-grid two">
            <label><span>{{ patientSiteFieldLabel("title") }}</span><input v-model.trim="editingDraft.title" type="text"></label>
            <label><span>页面简介</span><input v-model.trim="editingDraft.intro" type="text"></label>
          </div>
          <div class="nested-list">
            <div class="nested-list-head">
              <strong>页面要点</strong>
              <button type="button" class="topbar-refresh" @click="addEditingPoint">新增要点</button>
            </div>
            <div v-for="(point, pointIndex) in editingDraft.points" :key="`editing-point-${pointIndex}`" class="config-row-card">
              <div class="config-grid two">
                <label><span>要点标题</span><input v-model.trim="point.title" type="text"></label>
                <label><span>要点说明</span><input v-model.trim="point.text" type="text"></label>
              </div>
              <button type="button" class="danger-link" @click="editingDraft.points.splice(pointIndex, 1)">删除</button>
            </div>
          </div>
          <div class="nested-list">
            <div class="nested-list-head">
              <strong>主按钮</strong>
              <button v-if="!editingDraft.primary" type="button" class="topbar-refresh" @click="editingDraft.primary = { label: '返回首页', routeName: 'patient-home' }">添加主按钮</button>
              <button v-else type="button" class="danger-link" @click="editingDraft.primary.enabled = false">删除主按钮</button>
            </div>
            <div v-if="editingDraft.primary" class="config-grid two">
              <RouteTargetEditor :model="editingDraft.primary" prefix="primary" :patient-route-options="patientRouteOptions" />
            </div>
          </div>
        </template>
      </div>

      <div class="patient-config-modal-footer">
        <button type="button" class="topbar-refresh" @click="closeEditor">取消</button>
        <button type="button" class="quick-btn" @click="applyEditor">保存到编辑内容</button>
      </div>
    </section>
  </div>
</template>
