<script setup lang="ts">
import { computed, nextTick, ref, watch } from "vue";
import { RouterLink, useRoute } from "vue-router";

type UpdateKind = "全部" | "通知公告" | "健康资讯" | "科研动态";
type UpdateItem = {
  kind: Exclude<UpdateKind, "全部">;
  title: string;
  date: string;
  source: string;
  readingTime: string;
  summary: string;
  lead: string;
  body: string[];
  tags: string[];
};

const route = useRoute();
const query = ref("");
const kind = ref<UpdateKind>(String(route.name) === "about-news" ? "通知公告" : "健康资讯");
const selected = ref<UpdateItem | null>(null);
const drawerEl = ref<HTMLDivElement | null>(null);
watch(selected, (value) => {
  if (value) nextTick(() => drawerEl.value?.focus());
});

const updates: UpdateItem[] = [
  { kind: "通知公告", title: "门诊预约与取消规则提醒", date: "2026-06-20", source: "门诊服务中心", readingTime: "3 分钟", summary: "预约后请按时到院，无法就诊时提前取消，释放号源给其他患者。", lead: "门诊号源是有限医疗资源。为了让患者能够更稳定地安排就诊，平台将预约确认、取消规则、到院时间和诊后资料入口集中在患者服务中展示。", body: ["患者完成预约后，应第一时间进入“我的预约”核对科室、医生、就诊时间和状态。如果发现时间不合适，应在就诊前尽早取消，不要等到当天才处理。", "预约记录会显示医生、科室、就诊时间、状态和取消入口。已完成或已取消记录仍会保留，便于患者复盘就诊安排，也便于后续查看对应病历、处方和报告线索。", "普通门诊不适合处理胸痛、呼吸困难、意识改变、单侧肢体无力、大量出血等急症。出现这些情况时，应直接前往急诊或拨打急救电话，不要等待普通门诊号源。"], tags: ["预约", "号源", "就诊提醒"] },
  { kind: "通知公告", title: "检查检验报告查看说明", date: "2026-06-12", source: "医学检验与影像中心", readingTime: "4 分钟", summary: "报告生成后将按项目、时间和状态归档，复诊时请结合医生建议解读。", lead: "检查报告是医生判断病情的重要资料，但报告本身并不等于诊断。患者端报告入口的重点，是帮助患者归档检查线索，并在复诊前准备好需要医生解读的问题。", body: ["报告异常不等于最终诊断，应结合症状、病历、既往结果和医生判断。对于慢病患者，连续趋势往往比单次异常更重要。", "复诊前建议整理近期报告和既往检查变化，尤其是影像对比、检验指标趋势和医生上次医嘱。影像资料如需线下读取，请携带原始文件或可读取介质。", "如报告异常并伴随胸痛、呼吸困难、意识改变、持续高热或明显出血等症状，请优先线下急诊，不要等待线上报告解释。"], tags: ["检查报告", "复诊", "资料归档"] },
  { kind: "通知公告", title: "端午节期间门诊排班与取药窗口安排", date: "2026-06-05", source: "医院办公室", readingTime: "2 分钟", summary: "节日期间普通门诊、急诊、药房和检查窗口服务时间有所调整。", lead: "节假日期间，普通门诊、检查窗口和药房取药时间可能与工作日不同。患者已完成预约的，应以患者端记录和短信通知为准。", body: ["急诊 24 小时开放，普通门诊按节假日排班运行。患者如果症状紧急，不应因为节假日门诊减少而等待，应直接进入急诊流程。", "药房取药窗口上午 08:30-12:00、下午 13:30-17:00 开放。慢病长期用药患者建议提前核对药量，避免节假日期间临时断药。", "如需改约，请提前在患者端取消并重新选择号源。取消后原号源可能被其他患者预约，请谨慎处理。"], tags: ["节假日", "门诊排班", "药房"] },
  { kind: "健康资讯", title: "胸痛何时需要立即就医", date: "2026-06-08", source: "心血管内科", readingTime: "5 分钟", summary: "持续胸痛、出汗、气短、晕厥或放射痛应直接急诊，不要等待线上分诊。", lead: "胸痛是患者最容易犹豫的症状之一。有些胸痛来自肌肉、消化或焦虑，但也可能提示急性心血管事件。判断胸痛时，关键不是先猜病名，而是识别危险信号。", body: ["记录胸痛开始时间、性质、持续多久和是否与活动相关。如果疼痛呈压榨样，伴随出汗、濒死感、气短、晕厥，或放射到肩背、左臂、下颌，应直接急诊。", "既往冠心病、高血压、糖尿病、吸烟史或家族心血管病史的患者，应更谨慎处理胸痛。即使症状缓解，也建议按医生要求完成心电图、心肌酶和后续复诊。", "线上分诊和健康文章不能排除急性心梗、肺栓塞等严重情况。只要胸痛持续、反复或伴随全身不适，就不要等待普通门诊预约。"], tags: ["胸痛", "急诊", "心血管"] },
  { kind: "健康资讯", title: "复诊前如何整理用药清单", date: "2026-05-30", source: "药学部", readingTime: "4 分钟", summary: "药名、剂量、频次、漏服情况和不良反应，是医生调整方案的重要依据。", lead: "复诊时，医生不仅需要知道患者“吃过什么药”，更需要知道药物是否按时使用、有没有漏服、有没有不良反应，以及是否同时服用了保健品或非处方药。", body: ["不要只说“吃过降压药”，应尽量带药盒、处方记录或药品照片，并写清药名、剂量、频次和开始时间。", "记录最近一次调整药物的时间、原因和调整后的反应。慢病患者还应带上血压、血糖、心率或其他监测指标的连续趋势。", "保健品、中成药和非处方药也需要告诉医生。出现过敏、黑便、明显乏力、呼吸困难或严重皮疹时，应及时就医并保留相关信息。"], tags: ["用药", "复诊", "处方"] },
  { kind: "健康资讯", title: "儿童发热就诊前要记录什么", date: "2026-05-22", source: "儿科中心", readingTime: "4 分钟", summary: "体温趋势、精神状态、进食饮水、尿量和既往用药能帮助医生判断风险。", lead: "儿童发热不是只看体温数字。医生更关心孩子精神状态、进食饮水、尿量、皮疹、呼吸情况和退热药反应。监护人记录得越清楚，就诊沟通越高效。", body: ["持续高热、精神反应差、抽搐、呼吸费力、尿量明显减少或脱水表现，应尽快线下就医。婴幼儿和基础疾病儿童需要更谨慎。", "监护人应说明发热开始时间、最高体温、测量方式、退热药名称和用药后反应。还应记录是否咳嗽、腹泻、皮疹、耳痛或接触传染病患者。", "儿童病情变化快，不建议单纯依赖线上咨询。到院时建议由熟悉病情的监护人陪同，并携带疫苗本、既往病历和近期检查结果。"], tags: ["儿科", "发热", "就诊准备"] },
  { kind: "健康资讯", title: "胃肠镜检查前准备清单", date: "2026-05-18", source: "消化内镜中心", readingTime: "6 分钟", summary: "内镜检查需要按医嘱完成饮食调整、肠道准备、麻醉评估和用药确认。", lead: "胃肠镜检查质量很大程度取决于检查前准备。准备不充分不仅影响观察结果，还可能导致重新预约，增加患者等待时间。", body: ["检查前请确认是否需要停用抗凝、降糖、铁剂或其他影响检查的药物。不要自行停药，应按医生或检查科室要求处理。", "无痛检查需要陪同人员，并按麻醉要求禁食禁饮。肠道准备不充分会影响检查质量，必要时需要重新预约。", "检查后如出现剧烈腹痛、持续发热、黑便、呕血或明显头晕，应立即就医。报告解读应结合医生病历和病理结果。"], tags: ["内镜", "检查准备", "消化内科"] },
  { kind: "科研动态", title: "智能分诊质量评估项目启动", date: "2026-05-16", source: "临床质量改进办公室", readingTime: "4 分钟", summary: "项目评估推荐科室与医生最终判断的一致性，并持续优化问题设计。", lead: "智能分诊的价值不在于替代医生，而在于帮助患者更完整地描述症状，并减少错挂科室。质量评估项目会持续观察系统推荐是否真正改善就医流程。", body: ["研究指标包括错挂科室减少、患者资料完整度、医生重复追问减少和就诊效率提升。项目会比较推荐科室与医生最终判断的一致性。", "涉及患者资料的研究应遵循授权、脱敏和伦理审查要求。用于质量改进的数据不会直接暴露个人身份信息。", "研究结果将反哺患者端分诊问题、医生端接诊提示和管理端质量看板。项目不改变医生诊断和处方责任边界。"], tags: ["智能分诊", "科研", "质量改进"] },
  { kind: "科研动态", title: "病历摘要辅助复诊沟通试点", date: "2026-05-06", source: "医学信息化中心", readingTime: "5 分钟", summary: "围绕长期随访患者，探索从既往病历和处方中提取复诊沟通要点。", lead: "长期复诊患者往往有多次就诊记录、检查报告和处方调整。病历摘要试点希望帮助患者和医生在复诊前快速回顾关键变化。", body: ["摘要用于帮助患者和医生快速回顾关键变化，包括诊断、主诉、检查、处方、医嘱和症状变化。摘要会标注资料来源，避免脱离原始病历。", "所有诊断、治疗和处方判断仍需医生确认。摘要不能作为独立医疗结论，也不能替代患者带齐原始资料。", "试点重点关注患者可理解性、资料可追溯性和复诊沟通效率。后续将根据医生和患者反馈持续调整摘要结构。"], tags: ["病历摘要", "复诊", "AI 边界"] },
];

