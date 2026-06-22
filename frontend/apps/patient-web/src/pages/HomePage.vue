<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { api, type DataRow } from "@smart-cloud-brain/shared-api";

const searchOpen = ref(false);
const mobileNavOpen = ref(false);
const departments = ref<DataRow[]>([]);
const doctors = ref<DataRow[]>([]);

const featuredDepartments = computed(() => departments.value.slice(0, 12));

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
    <header class="home-topbar">
      <RouterLink class="scb-brand" :to="{ name: 'patient-home' }" aria-label="智慧云脑首页">
        <span>智慧<br />云脑</span>
        <i></i><i></i>
      </RouterLink>
      <nav class="home-nav" :class="{ open: mobileNavOpen }" aria-label="主导航">
        <div class="home-nav-item">
          <button type="button">智慧云脑<br />医疗服务 <span></span></button>
          <div class="home-mega">
            <div>
              <h3>以患者为中心的照护</h3>
              <p>多学科专家团队协作，为复杂疾病提供清晰诊断和连续治疗方案。</p>
            </div>
            <div>
              <RouterLink :to="{ name: 'patient-login' }">预约就诊</RouterLink>
              <a href="#departments">查找科室</a>
              <a href="#locations">院区位置</a>
              <a href="#care">重点诊疗</a>
            </div>
          </div>
        </div>
        <div class="home-nav-item">
          <button type="button">健康<br />资料库 <span></span></button>
          <div class="home-mega">
            <div>
              <h3>可靠的健康答案</h3>
              <p>从症状、检查、治疗到用药提醒，帮助患者做出更清晰的就诊决策。</p>
            </div>
            <div>
              <a href="#conditions">疾病与病症</a>
              <a href="#care">重点专科</a>
              <RouterLink :to="{ name: 'patient-triage' }">AI 分诊</RouterLink>
            </div>
          </div>
        </div>
        <div class="home-nav-item">
          <button type="button">面向医疗<br />专业人士 <span></span></button>
          <div class="home-mega">
            <div>
              <h3>专业协作</h3>
              <p>支持医生转诊、临床资料协同和连续诊疗追踪。</p>
            </div>
            <div>
              <a href="#research">临床试验</a>
              <a href="#research">科研中心</a>
              <a href="#research">继续教育</a>
            </div>
          </div>
        </div>
        <div class="home-nav-item">
          <button type="button">智慧云脑<br />科研与教育 <span></span></button>
          <div class="home-mega">
            <div>
              <h3>科研与人才培养</h3>
              <p>通过实验室、临床试验和医学教育，将前沿发现转化为真实照护能力。</p>
            </div>
            <div>
              <a href="#research">研究团队</a>
              <a href="#research">中心与项目</a>
              <a href="#research">医学教育</a>
            </div>
          </div>
        </div>
        <div class="home-nav-item">
          <button type="button">支持<br />智慧云脑 <span></span></button>
          <div class="home-mega">
            <div>
              <h3>支持未来医学</h3>
              <p>助力患者照护、医学研究和人才培养，让更多生命受益。</p>
            </div>
            <div>
              <a href="#research">立即支持</a>
              <a href="#research">公益合作</a>
            </div>
          </div>
        </div>
      </nav>
      <RouterLink class="home-appointment" :to="{ name: 'patient-login' }">预约就诊</RouterLink>
      <button class="home-icon-button" type="button" aria-label="打开搜索" @click="searchOpen = true"><span class="search-symbol"></span></button>
      <button class="home-menu-button" type="button" aria-label="打开菜单" @click="mobileNavOpen = !mobileNavOpen"><span></span></button>
    </header>

    <section class="home-hero">
      <span class="home-play" aria-hidden="true"></span>
      <div class="home-hero-content">
        <h1>重塑您的医疗照护</h1>
        <div class="home-hero-actions">
          <a href="#care">了解我们如何推动创新</a>
          <RouterLink class="home-pill" :to="{ name: 'patient-login' }">预约就诊</RouterLink>
        </div>
      </div>
    </section>

    <div class="home-alert">
      <strong>!</strong>
      <span>公告：患者信息有限披露说明</span>
    </div>

    <section id="conditions" class="home-section home-conditions">
      <div>
        <p class="home-eyebrow">按首字母查找疾病与病症</p>
        <div class="home-letters" aria-label="疾病索引">
          <a v-for="letter in ['A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','R','S','T','U','V','W','X','Y','Z','#']" :key="letter" href="#conditions">{{ letter }}</a>
        </div>
      </div>
      <div>
        <label class="condition-label" for="condition-search">搜索疾病与病症</label>
        <div class="home-searchbox">
          <span class="search-symbol" aria-hidden="true"></span>
          <input id="condition-search" type="search" placeholder="搜索">
        </div>
      </div>
    </section>

    <section class="home-section home-split">
      <div>
        <h2>疗愈从这里开始</h2>
        <h3>第一次就找到正确答案</h3>
        <p>有效治疗始于准确诊断。智慧云脑把分诊、号源、病历和处方串成一条连续的患者服务流程。</p>
        <h3>真实数据驱动的患者门户</h3>
        <p>当前已接入 {{ departments.length }} 个科室、{{ doctors.length }} 位医生，登录后可查看实时号源和个人诊疗记录。</p>
        <RouterLink class="home-pill outline-blue" :to="{ name: 'patient-login' }">开始预约流程</RouterLink>
      </div>
      <div class="home-image-panel">
        <img src="https://images.unsplash.com/photo-1582750433449-648ed127bb54?auto=format&fit=crop&w=1200&q=80" alt="医生与患者沟通">
      </div>
    </section>

    <section id="locations" class="home-section">
      <div class="home-section-head">
        <h2>院区位置</h2>
        <p>了解智慧云脑各院区信息，或选择特定院区查看服务。</p>
      </div>
      <div class="home-locations">
        <article>
          <img src="https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=900&q=80" alt="现代化医院院区">
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

    <section id="departments" class="home-section">
      <div class="home-section-head">
        <h2>重点诊疗领域</h2>
        <p>优先展示后端真实科室数据；暂无数据时显示核心诊疗方向。</p>
      </div>
      <div class="home-care-grid">
        <RouterLink v-for="dept in featuredDepartments" :key="String(dept.departmentId || dept.id || dept.name)" :to="{ name: 'patient-login' }">
          {{ dept.name || dept.departmentName || '科室' }} <span>›</span>
        </RouterLink>
        <template v-if="!featuredDepartments.length">
          <RouterLink v-for="name in ['神经内科','心血管内科','呼吸内科','消化内科','骨科','妇科','儿科','肿瘤科','康复医学科']" :key="name" :to="{ name: 'patient-login' }">
            {{ name }} <span>›</span>
          </RouterLink>
        </template>
      </div>
    </section>

    <section id="research" class="home-donate">
      <div>
        <img src="https://images.unsplash.com/photo-1579154204601-01588f351e67?auto=format&fit=crop&w=1200&q=80" alt="实验室医学研究人员">
      </div>
      <div>
        <h2>支持突破性的医学研究</h2>
        <p>您的支持将推动未来医学发展，帮助更多患者获得新的希望。</p>
        <a class="home-pill" href="#research">立即支持</a>
      </div>
    </section>

    <footer class="home-footer">
      <div class="home-footer-links">
        <RouterLink :to="{ name: 'patient-login' }">查找医生</RouterLink>
        <a href="#research">探索职业机会</a>
        <a href="#conditions">订阅健康资讯</a>
      </div>
      <p>语言：简体中文 · 使用条款 · 隐私政策 · 数字无障碍声明<br />© 2026 智慧云脑。患者端静态视觉复现，业务数据来自后端接口。</p>
    </footer>

    <div v-if="searchOpen" class="home-search-overlay" role="dialog" aria-modal="true" aria-labelledby="home-search-title" @click.self="searchOpen = false">
      <div class="home-search-panel">
        <button type="button" aria-label="关闭搜索" @click="searchOpen = false">×</button>
        <h2 id="home-search-title">搜索智慧云脑</h2>
        <div class="home-searchbox">
          <span class="search-symbol" aria-hidden="true"></span>
          <input type="search" placeholder="搜索医生、科室和服务" autofocus>
        </div>
      </div>
    </div>
  </main>
</template>
