import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import type { RouteTargetConfig } from "@smart-cloud-brain/shared-api";
import RouteTargetEditor from "./RouteTargetEditor.vue";

describe("RouteTargetEditor", () => {
  it("edits the slug when a route target points to a CMS page", async () => {
    const model: RouteTargetConfig = {
      label: "医院专题",
      routeName: "cms-page",
      slug: "",
      enabled: true,
      sort: 10,
    };

    const wrapper = mount(RouteTargetEditor, {
      props: {
        model,
        prefix: "link",
        patientRouteOptions: [
          { name: "patient-home", label: "首页" },
          { name: "cms-page", label: "CMS 页面" },
        ],
        includeSort: true,
        includeEnabled: true,
      },
      global: {
        stubs: {
          ScbSelect: { props: ["modelValue", "options"], template: "<select />" },
        },
      },
    });

    const slugInput = wrapper.find('input[placeholder="例如：hospital-guide"]');
    expect(slugInput.exists()).toBe(true);
    await slugInput.setValue("hospital-intro");

    expect(model.slug).toBe("hospital-intro");
  });
});