const filtered = computed(() => {
  const text = query.value.trim().toLowerCase();
  return updates.filter((item) => {
    const matchKind = kind.value === "全部" || item.kind === kind.value;
    const haystack = `${item.title} ${item.summary} ${item.lead} ${item.body.join(" ")} ${item.tags.join(" ")}`.toLowerCase();
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
const editorial = computed(() => String(route.name) === "about-news"
  ? {
      title: "公告不是简单通知，而是患者就医安排的一部分",
      paragraphs: [
        "医院公告的价值不只是告诉患者“发生了什么”，更要说明这件事会影响哪些患者、从什么时候开始、患者需要采取什么动作。门诊排班、检查窗口、药房取药、系统维护和预约规则，都会直接影响患者到院计划。如果公告只写一句简短通知，患者仍然需要反复咨询；如果公告把影响范围、时间、入口和注意事项讲清楚，就能减少无效等待。",
        "智慧云脑的新闻动态页把公告、健康资讯和科研动态放在同一个内容中心，但通过分类、专题和搜索把它们区分开。患者可以快速查看最新服务安排，也可以继续阅读检查准备、用药安全和急症识别等文章。公告内容会尽量连接到患者服务、就诊指南和预约入口，让信息不只停留在阅读层面。",
        "对真实医院门户来说，公告还需要承担“风险提示”的作用。节假日门诊减少时，页面必须说明急诊是否开放；检查窗口调整时，页面必须提醒患者如何查看已预约项目；系统维护时，页面必须告诉患者哪些功能受影响、哪些服务仍可线下处理。只有把这些细节写清楚，公告才不只是内部通知，而是患者可执行的就医指引。",
        "因此，新闻动态页采用头条、专题、分类和详情弹层组合。头条用于突出当前最重要事项，专题用于快速筛选常见问题，详情用于展开完整说明。患者不需要在多个入口之间跳转，就能理解公告背景、影响范围和下一步动作。",
      ],
    }
  : {
      title: "健康文章要帮助患者做准备，而不是制造焦虑",
      paragraphs: [
        "患者阅读健康文章时，最需要的是知道自己下一步该做什么：哪些症状需要急诊，哪些信息需要记录，复诊前要准备哪些资料，检查前需要注意什么，用药过程中出现问题该如何处理。健康文章如果只罗列疾病知识，容易让患者自行诊断；如果围绕真实就医场景展开，就能帮助患者更清楚地和医生沟通。",
        "本页文章按胸痛、儿童发热、复诊用药、胃肠镜准备等高频场景组织。每篇文章都强调风险边界和资料准备：急症优先线下处理，报告需要医生解读，处方不能自行调整，AI 内容不能替代诊断。这样的文章更适合作为医院患者服务门户的一部分，而不是孤立的科普内容。",
        "健康文章的写法也会尽量避免制造焦虑。患者看到胸痛、发热、腹痛等内容时，真正需要的是区分急症信号和普通门诊准备，而不是把所有可能疾病都罗列出来。文章会把重点放在可观察、可记录、可执行的信息上，例如开始时间、严重程度、伴随症状、既往病史和当前用药。",
        "同时，每篇文章都会尽量连接到平台中的实际服务：不确定科室时进入 AI 分诊，需要预约时进入医生号源，复诊前查看病历和处方，检查前阅读检查项目说明。这样健康文章不只是阅读内容，而是患者完成就医流程的一部分。",
      ],
    });

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
      <section class="resource-article-section">
        <article class="resource-article">
          <h2>{{ editorial.title }}</h2>
          <p v-for="paragraph in editorial.paragraphs" :key="paragraph">{{ paragraph }}</p>
        </article>
      </section>

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

      <Transition name="fade">
        <div v-if="selected" ref="drawerEl" class="public-detail-panel" role="dialog" aria-modal="true" tabindex="-1" @keydown.escape="selected = null" @click.self="selected = null">
        <button type="button" aria-label="关闭" @click="selected = null">×</button>
        <p>{{ selected.kind }} · {{ selected.date }} · {{ selected.source }} · {{ selected.readingTime }}</p>
        <h2>{{ selected.title }}</h2>
        <span>{{ selected.lead }}</span>
        <div class="update-detail-body">
          <p v-for="line in selected.body" :key="line">{{ line }}</p>
        </div>
        <div class="update-detail-actions">
          <RouterLink :to="{ name: 'public-search', query: { q: selected.tags[0] } }">搜索相关资料</RouterLink>
          <RouterLink :to="{ name: 'patient-doctors' }">预约相关门诊</RouterLink>
        </div>
      </div>
        </Transition>
    </section>
  </main>
</template>
