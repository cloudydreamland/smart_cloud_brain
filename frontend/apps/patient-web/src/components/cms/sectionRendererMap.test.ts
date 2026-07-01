import { describe, expect, it } from "vitest";
import { patientSiteSectionTypes } from "@smart-cloud-brain/shared-api";
import { sectionRendererMap } from "./sectionRendererMap";

describe("sectionRendererMap", () => {
  it("provides one patient renderer component for every CMS section type", () => {
    expect(Object.keys(sectionRendererMap).sort()).toEqual([...patientSiteSectionTypes].sort());
    patientSiteSectionTypes.forEach((type) => {
      expect(sectionRendererMap[type]).toBeTruthy();
    });
  });
});
