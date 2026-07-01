// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { flushPromises, mount, shallowMount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { defineComponent, nextTick, ref } from "vue";
import {
  api,
  doctorApi,
  useAuthStore,
  useDoctorWorkflowStore,
  type MedicalRecord,
  type DrugItem,
  type Registration,
} from "../../packages/shared-api/src/index";
import DatePicker from "../../packages/shared-ui/src/components/DatePicker.vue";
import AdminAnalyticsOverview from "../admin-web/src/components/AdminAnalyticsOverview.vue";
import DevicesPage from "../admin-web/src/pages/DevicesPage.vue";
import ConsultationPage from "../doctor-web/src/pages/ConsultationPage.vue";
import HighRiskConfirmModal from "../doctor-web/src/components/HighRiskConfirmModal.vue";
import DrugCatalogSelect from "../doctor-web/src/components/DrugCatalogSelect.vue";
import DoctorWorkspaceLayout from "../doctor-web/src/layouts/DoctorWorkspaceLayout.vue";
import RecordsPage from "../doctor-web/src/pages/RecordsPage.vue";
import DoctorSettingsPage from "../doctor-web/src/pages/DoctorSettingsPage.vue";
import { loadDoctorSettings } from "../doctor-web/src/composables/useDoctorSettings";

vi.mock("vue-router", () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ path: "/", name: "doctor-dashboard", query: {} }),
}));

const ModalStub = defineComponent({
  name: "Modal",
  template: "<div><slot /><slot name='footer' /></div>",
});

const ToastStub = defineComponent({
  name: "Toast",
  template: "<div />",
  methods: {
    success: vi.fn(),
    error: vi.fn(),
    info: vi.fn(),
  },
});

function seedDoctorSettings(value: Record<string, unknown>) {
  localStorage.setItem("doctor-settings", JSON.stringify(value));
  loadDoctorSettings();
}

function mockDoctorRefreshApis() {
  vi.spyOn(doctorApi, "registrations").mockResolvedValue([]);
  vi.spyOn(doctorApi, "triageList").mockResolvedValue([]);
  vi.spyOn(doctorApi, "medicalRecords").mockResolvedValue([]);
  vi.spyOn(doctorApi, "prescriptions").mockResolvedValue([]);
  vi.spyOn(doctorApi, "notifications").mockResolvedValue([]);
}

const sampleDrugCatalog = [
  {
    id: 1,
    name: "阿莫西林胶囊",
    specification: "0.5g",
    contraindication: "青霉素过敏禁用",
    interactionRule: "避免与同类抗生素重复",
    status: "ENABLED",
  },
  {
    id: 2,
    name: "氨溴索片",
    specification: "30mg",
    status: "ENABLED",
  },
];

function mockDrugCatalog(rows = sampleDrugCatalog) {
  vi.spyOn(api, "doctorDrugs").mockResolvedValue(rows);
}

describe("admin shared date picker adoption", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.restoreAllMocks();
    localStorage.clear();
  });

  afterEach(() => {
    document.body.innerHTML = "";
  });

  it("uses the shared date picker in analytics filters", () => {
    const wrapper = shallowMount(AdminAnalyticsOverview, {
      global: {
        provide: {
          adminAnalytics: {
            loading: ref(false),
            exporting: ref(false),
            loaded: ref(true),
            error: ref(""),
            startDate: ref("2026-06-16"),
            endDate: ref("2026-06-30"),
            metricCards: ref([]),
            refresh: vi.fn(),
            exportCsv: vi.fn(),
          },
        },
      },
    });

    expect(wrapper.findAllComponents(DatePicker)).toHaveLength(2);
    expect(wrapper.find('input[type="date"]').exists()).toBe(false);
  });

  it("uses the shared date picker for device purchase dates while keeping the field optional", async () => {
    vi.spyOn(api, "devices").mockResolvedValue([]);
    const wrapper = shallowMount(DevicesPage, {
      global: {
        provide: { toast: ref(null) },
        stubs: { Modal: ModalStub, RowActionMenu: true },
      },
    });
    await flushPromises();

    const purchaseDate = wrapper.findAllComponents(DatePicker)
      .find((picker) => picker.props("ariaLabel") === "采购日期");
    expect(purchaseDate?.props("clearable")).not.toBe(false);
    expect(wrapper.find('input[type="date"]').exists()).toBe(false);
  });
});

