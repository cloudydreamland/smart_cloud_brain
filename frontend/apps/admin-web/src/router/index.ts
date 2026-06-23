import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@smart-cloud-brain/shared-api";

import AdminWorkspaceLayout from "../layouts/AdminWorkspaceLayout.vue";

const AdminLoginPage = () => import("../pages/AdminLoginPage.vue");
const AdminDashboard = () => import("../pages/AdminDashboard.vue");
const DepartmentsPage = () => import("../pages/DepartmentsPage.vue");
const DoctorsPage = () => import("../pages/DoctorsPage.vue");
const DrugsPage = () => import("../pages/DrugsPage.vue");
const KnowledgePage = () => import("../pages/KnowledgePage.vue");
const PromptsPage = () => import("../pages/PromptsPage.vue");
const DictsPage = () => import("../pages/DictsPage.vue");
const SchedulePage = () => import("../pages/SchedulePage.vue");
const TriageDeskPage = () => import("../pages/TriageDeskPage.vue");
const SearchPage = () => import("../pages/SearchPage.vue");
const AccountsPage = () => import("../pages/AccountsPage.vue");
const DevicesPage = () => import("../pages/DevicesPage.vue");
const PatientsPage = () => import("../pages/PatientsPage.vue");
const StatisticsPage = () => import("../pages/StatisticsPage.vue");
const PermissionsPage = () => import("../pages/PermissionsPage.vue");
const PatientSiteConfigPage = () => import("../pages/PatientSiteConfigPage.vue");
const NotFound = () => import("../pages/NotFound.vue");

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/login", name: "admin-login", component: AdminLoginPage },
    {
      path: "/",
      component: AdminWorkspaceLayout,
      meta: { requiresAuth: true },
      children: [
        { path: "", name: "admin-dashboard", component: AdminDashboard },
        { path: "departments", name: "admin-departments", component: DepartmentsPage },
        { path: "doctors", name: "admin-doctors", component: DoctorsPage },
        { path: "drugs", name: "admin-drugs", component: DrugsPage },
        { path: "knowledge", name: "admin-knowledge", component: KnowledgePage },
        { path: "prompts", name: "admin-prompts", component: PromptsPage },
        { path: "dicts", name: "admin-dicts", component: DictsPage },
        { path: "schedule", name: "admin-schedule", component: SchedulePage },
        { path: "triage-desk", name: "admin-triage", component: TriageDeskPage },
        { path: "devices", name: "admin-devices", component: DevicesPage },
        { path: "patients", name: "admin-patients", component: PatientsPage },
        { path: "statistics", name: "admin-statistics", component: StatisticsPage },
        { path: "accounts", name: "admin-accounts", component: AccountsPage },
        { path: "permissions", name: "admin-permissions", component: PermissionsPage },
        { path: "patient-site", name: "admin-patient-site", component: PatientSiteConfigPage },
        { path: "search", name: "admin-search", component: SearchPage },
      ],
    },
    { path: "/:pathMatch(.*)*", name: "not-found", component: NotFound },
  ],
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  auth.load("admin-session", "ADMIN");
  if (to.meta.requiresAuth && (!auth.session || auth.permissionError)) return { name: "admin-login", query: { redirect: to.fullPath } };
  if (to.name === "admin-login" && auth.session && !auth.permissionError) return { name: "admin-dashboard" };
  return true;
});

export default router;
