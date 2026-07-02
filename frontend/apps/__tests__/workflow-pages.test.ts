// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from "vitest";
import { flushPromises, mount, shallowMount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { nextTick, ref } from "vue";
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
import { useDoctorSlots } from "../patient-web/src/composables/useDoctorSlots";

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
    vi.spyOn(api, "patientVisitors").mockResolvedValue([
      { id: 1, name: "本人", visitorType: "ACCOUNT" },
      { id: 8, name: "小明", visitorType: "VISITOR", relationship: "家属" },
    ]);
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

  it("submits patient triage for selected visitor subject", async () => {
    const triageSpy = vi.spyOn(api, "triage").mockResolvedValue({
      triageRecordId: 10,
      patientId: 1,
      ownerPatientId: 1,
      subjectType: "VISITOR",
      subjectId: 8,
      subjectName: "小明",
      subjectRelationship: "家属",
      status: "AI_RECOMMENDED",
    });
    vi.spyOn(api, "triageList").mockResolvedValue([]);
    vi.spyOn(api, "registrationSlots").mockResolvedValue([]);
    vi.spyOn(api, "registrations").mockResolvedValue([]);
    vi.spyOn(api, "medicalRecords").mockResolvedValue([]);
    vi.spyOn(api, "prescriptions").mockResolvedValue([]);
    vi.spyOn(api, "patientInfo").mockResolvedValue({ id: 1, name: "本人" });
    vi.spyOn(api, "departments").mockResolvedValue([]);
    vi.spyOn(api, "doctors").mockResolvedValue([]);

    const wrapper = mount(TriagePage);
    await flushPromises();
    await wrapper.find("select").setValue("VISITOR:8");
    await wrapper.find("textarea").setValue("发热");
    await wrapper.find("form").trigger("submit.prevent");
    await flushPromises();

    expect(triageSpy).toHaveBeenCalledWith(expect.objectContaining({
      subjectType: "VISITOR",
      subjectId: 8,
    }));
  });

  it("selects latest triage and booked slots by current subject", async () => {
    const state = useDoctorSlots(
      ref([
        { slotId: 1, doctorId: 2, departmentId: 3, departmentName: "心内科", startTime: "2026-07-03T09:00:00", endTime: "2026-07-03T10:00:00", capacity: 1, remainingCapacity: 1, status: "AVAILABLE" },
        { slotId: 2, doctorId: 2, departmentId: 4, departmentName: "儿科", startTime: "2026-07-03T10:00:00", endTime: "2026-07-03T11:00:00", capacity: 1, remainingCapacity: 1, status: "AVAILABLE" },
      ]),
      ref([
        { subjectType: "ACCOUNT", subjectId: 1, appointmentTime: "2026-07-03T09:00:00", status: "CREATED" },
        { subjectType: "VISITOR", subjectId: 8, appointmentTime: "2026-07-03T10:00:00", status: "CREATED" },
      ]),
      ref([{ id: 3, code: "CARD", name: "心内科" }, { id: 4, code: "PED", name: "儿科" }]),
      ref({ triageRecordId: 2, subjectType: "ACCOUNT", subjectId: 1, recommendedDepartment: "心内科" }),
      ref([
        { triageRecordId: 2, subjectType: "ACCOUNT", subjectId: 1, recommendedDepartment: "心内科" },
        { triageRecordId: 5, subjectType: "VISITOR", subjectId: 8, recommendedDepartment: "儿科" },
      ]),
    );
    state.visitors.value = [
      { id: 1, name: "本人", visitorType: "ACCOUNT" },
      { id: 8, name: "小明", visitorType: "VISITOR", relationship: "家属" },
    ];
    state.selectedVisitorKey.value = "VISITOR:8";
    await nextTick();

    expect(state.selectedTriage.value?.triageRecordId).toBe(5);
    expect(state.recommendedDepartment.value).toBe("儿科");
    expect(state.isSlotBooked({ slotId: 2, startTime: "2026-07-03T10:00:00", endTime: "", doctorId: 2, departmentId: 4, capacity: 1, remainingCapacity: 1, status: "AVAILABLE" })).toBe(true);
    expect(state.isSlotBooked({ slotId: 1, startTime: "2026-07-03T09:00:00", endTime: "", doctorId: 2, departmentId: 3, capacity: 1, remainingCapacity: 1, status: "AVAILABLE" })).toBe(false);
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
    expect(wrapper.find("input").attributes("placeholder")).toContain("搜索");
  });

  it("renders administrator analytics at the bottom of the dashboard", async () => {
    const dashboard = shallowMount(AdminDashboard, { global: { stubs: { RouterLink: true } } });
    expect(dashboard.findComponent(AdminAnalyticsSection).exists()).toBe(true);

    const analyticsState = {
      loaded: ref(true),
      trend: ref([{ day: "2026-06-28", registrations: 5 }]),
      workload: ref([{ doctorName: "张医生", registrations: 5 }]),
      distribution: ref({ gender: [{ name: "女", value: 9 }] }),
      deviceUsage: ref([{ name: "CT", usageCount: 4, abnormalCount: 1 }]),
    };
    const wrapper = shallowMount(AdminAnalyticsSection, {
      global: { provide: { adminAnalytics: analyticsState } },
    });
    await flushPromises();

    expect(wrapper.text()).toContain("就诊趋势");
    expect(wrapper.text()).toContain("医生工作量");
    expect(wrapper.text()).toContain("患者分布");
    expect(wrapper.text()).toContain("设备使用");
    expect(wrapper.findAll(".analytics-chart-empty")).toHaveLength(0);
  });

  it("keeps analytics failures and empty states inside the analytics section", async () => {
    const provideAnalytics = (overrides = {}) => ({
      global: {
        provide: {
          adminAnalytics: {
            loaded: ref(true),
            trend: ref([]),
            workload: ref([]),
            distribution: ref({}),
            deviceUsage: ref([]),
            ...overrides,
          },
        },
      },
    });

    const loaded = shallowMount(AdminAnalyticsSection, provideAnalytics());
    await flushPromises();
    expect(loaded.findAll(".analytics-chart-empty")).toHaveLength(4);
    expect(loaded.text()).toContain("暂无就诊趋势数据");
    expect(loaded.text()).toContain("暂无医生工作量数据");

    const notLoaded = shallowMount(AdminAnalyticsSection, provideAnalytics({ loaded: ref(false) }));
    await flushPromises();
    expect(notLoaded.findAll(".analytics-chart-empty")).toHaveLength(0);
  });

  it("renders administrator permissions", () => {
    const wrapper = shallowMount(PermissionsPage);
    expect(wrapper.text()).toContain("角色权限");
    expect(wrapper.text()).toContain("保存");
  });
});