describe("doctor settings behavior", () => {
  const registration: Registration = {
    registrationId: 1,
    patientId: 2,
    patientName: "测试患者",
    status: "IN_PROGRESS",
  };

  beforeEach(() => {
    setActivePinia(createPinia());
    vi.restoreAllMocks();
    localStorage.clear();
    mockDoctorRefreshApis();
  });

  afterEach(() => {
    document.body.innerHTML = "";
  });

  function mountConsultation() {
    mockDrugCatalog();
    const workflow = useDoctorWorkflowStore();
    workflow.registrations = [registration];
    return shallowMount(ConsultationPage, {
      props: { registrationId: "1" },
      global: {
        stubs: { Toast: ToastStub },
      },
    });
  }

  it("keeps the high-risk confirmation modal when the setting is enabled", async () => {
    seedDoctorSettings({ highRiskConfirm: true });
    const createSpy = vi.spyOn(api, "createPrescription").mockResolvedValue({});
    const wrapper = mountConsultation();
    await flushPromises();

    const vm = wrapper.vm as unknown as {
      prescription: { prescription: { medicalRecordId: number; riskLevel: string; drugs: DrugItem[] } };
    };
    vm.prescription.prescription.medicalRecordId = 8;
    vm.prescription.prescription.riskLevel = "HIGH";
    vm.prescription.prescription.drugs[0] = { drugName: "阿莫西林胶囊", dosage: "0.5g", frequency: "tid", usageMethod: "口服" };
    await nextTick();

    const createButton = wrapper.findAll("button").find((button) => button.text() === "创建处方");
    await createButton?.trigger("click");
    await nextTick();

    expect(createSpy).not.toHaveBeenCalled();
    expect(wrapper.findComponent(HighRiskConfirmModal).props("open")).toBe(true);
  });

  it("bypasses the high-risk confirmation modal when the setting is disabled", async () => {
    seedDoctorSettings({ highRiskConfirm: false });
    const createSpy = vi.spyOn(api, "createPrescription").mockResolvedValue({});
    const wrapper = mountConsultation();
    await flushPromises();

    const vm = wrapper.vm as unknown as {
      prescription: { prescription: { medicalRecordId: number; riskLevel: string; drugs: DrugItem[] } };
    };
    vm.prescription.prescription.medicalRecordId = 8;
    vm.prescription.prescription.riskLevel = "HIGH";
    vm.prescription.prescription.drugs[0] = { drugName: "阿莫西林胶囊", dosage: "0.5g", frequency: "tid", usageMethod: "口服" };
    await nextTick();

    const createButton = wrapper.findAll("button").find((button) => button.text() === "创建处方");
    await createButton?.trigger("click");
    await flushPromises();

    expect(createSpy).toHaveBeenCalled();
    expect(wrapper.findComponent(HighRiskConfirmModal).props("open")).toBe(false);
  });

  it("keeps one primary AI generation entry inside the prompt input", async () => {
    seedDoctorSettings({ aiDraftMode: "preview" });
    const wrapper = mountConsultation();
    await flushPromises();

    const generateButtons = wrapper.findAll("button").filter((button) => button.text().includes("生成病历"));
    expect(generateButtons).toHaveLength(1);
    expect(wrapper.text()).toContain("病历工作区");
    expect(wrapper.text()).toContain("处方与风险");
    expect(wrapper.text()).not.toContain("药品目录选择");
    expect(wrapper.text()).not.toContain("仅可选择管理端已维护");
    expect(wrapper.text()).not.toContain("可选 30 种目录药品");
    expect(wrapper.findComponent(DrugCatalogSelect).props("placeholder")).toBe("搜索药品");

    const deleteButton = wrapper.findAll("button").find((button) => button.text() === "×");
    expect(deleteButton?.attributes("aria-label")).toBe("删除第 1 行药品");
  });

  it("requires prescription drugs to come from the doctor drug catalog without calling admin drugs", async () => {
    seedDoctorSettings({ highRiskConfirm: false });
    const adminDrugSpy = vi.spyOn(api, "drugs").mockResolvedValue([]);
    const createSpy = vi.spyOn(api, "createPrescription").mockResolvedValue({});
    const wrapper = mountConsultation();
    await flushPromises();

    const vm = wrapper.vm as unknown as {
      prescription: {
        prescription: { medicalRecordId: number; riskLevel: string; drugs: Array<{ drugName: string; dosage: string; frequency: string; usageMethod: string }> };
      };
    };
    vm.prescription.prescription.medicalRecordId = 8;
    vm.prescription.prescription.riskLevel = "LOW";
    vm.prescription.prescription.drugs[0] = { drugName: "目录外药品", dosage: "1片", frequency: "bid", usageMethod: "口服" };
    await nextTick();

    await wrapper.findAll("button").find((button) => button.text() === "创建处方")?.trigger("click");
    await flushPromises();
    expect(createSpy).not.toHaveBeenCalled();

    vm.prescription.prescription.drugs[0].drugName = "阿莫西林胶囊";
    await nextTick();
    await wrapper.findAll("button").find((button) => button.text() === "创建处方")?.trigger("click");
    await flushPromises();
    expect(createSpy).toHaveBeenCalled();
    expect(adminDrugSpy).not.toHaveBeenCalled();
  });

  it("shows catalog unavailable state when the drug catalog is empty", async () => {
    vi.spyOn(api, "doctorDrugs").mockResolvedValue([]);
    const workflow = useDoctorWorkflowStore();
    workflow.registrations = [registration];
    const wrapper = shallowMount(ConsultationPage, {
      props: { registrationId: "1" },
      global: { stubs: { Toast: ToastStub } },
    });
    await flushPromises();

    const selector = wrapper.findAllComponents(DrugCatalogSelect)[0];
    expect(wrapper.text()).not.toContain("药品目录选择");
    expect(selector.props("placeholder")).toBe("暂无可用药品");
    expect(selector.props("emptyMessage")).toContain("暂无可用药品");
    expect(selector.props("disabled")).toBe(false);
  });

  it("shows a friendly catalog loading error instead of raw forbidden text", async () => {
    vi.spyOn(api, "doctorDrugs").mockRejectedValue(new Error("forbidden"));
    const workflow = useDoctorWorkflowStore();
    workflow.registrations = [registration];
    const wrapper = shallowMount(ConsultationPage, {
      props: { registrationId: "1" },
      global: { stubs: { Toast: ToastStub } },
    });
    await flushPromises();

    const selector = wrapper.findAllComponents(DrugCatalogSelect)[0];
    expect(selector.props("placeholder")).toBe("目录加载失败");
    expect(selector.props("emptyMessage")).toBe("药品目录加载失败，请稍后重试。");
    expect(selector.props("emptyActionLabel")).toBe("重新加载");
    expect(wrapper.text()).not.toContain("forbidden");
    expect(selector.props("disabled")).toBe(false);

    const riskButton = wrapper.findAll("button").find((button) => button.text() === "风险审核");
    const createButton = wrapper.findAll("button").find((button) => button.text() === "创建处方");
    expect(riskButton?.attributes("disabled")).toBeDefined();
    expect(createButton?.attributes("disabled")).toBeDefined();
  });
});

