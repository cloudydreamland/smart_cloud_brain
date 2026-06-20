import { mount, shallowMount } from "@vue/test-utils";
import { nextTick } from "vue";
import { afterEach, describe, expect, it } from "vitest";
import ConfirmDialog from "../components/ConfirmDialog.vue";
import DataTable from "../components/DataTable.vue";
import Drawer from "../components/Drawer.vue";
import EmptyState from "../components/EmptyState.vue";
import ErrorState from "../components/ErrorState.vue";
import LoadingState from "../components/LoadingState.vue";
import Modal from "../components/Modal.vue";
import StatusTag from "../components/StatusTag.vue";

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
});
