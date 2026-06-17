import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@smart-cloud-brain/shared-api";
import AdminLoginPage from "../pages/AdminLoginPage.vue";
import AdminWorkspaceLayout from "../layouts/AdminWorkspaceLayout.vue";
import AdminDashboard from "../pages/AdminDashboard.vue";
import DepartmentsPage from "../pages/DepartmentsPage.vue";
import DoctorsPage from "../pages/DoctorsPage.vue";
import DrugsPage from "../pages/DrugsPage.vue";
import KnowledgePage from "../pages/KnowledgePage.vue";
import PromptsPage from "../pages/PromptsPage.vue";
import DictsPage from "../pages/DictsPage.vue";
import SchedulePage from "../pages/SchedulePage.vue";
import TriageDeskPage from "../pages/TriageDeskPage.vue";
import SearchPage from "../pages/SearchPage.vue";

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
        { path: "search", name: "admin-search", component: SearchPage },
      ],
    },
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