describe("doctor drug catalog selector", () => {
  it("renders searchable catalog options and emits the selected drug", async () => {
    const wrapper = mount(DrugCatalogSelect, {
      props: {
        modelValue: "",
        drugs: sampleDrugCatalog,
      },
    });

    await wrapper.get("input").trigger("focus");
    await nextTick();

    expect(document.body.textContent).toContain("阿莫西林胶囊");
    expect(document.body.textContent).toContain("青霉素过敏禁用");

    (document.body.querySelector(".drug-option") as HTMLButtonElement).click();
    await nextTick();

    expect(wrapper.emitted("update:modelValue")?.[0]).toEqual(["阿莫西林胶囊"]);
    expect(wrapper.emitted("select")?.[0]?.[0]).toMatchObject({ name: "阿莫西林胶囊", specification: "0.5g" });
    wrapper.unmount();
  });

  it("shows the complete catalog when reopening a selected drug and only filters after typing", async () => {
    const drugs = Array.from({ length: 30 }, (_, index) => ({
      id: index + 1,
      name: `目录药品${String(index + 1).padStart(2, "0")}`,
      specification: `${index + 1}mg`,
      contraindication: `第${index + 1}项禁忌`,
      status: "启用",
    }));
    const wrapper = mount(DrugCatalogSelect, {
      props: {
        modelValue: "目录药品01",
        drugs,
      },
    });

    await wrapper.get("input").trigger("focus");
    await nextTick();

    expect(document.body.querySelectorAll(".drug-option")).toHaveLength(30);
    expect(wrapper.find(".drug-selected-meta").exists()).toBe(false);

    await wrapper.get("input").setValue("目录药品2");
    await nextTick();

    expect(document.body.querySelectorAll(".drug-option")).toHaveLength(10);
    expect(document.body.textContent).toContain("目录药品20");
    expect(document.body.textContent).not.toContain("目录药品01");
    wrapper.unmount();
  });

  it("keeps retry inside the selector empty state", async () => {
    const wrapper = mount(DrugCatalogSelect, {
      props: {
        modelValue: "",
        drugs: [],
        emptyMessage: "药品目录加载失败，请稍后重试。",
        emptyActionLabel: "重新加载",
      },
    });

    await wrapper.get("input").trigger("focus");
    await nextTick();
    expect(document.body.textContent).toContain("药品目录加载失败，请稍后重试。");

    (document.body.querySelector(".drug-option-retry") as HTMLButtonElement).click();
    await nextTick();
    expect(wrapper.emitted("retry")).toHaveLength(1);
    wrapper.unmount();
  });
});

