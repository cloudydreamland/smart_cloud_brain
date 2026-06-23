<script setup lang="ts">
import { computed } from "vue";
import { RouterLink, useRoute } from "vue-router";

type RouteTarget = { name: string; query?: Record<string, string> };

type ResourceBlock = {
  title: string;
  summary: string;
  details: string[];
  note?: string;
  links?: { label: string; to: RouteTarget }[];
};

type PublicPage = {
  label: string;
  title: string;
  intro: string;
  emergency?: string;
  blocks: ResourceBlock[];
  related: { label: string; to: RouteTarget; description: string }[];
};

const route = useRoute();

const pages: Record<string, PublicPage> = {
  "public-guide": {
    label: "患者资料",
    title: "患者服务指南",
    intro: "按就诊前、预约、到院、诊后四个阶段整理患者端可用资料，帮助你知道下一步该准备什么、在哪里完成。",
    emergency: "突发胸痛、呼吸困难、意识障碍、单侧肢体无力、大量出血等情况，请立即前往急诊或拨打急救电话，不要等待线上分诊。",
    blocks: [
      {
        title: "就诊前准备",
        summary: "首次就诊前，先把能帮助医生判断的信息整理完整。信息越具体，分诊和预约越准确。",
        details: [
          "准备身份证件、医保信息、既往检查报告、影像资料和出院小结。",
          "记录主要症状的开始时间、变化趋势、诱因、缓解因素和伴随症状。",
          "列出正在使用的药物、保健品、过敏史、既往病史和家族病史。",
          "儿童、老人或代他人就诊时，提前维护就诊人资料和联系电话。",
        ],
        links: [{ label: "维护就诊人", to: { name: "patient-visitors" } }],
      },
      {
        title: "智能分诊",
        summary: "当你不确定挂哪个科室时，可先用 AI 分诊整理症状并获得推荐科室。",
        details: [
          "用自然语言描述本次最困扰你的症状，不需要使用医学术语。",
          "补充持续时间、严重程度、是否反复发作、是否影响睡眠或日常活动。",
          "分诊建议用于挂号参考，不替代医生诊断。",
        ],
        links: [{ label: "进入智能分诊", to: { name: "patient-triage" } }],
      },
      {
        title: "预约挂号",
        summary: "登录后可查看号源，按科室、医生和日期选择适合的就诊时段。",
        details: [
          "确认就诊人、联系方式、预约科室、医生和就诊时间。",
          "如分诊推荐科室和个人选择不一致，优先核对症状是否描述完整。",
          "无法按时到院时，应提前取消预约，避免影响后续号源使用。",
        ],
        links: [{ label: "登录预约", to: { name: "patient-login" } }],
      },
      {
        title: "诊后资料",
        summary: "医生保存病历和处方后，患者端会同步展示，便于复诊和用药核对。",
        details: [
          "病历记录包含诊断、主诉、现病史、医嘱等信息。",
          "处方记录可查看药品明细和风险审核结果。",
          "复诊时建议携带近期病历、处方、检查报告和症状变化记录。",
        ],
      },
    ],
    related: [
      { label: "科室与重点专科", to: { name: "public-departments" }, description: "了解各科室服务范围和常见就诊问题。" },
      { label: "疾病与病症索引", to: { name: "public-conditions" }, description: "按症状和疾病方向准备就诊资料。" },
      { label: "院区位置", to: { name: "public-locations" }, description: "查看协作院区和到院注意事项。" },
    ],
  },
  "public-conditions": {
    label: "健康资料",
    title: "疾病与病症索引",
    intro: "这里不是诊断结论，而是帮助患者判断该记录哪些信息、可能涉及哪些科室、什么时候需要尽快就医。",
    emergency: "症状突然严重、迅速进展或伴随意识改变、呼吸困难、持续胸痛、抽搐、高热不退时，应优先线下急诊。",
    blocks: [
      {
        title: "发热、咳嗽、咽痛",
        summary: "常见于呼吸道感染，也可能与过敏、哮喘、肺部疾病或其他感染有关。",
        details: [
          "记录体温最高值、持续天数、是否寒战、是否咳痰或胸痛。",
          "准备既往肺部疾病、哮喘史、近期接触史和用药记录。",
          "高热不退、呼吸困难、口唇发紫、精神反应差应尽快就医。",
        ],
        links: [{ label: "搜索呼吸内科", to: { name: "public-search", query: { q: "呼吸内科" } } }],
      },
      {
        title: "头痛、眩晕、肢体麻木",
        summary: "可能涉及神经内科、耳鼻喉、眼科、心血管或急诊评估。",
        details: [
          "记录头痛部位、性质、持续时间、是否突然达到最严重程度。",
          "留意是否伴随说话不清、口角歪斜、单侧无力、视物异常。",
          "突发剧烈头痛或神经功能异常需要立即急诊。",
        ],
        links: [{ label: "搜索神经内科", to: { name: "public-search", query: { q: "神经内科" } } }],
      },
      {
        title: "胸闷、胸痛、心悸",
        summary: "可涉及心血管、呼吸、消化或焦虑相关问题，其中持续胸痛需要谨慎处理。",
        details: [
          "记录发作时间、持续多久、是否与活动有关、是否向肩背或下颌放射。",
          "准备血压、心率、既往心脏病史、用药和检查结果。",
          "压榨样胸痛、出汗、濒死感、气短或晕厥应立即急诊。",
        ],
        links: [{ label: "搜索心血管内科", to: { name: "public-search", query: { q: "心血管内科" } } }],
      },
      {
        title: "腹痛、腹泻、恶心呕吐",
        summary: "常见于消化系统问题，也可能与泌尿、妇科、代谢或感染相关。",
        details: [
          "记录腹痛位置、性质、进食关系、排便变化和是否发热。",
          "准备近期饮食、用药、旅行史、既往胃肠或肝胆胰疾病史。",
          "持续加重、黑便、呕血、剧烈腹痛或腹部板硬应尽快就医。",
        ],
        links: [{ label: "搜索消化内科", to: { name: "public-search", query: { q: "消化内科" } } }],
      },
      {
        title: "慢病复诊资料",
        summary: "高血压、糖尿病、慢阻肺、骨关节疾病等慢病复诊，需要带趋势记录。",
        details: [
          "带上近期家庭监测数据，例如血压、血糖、体重、峰流速或疼痛评分。",
          "记录药物调整、漏服情况、不良反应和生活方式变化。",
          "不要只提供单次异常值，连续趋势更有助于医生判断。",
        ],
      },
    ],
    related: [
      { label: "智能分诊", to: { name: "patient-triage" }, description: "不确定科室时先整理症状并获得推荐方向。" },
      { label: "搜索资料", to: { name: "public-search" }, description: "搜索科室、医生、症状和服务条目。" },
      { label: "患者服务指南", to: { name: "public-guide" }, description: "了解预约和诊后资料查看流程。" },
    ],
  },
  "public-locations": {
    label: "院区资料",
    title: "院区位置",
    intro: "整理协作院区的服务范围、到院流程和出行注意事项。具体排班和号源以预约页面为准。",
    blocks: [
      {
        title: "北京中心",
        summary: "面向综合门诊、神经内科、心血管内科和康复医学等方向，适合需要多学科协作评估的患者。",
        details: [
          "建议提前 30 分钟到院完成签到和资料核验。",
          "携带既往影像资料时，优先准备原始影像或可读取的电子资料。",
          "复诊患者可在患者端回看病历和处方后再到院。",
        ],
        note: "院区信息用于到院准备，实际服务窗口和排班以当天现场安排为准。",
      },
      {
        title: "上海中心",
        summary: "聚焦消化、呼吸、妇科和儿科等常见专科服务，衔接线上分诊和现场就诊流程。",
        details: [
          "儿童就诊建议由熟悉病情的监护人陪同。",
          "慢病复诊请带近期监测记录和长期用药清单。",
          "如需检查，请根据医生建议确认是否空腹或停药。",
        ],
      },
      {
        title: "深圳中心",
        summary: "提供肿瘤随访、骨科康复和慢病管理协作服务，便于长期患者持续追踪。",
        details: [
          "康复评估建议穿着便于活动的衣物。",
          "肿瘤随访请带近期影像、病理、治疗记录和用药方案。",
          "长期管理患者建议固定记录症状变化和生活质量变化。",
        ],
      },
      {
        title: "到院通用流程",
        summary: "不同院区可能略有差异，但患者端预约后的现场流程通常一致。",
        details: [
          "预约确认后，按短信或患者端记录核对就诊时间。",
          "到院后完成签到、资料核验、候诊和诊室就诊。",
          "诊后根据医生建议完成检查、取药、复诊或线上资料查看。",
        ],
      },
    ],
    related: [
      { label: "预约就诊", to: { name: "patient-login" }, description: "登录后查看实际号源和医生排班。" },
      { label: "科室与重点专科", to: { name: "public-departments" }, description: "确认院区服务方向前先了解科室。" },
      { label: "患者服务指南", to: { name: "public-guide" }, description: "查看完整就诊流程。" },
    ],
  },
  "public-professionals": {
    label: "专业协作",
    title: "医疗专业人士",
    intro: "面向医生、护理团队和合作机构，说明患者转诊、资料协同、临床路径和继续教育的协作方式。",
    blocks: [
      {
        title: "转诊资料准备",
        summary: "协作医生可围绕患者主诉、已完成检查和初步处理，形成更清晰的转诊摘要。",
        details: [
          "明确转诊目的：进一步诊断、治疗方案评估、康复管理或长期随访。",
          "整理关键检查结果、既往诊断、已用药物和治疗反应。",
          "标注需要优先排除的风险和希望专科回答的问题。",
        ],
      },
      {
        title: "临床资料协同",
        summary: "患者端、医生端和管理端围绕分诊、挂号、病历、处方形成连续资料链。",
        details: [
          "分诊信息可作为首诊沟通线索，但需要医生复核。",
          "病历生成辅助用于提高记录效率，不替代医生判断和签名责任。",
          "处方审核提示风险项，最终用药决策由医生结合病情确认。",
        ],
      },
      {
        title: "继续教育内容",
        summary: "围绕常见病、急诊分级、处方风险、病历书写和 AI 工具边界组织学习材料。",
        details: [
          "病例结构化训练：从主诉到鉴别诊断再到处理建议。",
          "急诊分级训练：识别需要立即处理的红旗症状。",
          "用药安全训练：禁忌证、相互作用和特殊人群用药。",
        ],
      },
      {
        title: "协作边界",
        summary: "平台提供资料整理和流程协同，不改变医疗责任边界。",
        details: [
          "涉及诊断、治疗和处方的结论必须由具备资质的医生确认。",
          "科研和教学使用资料应遵循授权、脱敏和伦理审查要求。",
          "患者隐私资料按最小必要原则使用和展示。",
        ],
      },
    ],
    related: [
      { label: "科研与教育", to: { name: "public-research" }, description: "查看研究、教学和数据治理方向。" },
      { label: "科室与重点专科", to: { name: "public-departments" }, description: "了解可协作的专科方向。" },
      { label: "搜索资料", to: { name: "public-search", query: { q: "转诊" } }, description: "搜索协作相关条目。" },
    ],
  },
  "public-research": {
    label: "科研教育",
    title: "科研与教育",
    intro: "围绕真实临床流程中的问题，整理研究方向、教学材料和数据治理要求。",
    blocks: [
      {
        title: "临床应用研究",
        summary: "重点评估智能工具对分诊质量、预约效率、病历质量和用药安全的影响。",
        details: [
          "分诊准确性：推荐科室是否与医生最终判断一致。",
          "患者体验：是否减少重复询问、错挂科室和无效等待。",
          "诊后管理：病历、处方和复诊建议是否更容易被患者理解。",
        ],
      },
      {
        title: "医学教育",
        summary: "将真实业务流程拆解为可训练、可复盘的学习资料。",
        details: [
          "病历书写：主诉、现病史、既往史和医嘱结构化表达。",
          "处方审核：风险提示、特殊人群、相互作用和禁忌证。",
          "医患沟通：把复杂医学信息转换为患者能执行的建议。",
        ],
      },
      {
        title: "数据治理",
        summary: "科研和教育资料使用坚持授权、脱敏、审计和最小必要原则。",
        details: [
          "研究数据使用前应明确目的、范围和访问人员。",
          "教学案例应去标识化处理，避免暴露个人身份信息。",
          "系统操作、资料访问和导出应保留审计记录。",
        ],
      },
      {
        title: "转化方向",
        summary: "研究结果应回到临床流程，改善患者可理解性和团队协作效率。",
        details: [
          "优化分诊问题设计，减少患者遗漏关键信息。",
          "改进医生端病历和处方审核工作流。",
          "形成面向患者的复诊、用药和检查准备资料。",
        ],
      },
    ],
    related: [
      { label: "医疗专业人士", to: { name: "public-professionals" }, description: "了解协作和继续教育资料。" },
      { label: "支持智慧云脑", to: { name: "public-giving" }, description: "查看可支持的项目方向。" },
      { label: "疾病与病症索引", to: { name: "public-conditions" }, description: "查看患者教育资料基础内容。" },
    ],
  },
  "public-giving": {
    label: "支持项目",
    title: "支持智慧云脑",
    intro: "支持方向聚焦患者照护、医学研究和人才培养，强调可追踪、可解释、可持续的医疗服务改进。",
    blocks: [
      {
        title: "患者照护支持",
        summary: "帮助长期治疗、异地复诊、复杂病情和行动不便患者获得连续服务。",
        details: [
          "支持患者整理复诊资料、病历摘要和长期症状记录。",
          "支持慢病患者获得随访教育和用药提醒资料。",
          "支持弱势患者更顺畅地完成分诊、预约和诊后管理。",
        ],
      },
      {
        title: "医学研究支持",
        summary: "支持围绕诊疗质量、用药安全和患者体验的应用研究。",
        details: [
          "评估智能分诊对错挂科室和等待时间的影响。",
          "研究处方风险提示对用药安全的改善。",
          "优化患者端资料呈现，让医嘱更容易被理解和执行。",
        ],
      },
      {
        title: "人才培养支持",
        summary: "支持医护团队学习数字医疗、AI 辅助诊疗和数据治理。",
        details: [
          "建设病例复盘、急诊分级和用药风险教学资料。",
          "支持年轻医生参与跨学科协作训练。",
          "支持护理团队开展患者教育和诊后随访能力建设。",
        ],
      },
      {
        title: "项目透明度",
        summary: "支持项目应明确用途、阶段目标和结果反馈方式。",
        details: [
          "明确项目服务对象和预期改善的问题。",
          "定期汇总项目进展、使用范围和阶段性成果。",
          "涉及患者资料的项目必须遵循隐私保护和授权要求。",
        ],
      },
    ],
    related: [
      { label: "科研与教育", to: { name: "public-research" }, description: "了解研究和教学项目。" },
      { label: "患者服务指南", to: { name: "public-guide" }, description: "查看患者照护流程。" },
      { label: "医疗专业人士", to: { name: "public-professionals" }, description: "了解协作和培训方向。" },
    ],
  },
};

