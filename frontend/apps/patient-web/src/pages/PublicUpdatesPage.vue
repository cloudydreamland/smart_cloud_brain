<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { RouterLink, useRoute } from "vue-router";

type UpdateKind = "全部" | "通知公告" | "健康资讯" | "科研动态";
type UpdateItem = {
  kind: Exclude<UpdateKind, "全部">;
  title: string;
  date: string;
  source: string;
  readingTime: string;
  summary: string;
  body: string[];
  tags: string[];
};

const route = useRoute();
const query = ref("");
const kind = ref<UpdateKind>(String(route.name) === "about-news" ? "通知公告" : "健康资讯");
const selected = ref<UpdateItem | null>(null);

const updates: UpdateItem[] = [
  { kind: "通知公告", title: "门诊预约与取消规则提醒", date: "2026-06-20", source: "门诊服务中心", readingTime: "3 分钟", summary: "预约后请按时到院，无法就诊时提前取消，释放号源给其他患者。", body: ["患者可在“我的预约”查看当前挂号状态和就诊时间。", "预约记录会显示医生、科室、就诊时间、状态和取消入口。", "已完成或已取消记录会保留用于复盘就诊安排。", "急症患者请优先急诊，不要等待普通门诊号源。"], tags: ["预约", "号源", "就诊提醒"] },
  { kind: "通知公告", title: "检查检验报告查看说明", date: "2026-06-12", source: "医学检验与影像中心", readingTime: "4 分钟", summary: "报告生成后将按项目、时间和状态归档，复诊时请结合医生建议解读。", body: ["报告异常不等于最终诊断，应结合症状、病历和医生判断。", "复诊前建议整理近期报告和既往检查变化，尤其是连续趋势。", "影像资料如需线下读取，请携带原始文件或可读取介质。", "如报告异常并伴随胸痛、呼吸困难或意识改变，请优先线下急诊。"], tags: ["检查报告", "复诊", "资料归档"] },
  { kind: "通知公告", title: "端午节期间门诊排班与取药窗口安排", date: "2026-06-05", source: "医院办公室", readingTime: "2 分钟", summary: "节日期间普通门诊、急诊、药房和检查窗口服务时间有所调整。", body: ["急诊 24 小时开放，普通门诊按节假日排班运行。", "已预约患者请以患者端“我的预约”和短信通知为准。", "药房取药窗口上午 08:30-12:00、下午 13:30-17:00 开放。", "如需改约，请提前在患者端取消并重新选择号源。"], tags: ["节假日", "门诊排班", "药房"] },
  { kind: "健康资讯", title: "胸痛何时需要立即就医", date: "2026-06-08", source: "心血管内科", readingTime: "5 分钟", summary: "持续胸痛、出汗、气短、晕厥或放射痛应直接急诊，不要等待线上分诊。", body: ["记录胸痛开始时间、性质、持续多久和是否与活动相关。", "既往冠心病、高血压、糖尿病患者应更谨慎。", "压榨样疼痛、出汗、濒死感、放射到肩背或下颌时应直接急诊。", "症状缓解后仍建议按医生要求完成心电图、心肌酶和复诊。"], tags: ["胸痛", "急诊", "心血管"] },
  { kind: "健康资讯", title: "复诊前如何整理用药清单", date: "2026-05-30", source: "药学部", readingTime: "4 分钟", summary: "药名、剂量、频次、漏服情况和不良反应，是医生调整方案的重要依据。", body: ["不要只说“吃过降压药”，应尽量带药盒或处方记录。", "记录最近一次调整药物的时间、原因和调整后的反应。", "保健品、中成药和非处方药也需要告诉医生。", "出现过敏、黑便、明显乏力或呼吸困难时，应及时就医并保留相关信息。"], tags: ["用药", "复诊", "处方"] },
  { kind: "健康资讯", title: "儿童发热就诊前要记录什么", date: "2026-05-22", source: "儿科中心", readingTime: "4 分钟", summary: "体温趋势、精神状态、进食饮水、尿量和既往用药能帮助医生判断风险。", body: ["持续高热、精神反应差、抽搐或脱水表现应尽快线下就医。", "监护人应说明发热开始时间、最高体温和退热药使用情况。", "记录是否咳嗽、腹泻、皮疹、耳痛或接触传染病患者。", "儿童病情变化快，不建议单纯依赖线上咨询。"], tags: ["儿科", "发热", "就诊准备"] },
  { kind: "健康资讯", title: "胃肠镜检查前准备清单", date: "2026-05-18", source: "消化内镜中心", readingTime: "6 分钟", summary: "内镜检查需要按医嘱完成饮食调整、肠道准备、麻醉评估和用药确认。", body: ["检查前请确认是否需要停用抗凝、降糖或铁剂等药物。", "无痛检查需要陪同人员，并按麻醉要求禁食禁饮。", "肠道准备不充分会影响检查质量，必要时需要重新预约。", "检查后如出现剧烈腹痛、发热、黑便或呕血，应立即就医。"], tags: ["内镜", "检查准备", "消化内科"] },
  { kind: "科研动态", title: "智能分诊质量评估项目启动", date: "2026-05-16", source: "临床质量改进办公室", readingTime: "4 分钟", summary: "项目评估推荐科室与医生最终判断的一致性，并持续优化问题设计。", body: ["研究指标包括错挂科室减少、患者资料完整度和就诊效率。", "涉及患者资料的研究应遵循授权、脱敏和伦理审查要求。", "研究结果将反哺患者端分诊问题、医生端接诊提示和管理端质量看板。", "项目不改变医生诊断和处方责任边界。"], tags: ["智能分诊", "科研", "质量改进"] },
  { kind: "科研动态", title: "病历摘要辅助复诊沟通试点", date: "2026-05-06", source: "医学信息化中心", readingTime: "5 分钟", summary: "围绕长期随访患者，探索从既往病历和处方中提取复诊沟通要点。", body: ["摘要用于帮助患者和医生快速回顾关键变化。", "摘要会标注资料来源，避免脱离原始病历。", "所有诊断、治疗和处方判断仍需医生确认。", "试点重点关注患者可理解性、资料可追溯性和复诊沟通效率。"], tags: ["病历摘要", "复诊", "AI 边界"] },
];