describe("doctor records visible workflow", () => {
  const sampleRecords: MedicalRecord[] = [
    {
      medicalRecordId: 1,
      patientId: 1,
      patientName: "陈明",
      chiefComplaint: "胸痛1小时",
      diagnosis: "急性冠脉综合征待排",
      createdAt: "2026-06-24T23:34:00",
      aiGenerated: false,
    },
    {
      medicalRecordId: 7,
      patientId: 2,
      patientName: "何秀英",
      chiefComplaint: "头晕、恶心",
      diagnosis: "高血压3级",
      createdAt: "2026-06-24T16:02:00",
      aiGenerated: true,
    },
  ];

  beforeEach(() => {
    setActivePinia(createPinia());
    vi.restoreAllMocks();
    localStorage.clear();
  });

  afterEach(() => {
    document.body.innerHTML = "";
  });

  function mountRecordsPage() {
    const workflow = useDoctorWorkflowStore();
    workflow.records = [...sampleRecords];
    vi.spyOn(workflow, "refresh").mockResolvedValue(undefined);
    return shallowMount(RecordsPage, {
      global: {
        provide: { toast: ref(null) },
      },
    });
  }

  it("defaults to compact list view and filters records by source and keyword", async () => {
    const wrapper = mountRecordsPage();
    await nextTick();

    expect(wrapper.find(".card-grid").classes()).toContain("list-view");
    expect(wrapper.text()).toContain("总病历");
    expect(wrapper.text()).toContain("当前结果");
    expect(wrapper.findAll(".record-card")).toHaveLength(2);

    (wrapper.vm as unknown as { sourceFilter: string }).sourceFilter = "ai";
    await nextTick();
    expect(wrapper.findAll(".record-card")).toHaveLength(1);
    expect(wrapper.text()).toContain("何秀英");

    (wrapper.vm as unknown as { keyword: string }).keyword = "胸痛";
    await nextTick();
    expect(wrapper.findAll(".record-card")).toHaveLength(0);

    (wrapper.vm as unknown as { sourceFilter: string }).sourceFilter = "all";
    await nextTick();
    expect(wrapper.findAll(".record-card")).toHaveLength(1);
    expect(wrapper.text()).toContain("陈明");
  });
});

