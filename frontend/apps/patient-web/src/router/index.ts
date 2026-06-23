import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@smart-cloud-brain/shared-api";

/* Layout（首屏必需，不懒加载） */
import PatientPublicLayout from "../layouts/PatientPublicLayout.vue";
import PatientPortalLayout from "../layouts/PatientPortalLayout.vue";
import StaticPage from "../pages/StaticPage.vue";

/* 页面组件（懒加载，访问时才加载） */
const HomePage = () => import("../pages/HomePage.vue");
const LoginPage = () => import("../pages/LoginPage.vue");
const RegisterPage = () => import("../pages/RegisterPage.vue");
const PatientDashboard = () => import("../pages/PatientDashboard.vue");
const TriagePage = () => import("../pages/TriagePage.vue");
const DoctorSlotsPage = () => import("../pages/DoctorSlotsPage.vue");
const AppointmentsPage = () => import("../pages/AppointmentsPage.vue");
const MedicalRecordsPage = () => import("../pages/MedicalRecordsPage.vue");
const PrescriptionsPage = () => import("../pages/PrescriptionsPage.vue");
const ProfilePage = () => import("../pages/ProfilePage.vue");
const VisitPeoplePage = () => import("../pages/VisitPeoplePage.vue");
const PublicInfoPage = () => import("../pages/PublicInfoPage.vue");
const PublicSearchPage = () => import("../pages/PublicSearchPage.vue");
const DepartmentsPage = () => import("../pages/DepartmentsPage.vue");
const NotFound = () => import("../pages/NotFound.vue");

const staticRoutes = [
  { path: "services/internet-clinic", name: "service-internet-clinic" },
  { path: "services/exam-booking", name: "service-exam-booking" },
  { path: "services/inpatient", name: "service-inpatient" },
  { path: "services/emergency", name: "service-emergency" },
  { path: "services/international", name: "service-international" },
  { path: "doctors/experts", name: "doctor-experts" },
  { path: "doctors/centers", name: "doctor-centers" },
  { path: "doctors/schedules", name: "doctor-schedules" },
  { path: "library/symptoms", name: "library-symptoms" },
  { path: "library/drugs", name: "library-drugs" },
  { path: "library/tests", name: "library-tests" },
  { path: "library/rehab", name: "library-rehab" },
  { path: "library/articles", name: "library-articles" },
  { path: "ai/symptom-consult", name: "ai-symptom" },
  { path: "ai/record-summary", name: "ai-record-summary" },
  { path: "ai/medication", name: "ai-medication" },
  { path: "ai/health-assessment", name: "ai-assessment" },
  { path: "about/hospital", name: "about-hospital" },
  { path: "about/news", name: "about-news" },
  { path: "about/careers", name: "about-careers" },
  { path: "about/contact", name: "about-contact" },
  { path: "research", name: "public-research" },
  { path: "conditions", name: "public-conditions" },
  { path: "departments", name: "public-departments" },
  { path: "giving", name: "public-giving" },
  { path: "guide", name: "public-guide" },
  { path: "locations", name: "public-locations" },
  { path: "professionals", name: "public-professionals" },
  { path: "search", name: "public-search" },


].map((route) => ({ ...route, component: StaticPage }));

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      component: PatientPublicLayout,
      children: [
        { path: "", name: "patient-home", component: HomePage },
        { path: "login", name: "patient-login", component: LoginPage },
        { path: "register", name: "patient-register", component: RegisterPage },
        ...staticRoutes,
      ],
    },
    {
      path: "/patient-services",
      component: PatientPortalLayout,
      meta: { requiresAuth: true },
      children: [
        { path: "", name: "patient-dashboard", component: PatientDashboard },
        { path: "triage", name: "patient-triage", component: TriagePage },
        { path: "doctors", name: "patient-doctors", component: DoctorSlotsPage },
        { path: "appointments", name: "patient-appointments", component: AppointmentsPage },
        { path: "records", name: "patient-records", component: MedicalRecordsPage },
        { path: "prescriptions", name: "patient-prescriptions", component: PrescriptionsPage },
        { path: "reports", name: "patient-reports", component: StaticPage },
        { path: "invoices", name: "patient-invoices", component: StaticPage },
        { path: "messages", name: "patient-messages", component: StaticPage },
        { path: "profile", name: "patient-profile", component: ProfilePage },
        { path: "family", name: "patient-visitors", component: VisitPeoplePage },
      ],
    },
    /* 旧路径兼容重定向 */
    { path: "/portal", redirect: "/patient-services" },
    { path: "/portal/:pathMatch(.*)*", redirect: (to) => `/patient-services/${to.params.pathMatch || ""}` },
    { path: "/triage", redirect: "/patient-services/triage" },
    { path: "/doctors", redirect: "/patient-services/doctors" },
    { path: "/appointments", redirect: "/patient-services/appointments" },
    { path: "/records", redirect: "/patient-services/records" },
    { path: "/prescriptions", redirect: "/patient-services/prescriptions" },
    { path: "/profile", redirect: "/patient-services/profile" },
    { path: "/visitors", redirect: "/patient-services/family" },
    /* 404 兜底（必须放最后） */
    { path: "/:pathMatch(.*)*", name: "not-found", component: NotFound },
  ],
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  auth.load("patient-session", "PATIENT");
  if (to.meta.requiresAuth && (!auth.session || auth.permissionError)) {
    return { name: "patient-login", query: { redirect: to.fullPath } };
  }
  if ((to.name === "patient-login" || to.name === "patient-register") && auth.session && !auth.permissionError) {
    return { name: "patient-dashboard" };
  }
  return true;
});

export default router;
