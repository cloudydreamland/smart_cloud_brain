import { mount, shallowMount } from "@vue/test-utils";
import { nextTick } from "vue";
import { afterEach, describe, expect, it } from "vitest";
import ConfirmDialog from "../components/ConfirmDialog.vue";
import DataTable from "../components/DataTable.vue";
import DatePicker from "../components/DatePicker.vue";
import Drawer from "../components/Drawer.vue";
import EmptyState from "../components/EmptyState.vue";
import ErrorState from "../components/ErrorState.vue";
import LoadingState from "../components/LoadingState.vue";
import Modal from "../components/Modal.vue";
import StatusTag from "../components/StatusTag.vue";
import TimeRangePicker from "../components/TimeRangePicker.vue";

describe("shared ui components", () => {
  afterEach(() => {
    document.body.innerHTML = "";
  });

  it("renders status, empty, error and loading states", () => {
    expect(mount(StatusTag, { props: { status: "CUSTOM", tone: "warning" } }).text()).toContain("CUSTOM");
    expect(mount(StatusTag, { props: { status: "" } }).text()).toContain("-");
    expect(mount(EmptyState, { props: { title: "No rows", message: "Try later" } }).text()).toContain("No rows");
    expect(mount(ErrorState, { props: { title: "Bad", message: "Failed" } }).text()).toContain("Failed");
    expect(mount(LoadingState, { props: { title: "Loading", message: "Wait" } }).text()).toContain("Wait");
  });

  it("switches data table states and renders slot content", () => {
    expect(mount(DataTable, { props: { rows: [], loading: true, loadingTitle: "Loading" } }).text()).toContain("Loading");
    expect(mount(DataTable, { props: { rows: [], error: "Failed" } }).text()).toContain("Failed");
    expect(mount(DataTable, { props: { rows: [], emptyTitle: "Empty" } }).text()).toContain("Empty");

    const table = mount(DataTable, {
      props: { rows: [{ id: 1 }] },
      slots: { default: "<tbody><tr><td>Alice</td></tr></tbody>" },
    });
    expect(table.find("table").exists()).toBe(true);
    expect(table.text()).toContain("Alice");
  });

  it("emits modal and drawer close events", async () => {
    const modal = mount(Modal, {
      attachTo: document.body,
      props: { open: true, title: "Edit", description: "Details" },
      slots: { default: "Body", footer: "<button>Save</button>" },
    });
    expect(document.body.textContent).toContain("Body");
    (document.body.querySelector("button") as HTMLButtonElement).click();
    await nextTick();
    expect(modal.emitted("close")).toHaveLength(1);
    modal.unmount();

    const drawer = mount(Drawer, {
      attachTo: document.body,
      props: { open: true, title: "Panel" },
      slots: { default: "Drawer body" },
    });
    expect(document.body.textContent).toContain("Drawer body");
    (document.body.querySelector("button") as HTMLButtonElement).click();
    await nextTick();
    expect(drawer.emitted("close")).toHaveLength(1);
  });

  it("emits confirm dialog actions", async () => {
    const wrapper = shallowMount(ConfirmDialog, {
      props: { open: true, title: "Delete", message: "Sure?", confirmText: "Delete" },
      global: {
        stubs: {
          Modal: {
            template: "<div><slot /><slot name='footer' /></div>",
          },
        },
      },
    });

    const buttons = wrapper.findAll("button");
    await buttons[0].trigger("click");
    await buttons[1].trigger("click");
    expect(wrapper.emitted("close")).toHaveLength(1);
    expect(wrapper.emitted("confirm")).toHaveLength(1);
  });

  it("selects only dates inside the configured range and respects clearable", async () => {
    const wrapper = mount(DatePicker, {
      attachTo: document.body,
      props: {
        modelValue: "2026-06-22",
        min: "2026-06-22",
        max: "2026-06-24",
        clearable: false,
      },
    });

    await wrapper.get(".scb-date-picker-trigger").trigger("click");
    const beforeRange = document.body.querySelector<HTMLButtonElement>('[aria-label="2026-06-21"]');
    const validDay = document.body.querySelector<HTMLButtonElement>('[aria-label="2026-06-23"]');
    expect(beforeRange?.disabled).toBe(true);
    expect(document.body.textContent).not.toContain("清除");
    validDay?.click();
    await nextTick();
    expect(wrapper.emitted("update:modelValue")?.at(-1)).toEqual(["2026-06-23"]);
    wrapper.unmount();
  });

  it("chooses preset and custom time ranges without emitting an invalid order", async () => {
    const wrapper = mount(TimeRangePicker, {
      attachTo: document.body,
      props: { modelValue: "09:00-12:00", stepMinutes: 30 },
    });

    await wrapper.get(".scb-time-range-trigger").trigger("click");
    const afternoon = Array.from(document.body.querySelectorAll<HTMLButtonElement>(".scb-time-range-presets button"))
      .find((button) => button.textContent?.includes("下午门诊"));
    afternoon?.click();
    await nextTick();
    expect(wrapper.emitted("update:modelValue")?.at(-1)).toEqual(["14:00-17:00"]);

    await wrapper.setProps({ modelValue: "09:00-12:00" });
    await wrapper.get(".scb-time-range-trigger").trigger("click");
    const start = document.body.querySelector<HTMLSelectElement>('select[aria-label="开始时间"]');
    await wrapper.vm.$nextTick();
    if (!start) throw new Error("开始时间选择器未渲染");
    start.value = "12:00";
    start.dispatchEvent(new Event("change"));
    await nextTick();
    const emitted = String(wrapper.emitted("update:modelValue")?.at(-1)?.[0]);
    const [startValue, endValue] = emitted.split("-");
    expect(startValue).toBe("12:00");
    expect(endValue > startValue).toBe(true);
    wrapper.unmount();
  });
});
