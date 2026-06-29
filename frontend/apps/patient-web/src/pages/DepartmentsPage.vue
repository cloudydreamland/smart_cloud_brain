<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { api, type DataRow, type PatientRecommendation } from "@smart-cloud-brain/shared-api";

type DepartmentResource = {
  name: string;
  description: string;
  symptoms: string[];
  preparation: string[];
};

const departments = ref<DataRow[]>([]);
const recommendedDepartments = ref<PatientRecommendation[]>([]);
const loading = ref(true);
const failed = ref(false);

const fallbackDepartments: DepartmentResource[] = [
  {
    name: "神经内科",
    description: "评估头痛、眩晕、脑血管病、认知障碍、癫痫、睡眠和周围神经相关问题。",
    symptoms: ["突发或反复头痛", "眩晕、走路不稳", "肢体麻木无力", "记忆下降或睡眠异常"],
    preparation: ["记录发作时间和持续多久", "携带头颅影像、脑电图或既往病历", "整理正在使用的降压、抗凝或镇静药物"],
  },
  {
    name: "心血管内科",
    description: "处理胸闷胸痛、心悸、高血压、冠心病、心律失常和心功能管理。",
    symptoms: ["胸痛或胸闷", "心悸、心跳不齐", "血压长期异常", "活动后气短或水肿"],
    preparation: ["带近期血压和心率记录", "携带心电图、心脏彩超、冠脉检查结果", "列出长期用药和不良反应"],
  },
  {
    name: "呼吸内科",
    description: "关注咳嗽、哮喘、慢阻肺、肺部感染、肺结节和睡眠呼吸问题。",
    symptoms: ["持续咳嗽或咳痰", "喘息、气短", "反复发热伴肺部阴影", "睡眠打鼾和白天嗜睡"],
    preparation: ["记录体温、咳痰颜色和气短程度", "携带胸片或胸部 CT", "说明吸烟史、过敏史和既往肺病史"],
  },
  {
    name: "消化内科",
    description: "处理腹痛、反酸、腹泻便秘、肝胆胰疾病和胃肠镜相关评估。",
    symptoms: ["上腹痛、反酸烧心", "腹泻或便秘", "黑便、呕血", "肝功能异常或黄疸"],
    preparation: ["记录饮食、排便和体重变化", "携带胃肠镜、腹部超声或肝功能结果", "如需检查，提前确认是否空腹"],
  },
  {
    name: "骨科",
    description: "评估关节疼痛、运动损伤、脊柱问题、骨折术后和慢性疼痛。",
    symptoms: ["颈肩腰腿痛", "关节肿痛或活动受限", "运动损伤", "骨折或术后复查"],
    preparation: ["携带 X 线、CT、MRI 或手术记录", "记录疼痛部位、诱因和活动受限程度", "穿着便于暴露和活动检查的衣物"],
  },
  {
    name: "妇科",
    description: "处理月经异常、妇科炎症、盆腔疼痛、围绝经期管理和孕前咨询。",
    symptoms: ["月经异常", "下腹痛或异常分泌物", "围绝经期不适", "孕前或备孕咨询"],
    preparation: ["记录月经周期和末次月经", "携带妇科超声、宫颈筛查或激素检查", "说明妊娠、手术和用药情况"],
  },
  {
    name: "儿科",
    description: "覆盖儿童发热、咳嗽、消化问题、生长发育、过敏和疫苗咨询。",
    symptoms: ["儿童发热咳嗽", "腹泻呕吐", "皮疹或过敏", "生长发育问题"],
    preparation: ["记录体温变化、精神状态和进食情况", "携带疫苗本和既往检查", "由熟悉病情的监护人陪同"],
  },
  {
    name: "肿瘤科",
    description: "提供肿瘤筛查、治疗方案咨询、随访管理、症状控制和支持治疗建议。",
    symptoms: ["肿瘤筛查异常", "治疗方案咨询", "复查影像异常", "疼痛、乏力或营养问题"],
    preparation: ["携带病理报告、分期资料和治疗记录", "整理近期影像和肿瘤标志物", "列出当前治疗副作用和用药"],
  },
  {
    name: "康复医学科",
    description: "面向术后康复、神经康复、疼痛管理、运动功能训练和长期功能恢复。",
    symptoms: ["术后活动受限", "脑卒中后功能障碍", "慢性疼痛", "步态和平衡问题"],
    preparation: ["携带手术或住院记录", "记录当前活动能力和目标", "穿着便于评估活动度的衣物"],
  },
];