const page = computed(() => pages[String(route.name)] ?? pages["public-guide"]);
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
        <span>{{ page.title }}</span>
      </nav>
      <p class="resource-label">{{ page.label }}</p>
      <h1>{{ page.title }}</h1>
      <p>{{ page.intro }}</p>
    </header>

    <div class="resource-layout">
      <aside class="resource-toc" aria-label="资料目录">
        <strong>资料目录</strong>
        <a v-for="block in page.blocks" :key="block.title" :href="`#${block.title}`">{{ block.title }}</a>
      </aside>

      <section class="resource-content">
        <div v-if="page.emergency" class="resource-alert">
          <strong>就医提醒</strong>
          <p>{{ page.emergency }}</p>
        </div>

        <article v-for="block in page.blocks" :id="block.title" :key="block.title" class="resource-block">
          <div>
            <h2>{{ block.title }}</h2>
            <p>{{ block.summary }}</p>
          </div>
          <ul>
            <li v-for="detail in block.details" :key="detail">{{ detail }}</li>
          </ul>
          <p v-if="block.note" class="resource-note">{{ block.note }}</p>
          <div v-if="block.links?.length" class="resource-links">
            <RouterLink v-for="link in block.links" :key="link.label" :to="link.to">{{ link.label }}</RouterLink>
          </div>
        </article>

        <section class="resource-related" aria-label="相关资料">
          <h2>相关资料</h2>
          <div>
            <RouterLink v-for="item in page.related" :key="item.label" :to="item.to">
              <strong>{{ item.label }}</strong>
              <span>{{ item.description }}</span>
            </RouterLink>
          </div>
        </section>
      </section>
    </div>
  </main>
</template>
