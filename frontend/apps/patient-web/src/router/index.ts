import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@smart-cloud-brain/shared-api";

/* Layout（首屏必需，不懒加载） */
import PatientPublicLayout from "../layouts/PatientPublicLayout.vue";
import PatientPortalLayout from "../layouts/PatientPortalLayout.vue";

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
const CmsPage = () => import("../pages/CmsPage.vue");
const PublicSearchPage = () => import("../pages/PublicSearchPage.vue");
const DepartmentsPage = () => import("../pages/DepartmentsPage.vue");
const DoctorTeamPage = () => import("../pages/DoctorTeamPage.vue");
const PublicUpdatesPage = () => import("../pages/PublicUpdatesPage.vue");
const PatientReportsPage = () => import("../pages/PatientReportsPage.vue");
const PatientInvoicesPage = () => import("../pages/PatientInvoicesPage.vue");
const PatientMessagesPage = () => import("../pages/PatientMessagesPage.vue");
const NotFound = () => import("../pages/NotFound.vue");

const staticRoutes = [
  { path: "services/internet-clinic", name: "service-internet-clinic", component: PublicInfoPage },
  { path: "services/exam-booking", name: "service-exam-booking", component: PublicInfoPage },
  { path: "services/inpatient", name: "service-inpatient", component: PublicInfoPage },
  { path: "services/emergency", name: "service-emergency", component: PublicInfoPage },
  { path: "services/international", name: "service-international", component: PublicInfoPage },
  { path: "doctors/experts", name: "doctor-experts", component: DoctorTeamPage },
  { path: "doctors/centers", name: "doctor-centers", component: PublicInfoPage },
  { path: "doctors/schedules", name: "doctor-schedules", component: PublicInfoPage },
  { path: "library/symptoms", name: "library-symptoms", component: PublicInfoPage },
  { path: "library/drugs", name: "library-drugs", component: PublicInfoPage },
  { path: "library/tests", name: "library-tests", component: PublicInfoPage },
  { path: "library/rehab", name: "library-rehab", component: PublicInfoPage },
  { path: "library/articles", name: "library-articles", component: PublicUpdatesPage },
  { path: "ai/symptom-consult", name: "ai-symptom", component: PublicInfoPage },
  { path: "ai/record-summary", name: "ai-record-summary", component: PublicInfoPage },
  { path: "ai/medication", name: "ai-medication", component: PublicInfoPage },
  { path: "ai/health-assessment", name: "ai-assessment", component: PublicInfoPage },
  { path: "about/hospital", name: "about-hospital", component: PublicInfoPage },
  { path: "about/news", name: "about-news", component: PublicUpdatesPage },
  { path: "about/careers", name: "about-careers", component: PublicInfoPage },
  { path: "about/contact", name: "about-contact", component: PublicInfoPage },
  { path: "research", name: "public-research", component: PublicInfoPage },
  { path: "conditions", name: "public-conditions", component: PublicInfoPage },
  { path: "departments", name: "public-departments", component: DepartmentsPage },
  { path: "giving", name: "public-giving", component: PublicInfoPage },
  { path: "guide", name: "public-guide", component: PublicInfoPage },
  { path: "locations", name: "public-locations", component: PublicInfoPage },
  { path: "professionals", name: "public-professionals", component: PublicInfoPage },
  { path: "search", name: "public-search", component: PublicSearchPage },
];

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
        { path: "pages/:slug", name: "cms-page", component: CmsPage },
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
        { path: "reports", name: "patient-reports", component: PatientReportsPage },
        { path: "invoices", name: "patient-invoices", component: PatientInvoicesPage },
        { path: "messages", name: "patient-messages", component: PatientMessagesPage },
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
