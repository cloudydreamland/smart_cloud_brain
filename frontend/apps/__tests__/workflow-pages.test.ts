// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from "vitest";
import { flushPromises, shallowMount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import PatientLoginPage from "../patient-web/src/pages/LoginPage.vue";
import TriagePage from "../patient-web/src/pages/TriagePage.vue";
import ConsultationPage from "../doctor-web/src/pages/ConsultationPage.vue";
import AdminAnalyticsSection from "../admin-web/src/components/AdminAnalyticsSection.vue";
import AdminDashboard from "../admin-web/src/pages/AdminDashboard.vue";
import SchedulePage from "../admin-web/src/pages/SchedulePage.vue";
import AccountsPage from "../admin-web/src/pages/AccountsPage.vue";
import DevicesPage from "../admin-web/src/pages/DevicesPage.vue";
import PatientsPage from "../admin-web/src/pages/PatientsPage.vue";
import PermissionsPage from "../admin-web/src/pages/PermissionsPage.vue";
import { api, useDoctorWorkflowStore } from "../../packages/shared-api/src/index";

vi.mock("../admin-web/src/echarts", () => ({
  default: {
    graphic: { LinearGradient: vi.fn(() => ({})) },
    init: vi.fn(() => ({
      setOption: vi.fn(),
      dispose: vi.fn(),
      resize: vi.fn(),
    })),
  },
}));

vi.mock("vue-router", () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ query: {} }),
}));

describe("closed-loop page smoke tests", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
    vi.clearAllMocks();
    vi.stubGlobal("ResizeObserver", class {
      observe() {}
      disconnect() {}
      unobserve() {}
    });
  });

  it("renders patient login", () => {
    const wrapper = shallowMount(PatientLoginPage, { global: { stubs: { RouterLink: true } } });
    expect(wrapper.text()).toContain("确认您的患者身份");
  });

  it("renders patient triage", () => {
    const wrapper = shallowMount(TriagePage);
    expect(wrapper.text()).toContain("智能分诊");
    expect(wrapper.text()).toContain("提交分诊");
  });

  it("keeps patient triage submit available before validation", () => {
    const wrapper = shallowMount(TriagePage);
    expect((wrapper.get("button.primary").element as HTMLButtonElement).disabled).toBe(false);
  });

  it("renders doctor consultation", () => {
    const workflow = useDoctorWorkflowStore();
    workflow.registrations = [{ registrationId: 1, patientId: 1, patientName: "患者" }];
    const wrapper = shallowMount(ConsultationPage, { props: { registrationId: "1" } });
    expect(wrapper.text()).toContain("病历工作区");
    expect(wrapper.text()).toContain("患者分诊");
  });

  it("renders administrator AI schedule", () => {
    const wrapper = shallowMount(SchedulePage);
    expect(wrapper.text()).toContain("排班管理");
    expect(wrapper.text()).toContain("生成建议");
    expect(wrapper.text()).not.toContain("搜索");
  });

  it("renders administrator account permissions", () => {
    const wrapper = shallowMount(AccountsPage);
    expect(wrapper.text()).toContain("账户与权限管理");
    expect(wrapper.text()).toContain("新增账户");
  });

  it("renders administrator device management", () => {
    const wrapper = shallowMount(DevicesPage);
    expect(wrapper.text()).toContain("设备管理");
    expect(wrapper.text()).toContain("新增设备");
  });

  it("renders administrator patient management", () => {
    const wrapper = shallowMount(PatientsPage);
    expect(wrapper.text()).toContain("患者管理");
    expect(wrapper.text()).toContain("搜索");
  });

  it("renders administrator analytics at the bottom of the dashboard", async () => {
    vi.spyOn(api, "statisticsOverview").mockResolvedValue({
      registrations: 18,
      completedRegistrations: 12,
      patients: 9,
      doctors: 3,
      devices: 5,
      deviceWarnings: 1,
    });
    vi.spyOn(api, "statisticsTrend").mockResolvedValue([{ day: "2026-06-28", registrations: 5 }]);
    vi.spyOn(api, "doctorWorkload").mockResolvedValue([{ doctor_name: "张医生", registrations: 5 }]);
    vi.spyOn(api, "patientDistribution").mockResolvedValue({ gender: [{ name: "女", value: 9 }] });
    vi.spyOn(api, "deviceUsageStatistics").mockResolvedValue([{ name: "CT", usage_count: 4, abnormal_count: 1 }]);
    vi.spyOn(api, "statisticsReport").mockResolvedValue([{ metric: "registrations", value: 18 }]);
    Object.defineProperty(URL, "createObjectURL", { configurable: true, value: vi.fn(() => "blob:test") });
    Object.defineProperty(URL, "revokeObjectURL", { configurable: true, value: vi.fn() });
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});

    const dashboard = shallowMount(AdminDashboard, { global: { stubs: { RouterLink: true } } });
    expect(dashboard.findComponent(AdminAnalyticsSection).exists()).toBe(true);

    const wrapper = shallowMount(AdminAnalyticsSection);
    await flushPromises();

    expect(wrapper.text()).toContain("运营数据概览");
    expect(wrapper.text()).toContain("就诊趋势");
    expect(wrapper.text()).toContain("医生工作量");
    expect(wrapper.text()).toContain("患者分布");
    expect(wrapper.text()).toContain("设备使用");
    expect(wrapper.findAll(".analytics-metric-card")).toHaveLength(5);
    expect(wrapper.findAll(".analytics-metric-head").map((item) => item.text())).not.toContain("医生数");
    expect(api.statisticsTrend).toHaveBeenCalledWith(expect.objectContaining({
      startDate: expect.any(String),
      endDate: expect.any(String),
    }));

    const exportButton = wrapper.findAll("button").find((button) => button.text().includes("导出 CSV"));
    await exportButton?.trigger("click");
    await flushPromises();
    expect(api.statisticsReport).toHaveBeenCalled();
    expect(clickSpy).toHaveBeenCalled();
  });

  it("keeps analytics failures and empty states inside the analytics section", async () => {
    vi.spyOn(api, "statisticsOverview").mockRejectedValueOnce(new Error("服务异常"));
    vi.spyOn(api, "statisticsTrend").mockResolvedValue([]);
    vi.spyOn(api, "doctorWorkload").mockResolvedValue([]);
    vi.spyOn(api, "patientDistribution").mockResolvedValue({});
    vi.spyOn(api, "deviceUsageStatistics").mockResolvedValue([]);

    const failed = shallowMount(AdminAnalyticsSection, { global: { stubs: { ErrorState: false } } });
    await flushPromises();
    expect(failed.text()).toContain("统计数据暂不可用");

    vi.mocked(api.statisticsOverview).mockResolvedValueOnce({});
    const empty = shallowMount(AdminAnalyticsSection);
    await flushPromises();
    expect(empty.findAll(".analytics-chart-empty")).toHaveLength(4);
  });

  it("renders administrator permissions", () => {
    const wrapper = shallowMount(PermissionsPage);
    expect(wrapper.text()).toContain("角色权限");
    expect(wrapper.text()).toContain("保存");
  });
});
