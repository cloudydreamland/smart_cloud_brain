import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@smart-cloud-brain/shared-api";
import PatientPublicLayout from "../layouts/PatientPublicLayout.vue";
import PatientPortalLayout from "../layouts/PatientPortalLayout.vue";
import AppointmentsPage from "../pages/AppointmentsPage.vue";
import DepartmentsPage from "../pages/DepartmentsPage.vue";
import DoctorSlotsPage from "../pages/DoctorSlotsPage.vue";
import HomePage from "../pages/HomePage.vue";
import LoginPage from "../pages/LoginPage.vue";
import MedicalRecordsPage from "../pages/MedicalRecordsPage.vue";
import PatientDashboard from "../pages/PatientDashboard.vue";
import PrescriptionsPage from "../pages/PrescriptionsPage.vue";
import ProfilePage from "../pages/ProfilePage.vue";
import PublicInfoPage from "../pages/PublicInfoPage.vue";
import PublicSearchPage from "../pages/PublicSearchPage.vue";
import RegisterPage from "../pages/RegisterPage.vue";
import StaticPage from "../pages/StaticPage.vue";
import TriagePage from "../pages/TriagePage.vue";
import VisitPeoplePage from "../pages/VisitPeoplePage.vue";

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
].map((route) => ({ ...route, component: StaticPage }));

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      component: PatientPublicLayout,
      children: [
        { path: "", name: "patient-home", component: HomePage },
        { path: "guide", name: "public-guide", component: PublicInfoPage },
        { path: "conditions", name: "public-conditions", component: PublicInfoPage },
        { path: "departments", name: "public-departments", component: DepartmentsPage },
        { path: "locations", name: "public-locations", component: PublicInfoPage },
        { path: "professionals", name: "public-professionals", component: PublicInfoPage },
        { path: "research", name: "public-research", component: PublicInfoPage },
        { path: "giving", name: "public-giving", component: PublicInfoPage },
        { path: "search", name: "public-search", component: PublicSearchPage },
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
    { path: "/portal", redirect: "/patient-services" },
    { path: "/portal/:pathMatch(.*)*", redirect: (to) => `/patient-services/${to.params.pathMatch || ""}` },
    { path: "/triage", redirect: "/patient-services/triage" },
    { path: "/doctors", redirect: "/patient-services/doctors" },
    { path: "/appointments", redirect: "/patient-services/appointments" },
    { path: "/records", redirect: "/patient-services/records" },
    { path: "/prescriptions", redirect: "/patient-services/prescriptions" },
    { path: "/profile", redirect: "/patient-services/profile" },
    { path: "/visitors", redirect: "/patient-services/family" },
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
