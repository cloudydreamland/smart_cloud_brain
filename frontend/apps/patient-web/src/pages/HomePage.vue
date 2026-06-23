<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { api, type DataRow } from "@smart-cloud-brain/shared-api";

const router = useRouter();
const departments = ref<DataRow[]>([]);
const doctors = ref<DataRow[]>([]);
const conditionQuery = ref("");

const featuredDepartments = computed(() => departments.value.slice(0, 12));

function goSearch(q = "") {
  const queryText = q.trim();
  router.push({ name: "public-search", query: queryText ? { q: queryText } : {} });
}

onMounted(async () => {
  try {
    const [departmentRows, doctorRows] = await Promise.all([api.departments(), api.doctors()]);
    departments.value = departmentRows;
    doctors.value = doctorRows;
  } catch {
    departments.value = [];
    doctors.value = [];
  }
});
</script>

<template>
  <main class="home-page">
    <section class="home-hero">
      <div class="home-hero-content">
        <p class="home-eyebrow">智慧云脑医疗服务</p>
        <h1>专业照护，从清晰入口开始</h1>
        <div class="home-hero-actions">
          <RouterLink :to="{ name: 'patient-doctors' }">预约就诊</RouterLink>
          <RouterLink class="home-pill" :to="{ name: 'patient-triage' }">AI智能分诊</RouterLink>
        </div>
      </div>
    </section>

    <div class="home-alert">
      <strong>!</strong>
      <span>如出现胸痛、呼吸困难、意识改变、单侧肢体无力或大量出血，请立即前往急诊或拨打急救电话。</span>
    </div>

    <section class="home-section home-conditions">
      <div>
        <p class="home-eyebrow">按首字母查找疾病百科</p>
        <div class="home-letters" aria-label="疾病索引">
          <RouterLink
            v-for="letter in ['A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','R','S','T','U','V','W','X','Y','Z','#']"
            :key="letter"
            :to="{ name: 'public-conditions', query: { letter } }"
          >
            {{ letter }}
          </RouterLink>
        </div>
      </div>
      <div>
        <label class="condition-label" for="condition-search">搜索疾病、症状、医生或科室</label>
        <form class="home-searchbox" @submit.prevent="goSearch(conditionQuery)">
          <span class="search-symbol" aria-hidden="true"></span>
          <input id="condition-search" v-model="conditionQuery" type="search" placeholder="搜索">
        </form>
      </div>
    </section>

    <section class="home-section home-split">
      <div>
        <h2>患者服务是医院官网的一部分</h2>
        <h3>从分诊到复诊，保持同一条就医线索</h3>
        <p>智慧云脑把 AI 分诊、医生号源、预约记录、病历和处方连接起来，让患者在同一医院门户内完成关键服务。</p>
        <h3>真实数据驱动服务入口</h3>
        <p>当前已接入 {{ departments.length }} 个科室、{{ doctors.length }} 位医生。登录后可查看个人预约、诊后资料和家庭成员信息。</p>
        <RouterLink class="home-pill outline-blue" :to="{ name: 'patient-dashboard' }">进入患者服务</RouterLink>
      </div>
      <div class="home-image-panel">
        <img src="https://images.unsplash.com/photo-1582750433449-648ed127bb54?auto=format&fit=crop&w=1200&q=80" alt="医生与患者沟通">
      </div>
    </section>

    <section class="home-section">
      <div class="home-section-head">
        <h2>院区与到院服务</h2>
        <p>了解智慧云脑各院区服务方向，结合预约时间、科室位置和检查安排做好到院准备。</p>
      </div>
      <div class="home-locations">
        <article>
          <img src="https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=900&q=80" alt="现代化医院建筑">
          <h3>智慧云脑北京中心</h3>
          <p>北京 · 海淀</p>
        </article>
        <article>
          <img src="https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?auto=format&fit=crop&w=900&q=80" alt="医院走廊">
          <h3>智慧云脑上海中心</h3>
          <p>上海 · 浦东</p>
        </article>
        <article>
          <img src="https://images.unsplash.com/photo-1586773860418-d37222d8fce3?auto=format&fit=crop&w=900&q=80" alt="临床团队工作场景">
          <h3>智慧云脑深圳中心</h3>
          <p>深圳 · 南山</p>
        </article>
      </div>
    </section>

    <section class="home-section">
      <div class="home-section-head">
        <h2>重点诊疗领域</h2>
        <p>优先展示后端真实科室数据；暂无数据时展示常见诊疗方向。</p>
      </div>
      <div class="home-care-grid">
        <RouterLink
          v-for="dept in featuredDepartments"
          :key="String(dept.departmentId || dept.id || dept.name)"
          :to="{ name: 'public-search', query: { q: String(dept.name || dept.departmentName || '科室') } }"
        >
          {{ dept.name || dept.departmentName || "科室" }} <span>›</span>
        </RouterLink>
        <template v-if="!featuredDepartments.length">
          <RouterLink v-for="name in ['神经内科','心血管内科','呼吸内科','消化内科','骨科','妇科','儿科','肿瘤科','康复医学科']" :key="name" :to="{ name: 'public-search', query: { q: name } }">
            {{ name }} <span>›</span>
          </RouterLink>
        </template>
      </div>
    </section>

    <section class="home-donate">
      <div>
        <img src="https://images.unsplash.com/photo-1579154204601-01588f351e67?auto=format&fit=crop&w=1200&q=80" alt="实验室医学研究人员">
      </div>
      <div>
        <h2>科研与教育推动更好的患者照护</h2>
        <p>智慧云脑关注分诊质量、诊疗效率、用药安全和患者可理解性，把临床问题转化为可持续改进的服务能力。</p>
        <RouterLink class="home-pill" :to="{ name: 'public-research' }">了解科研与教育</RouterLink>
      </div>
    </section>
  </main>
</template>