const filtered = computed(() => {
  const text = query.value.trim().toLowerCase();
  return updates.filter((item) => {
    const matchKind = kind.value === "全部" || item.kind === kind.value;
    const haystack = `${item.title} ${item.summary} ${item.body.join(" ")} ${item.tags.join(" ")}`.toLowerCase();
    return matchKind && (!text || haystack.includes(text));
  });
});
const pageTitle = computed(() => String(route.name) === "about-news" ? "通知公告" : "健康资讯");
const kinds: UpdateKind[] = ["全部", "通知公告", "健康资讯", "科研动态"];
const featured = computed(() => filtered.value[0] || updates[0]);
const metrics = computed(() => [
  { value: String(updates.filter((item) => item.kind === "通知公告").length), label: "公告", caption: "门诊、检查、系统和窗口安排" },
  { value: String(updates.filter((item) => item.kind === "健康资讯").length), label: "健康资讯", caption: "按真实就诊场景整理患者教育" },
  { value: String(updates.filter((item) => item.kind === "科研动态").length), label: "科研动态", caption: "质量改进、AI 边界和教学项目" },
]);
const topicCards = [
  { title: "就诊提醒", text: "预约、取消、签到、排班和节假日服务安排。", tag: "预约" },
  { title: "检查准备", text: "检验、影像、内镜、超声等检查前后注意事项。", tag: "检查报告" },
  { title: "用药安全", text: "处方理解、漏服处理、不良反应和特殊人群用药。", tag: "用药" },
  { title: "AI 与科研", text: "智能分诊、病历摘要和医疗质量改进项目。", tag: "智能分诊" },
];

