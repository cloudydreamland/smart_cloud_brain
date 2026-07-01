import { mount } from "@vue/test-utils";
import { describe, expect, it, vi } from "vitest";
import type { FaqSection } from "@smart-cloud-brain/shared-api";
import FaqSectionEditor from "./FaqSectionEditor.vue";

function faqSection(): FaqSection {
  return {
    id: "faq",
    type: "faq",
    enabled: true,
    sort: 10,
    title: "常见问题",
    items: [{ question: "如何预约？", answer: "通过患者端预约。" }],
  };
}

describe("FaqSectionEditor", () => {
  it("keeps the question when delete confirmation is cancelled", async () => {
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
    const section = faqSection();
    const wrapper = mount(FaqSectionEditor, { props: { section } });

    await wrapper.find("button.danger-link").trigger("click");

    expect(confirm).toHaveBeenCalledWith(expect.stringContaining("确认删除问答"));
    expect(section.items).toHaveLength(1);
    confirm.mockRestore();
  });

  it("removes the question after delete confirmation", async () => {
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(true);
    const section = faqSection();
    const wrapper = mount(FaqSectionEditor, { props: { section } });

    await wrapper.find("button.danger-link").trigger("click");

    expect(section.items).toHaveLength(0);
    confirm.mockRestore();
  });
});