describe("doctor settings visible state", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.restoreAllMocks();
    localStorage.clear();
    loadDoctorSettings();
  });

  afterEach(() => {
    document.body.innerHTML = "";
  });

  it("updates the active effect panel and persists settings", async () => {
    const toast = ref({ success: vi.fn(), error: vi.fn() });
    const wrapper = shallowMount(DoctorSettingsPage, {
      global: {
        provide: { toast },
      },
    });

    expect(wrapper.text()).toContain("当前已生效");
    expect(wrapper.text()).toContain("预览模式");

    await wrapper.findAll(".choicebox-item")[1].trigger("click");
    await nextTick();
    expect(wrapper.text()).toContain("自动填入");

    await wrapper.get('[role="switch"]').trigger("click");
    await nextTick();
    expect(wrapper.text()).toContain("已关闭");

    const notifyButtons = wrapper.findAll(".segmented-control button");
    await notifyButtons[2].trigger("click");
    await nextTick();
    expect(wrapper.text()).toContain("免打扰");

    const saved = JSON.parse(localStorage.getItem("doctor-settings") || "{}");
    expect(saved).toMatchObject({
      aiDraftMode: "auto",
      highRiskConfirm: false,
      notifyMode: "dnd",
    });
    expect(toast.value.success).toHaveBeenCalled();
  });
});

describe("doctor notification mode", () => {
  let WebSocketMock: ReturnType<typeof vi.fn>;
  let intervalSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    setActivePinia(createPinia());
    vi.restoreAllMocks();
    localStorage.clear();
    mockDoctorRefreshApis();
    WebSocketMock = vi.fn().mockImplementation(function MockSocket(this: { close: () => void }, _url: string) {
      this.close = vi.fn();
    });
    vi.stubGlobal("WebSocket", WebSocketMock);
    intervalSpy = vi.spyOn(window, "setInterval").mockReturnValue(1 as unknown as number);
    vi.spyOn(window, "clearInterval").mockImplementation(() => undefined);
    vi.spyOn(window, "setTimeout").mockReturnValue(1 as unknown as number);
    vi.spyOn(window, "clearTimeout").mockImplementation(() => undefined);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    document.body.innerHTML = "";
  });

  async function mountWorkspaceWithMode(mode: "realtime" | "queue" | "dnd") {
    seedDoctorSettings({ notifyMode: mode });
    const auth = useAuthStore();
    auth.save("doctor-session", {
      userId: 1,
      role: "DOCTOR",
      token: "token",
      name: "张医生",
    }, "DOCTOR");
    const wrapper = shallowMount(DoctorWorkspaceLayout, {
      global: {
        stubs: {
          CollapsibleSidebar: true,
          RouterView: true,
          Toast: ToastStub,
        },
      },
    });
    await flushPromises();
    return wrapper;
  }

  it("uses WebSocket in realtime mode", async () => {
    const wrapper = await mountWorkspaceWithMode("realtime");

    expect(WebSocketMock).toHaveBeenCalledTimes(1);
    expect(intervalSpy).not.toHaveBeenCalled();
    wrapper.unmount();
  });

  it("uses polling without WebSocket in queue mode", async () => {
    const wrapper = await mountWorkspaceWithMode("queue");

    expect(WebSocketMock).not.toHaveBeenCalled();
    expect(intervalSpy).toHaveBeenCalled();
    expect(wrapper.text()).toContain("队列刷新");
    wrapper.unmount();
  });

  it("keeps automatic notification refresh off in dnd mode", async () => {
    const wrapper = await mountWorkspaceWithMode("dnd");

    expect(WebSocketMock).not.toHaveBeenCalled();
    expect(intervalSpy).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain("免打扰");
    wrapper.unmount();
  });
});