watch(() => route.name, (name) => {
  kind.value = String(name) === "about-news" ? "通知公告" : "健康资讯";
});
</script>

<template>
  <main class="public-info-page">
    <header class="resource-header">
      <RouterLink class="scb-brand" :to="{ name: 'patient-home' }" aria-label="智慧云脑首页">
        <span>智慧<br />云脑</span>
        <i></i><i></i>
      </RouterLink>
      <nav aria-label="当前位置">
        <RouterLink :to="{ name: 'patient-home' }">首页</RouterLink>
        <span>/</span>
        <span>{{ pageTitle }}</span>
      </nav>
      <p class="resource-label">资讯中心</p>
      <h1>{{ pageTitle }}</h1>
      <p>集中查看医院公告、服务更新、患者健康资讯和科研教育动态。内容用于就医准备和健康教育，不替代医生诊断。</p>
      <div class="resource-metrics">
        <article v-for="item in metrics" :key="item.label">
          <strong>{{ item.value }}</strong>
          <span>{{ item.label }}</span>
          <p>{{ item.caption }}</p>
        </article>
      </div>
      <form class="resource-search-form" @submit.prevent>
        <span class="search-symbol" aria-hidden="true"></span>
        <input v-model.trim="query" type="search" placeholder="搜索公告、资讯或关键词">
        <button type="button" @click="query = ''">清空</button>
      </form>
    </header>

    <section class="resource-search-page">
      <article class="update-featured">
        <div>
          <span>{{ featured.kind }} · {{ featured.source }} · {{ featured.readingTime }}</span>
          <h2>{{ featured.title }}</h2>
          <p>{{ featured.summary }}</p>
          <button type="button" @click="selected = featured">阅读头条</button>
        </div>
        <aside>
          <strong>本周服务提醒</strong>
          <p>完成预约后请在“我的预约”核对时间；检查前准备以医生申请单和检查科室说明为准。</p>
          <RouterLink :to="{ name: 'public-guide' }">查看就诊指南</RouterLink>
        </aside>
      </article>

      <div class="update-topic-grid">
        <button v-for="item in topicCards" :key="item.title" type="button" @click="query = item.tag">
          <strong>{{ item.title }}</strong>
          <span>{{ item.text }}</span>
        </button>
      </div>

      <div class="update-filter" role="tablist" aria-label="资讯分类">
        <button v-for="item in kinds" :key="item" type="button" :class="{ active: kind === item }" @click="kind = item">{{ item }}</button>
      </div>
      <div class="update-list">
        <article v-for="item in filtered" :key="`${item.kind}-${item.title}`" class="update-card">
          <span>{{ item.kind }} · {{ item.date }} · {{ item.source }}</span>
          <h2>{{ item.title }}</h2>
          <p>{{ item.summary }}</p>
          <div>
            <em v-for="tag in item.tags" :key="tag">{{ tag }}</em>
          </div>
          <button type="button" @click="selected = item">查看详情</button>
        </article>
      </div>
      <p v-if="!filtered.length" class="public-empty">没有匹配内容。可切换分类或减少关键词。</p>

      <div v-if="selected" class="public-detail-panel" role="dialog" aria-modal="true">
        <button type="button" aria-label="关闭" @click="selected = null">×</button>
        <p>{{ selected.kind }} · {{ selected.date }} · {{ selected.source }} · {{ selected.readingTime }}</p>
        <h2>{{ selected.title }}</h2>
        <span>{{ selected.summary }}</span>
        <ul>
          <li v-for="line in selected.body" :key="line">{{ line }}</li>
        </ul>
        <div class="update-detail-actions">
          <RouterLink :to="{ name: 'public-search', query: { q: selected.tags[0] } }">搜索相关资料</RouterLink>
          <RouterLink :to="{ name: 'patient-doctors' }">预约相关门诊</RouterLink>
        </div>
      </div>
    </section>
  </main>
</template>