function normalizeDepartment(row: DataRow | PatientRecommendation): DepartmentResource {
  const name = String(("title" in row && row.title) || ("targetName" in row && row.targetName) || ("name" in row && row.name) || ("departmentName" in row && row.departmentName) || "未命名科室");
  const matched = fallbackDepartments.find((item) => name.includes(item.name) || item.name.includes(name));
  return {
    name,
    description: String(("description" in row && row.description) || ("remark" in row && row.remark) || ("specialty" in row && row.specialty) || matched?.description || "提供门诊、复诊和专科评估服务。"),
    symptoms: matched?.symptoms ?? ["常见病和慢病复诊", "专科症状评估", "检查结果解读", "治疗方案咨询"],
    preparation: matched?.preparation ?? ["准备既往病历和检查报告", "记录主要症状的时间线", "整理长期用药和过敏史"],
  };
}

const visibleDepartments = computed(() => {
  if (recommendedDepartments.value.length) return recommendedDepartments.value.map(normalizeDepartment);
  return departments.value.length ? departments.value.map(normalizeDepartment) : fallbackDepartments;
});

onMounted(async () => {
  try {
    const [recommendationRows, departmentRows] = await Promise.all([
      api.patientSiteRecommendations("DEPARTMENT").catch(() => []),
      api.departments(),
    ]);
    recommendedDepartments.value = recommendationRows;
    departments.value = departmentRows;
  } catch {
    failed.value = true;
    departments.value = [];
  } finally {
    loading.value = false;
  }
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
        <span>科室与重点专科</span>
      </nav>
      <p class="resource-label">科室资料</p>
      <h1>科室与重点专科</h1>
      <p>优先展示后端科室数据，同时补充常见症状和就诊准备。无后端数据时展示默认重点专科资料。</p>
    </header>

    <div class="resource-layout">
      <aside class="resource-toc" aria-label="科室目录">
        <strong>科室目录</strong>
        <a v-for="department in visibleDepartments" :key="department.name" :href="`#${department.name}`">{{ department.name }}</a>
      </aside>

      <section class="resource-content">
        <div class="resource-alert subtle">
          <strong>{{ loading ? "正在同步科室数据" : recommendedDepartments.length ? "数据来自患者端推荐配置" : failed || !departments.length ? "当前显示默认重点专科" : "数据来自后端科室接口" }}</strong>
          <p v-if="loading">请稍候，正在读取可用科室。</p>
          <p v-else-if="failed">科室接口暂不可用，以下资料用于就诊方向参考。</p>
          <p v-else-if="!departments.length">后端暂无科室数据，以下资料用于就诊方向参考。</p>
          <p v-else>每个科室条目已结合本地资料补充就诊准备信息。</p>
        </div>

        <article v-for="department in visibleDepartments" :id="department.name" :key="department.name" class="resource-block department-resource">
          <div>
            <h2>{{ department.name }}</h2>
            <p>{{ department.description }}</p>
          </div>
          <div class="resource-columns">
            <section>
              <h3>常见就诊问题</h3>
              <ul>
                <li v-for="symptom in department.symptoms" :key="symptom">{{ symptom }}</li>
              </ul>
            </section>
            <section>
              <h3>建议准备</h3>
              <ul>
                <li v-for="item in department.preparation" :key="item">{{ item }}</li>
              </ul>
            </section>
          </div>
          <div class="resource-links">
            <RouterLink :to="{ name: 'public-search', query: { q: department.name } }">搜索相关医生和资料</RouterLink>
            <RouterLink :to="{ name: 'patient-triage' }">不确定科室，先做分诊</RouterLink>
          </div>
        </article>
      </section>
    </div>
  </main>
</template>
