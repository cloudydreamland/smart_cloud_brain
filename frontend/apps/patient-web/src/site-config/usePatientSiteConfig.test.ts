import { describe, expect, it, vi } from "vitest";
import { api, patientSiteConfigTemplates, resolvePatientSiteConfigSection } from "@smart-cloud-brain/shared-api";
import { defaultPatientSiteConfig } from "./defaultConfig";
import { normalizeConfig, usePatientSiteConfig } from "./usePatientSiteConfig";

vi.mock("@smart-cloud-brain/shared-api", async () => {
  const actual = await vi.importActual<typeof import("@smart-cloud-brain/shared-api")>("@smart-cloud-brain/shared-api");
  return {
    ...actual,
    api: {
      ...actual.api,
      patientSiteConfig: vi.fn(),
    },
  };
});

describe("normalizeConfig", () => {
  it("falls back to default config for empty payloads", () => {
    const config = normalizeConfig({});
    expect(config.nav.menus.length).toBeGreaterThan(0);
    expect(config.home.hero.title).toBeTruthy();
    expect(config.staticPages.pages.length).toBeGreaterThan(0);
  });

  it("keeps every default nav route that is backed by the router whitelist", () => {
    const config = normalizeConfig({});
    const about = config.nav.menus.find((menu) => menu.key === "about");
    expect(about?.links?.some((link) => link.routeName === "public-research")).toBe(true);
  });

  it("filters disabled entries and unknown routes", () => {
    const config = normalizeConfig({
      nav: {
        brand: { name: "Test", homeRoute: "bad-route" },
        menus: [
          {
            key: "x",
            label: "X",
            enabled: true,
            links: [
              { label: "Good", routeName: "patient-home", sort: 20 },
              { label: "Bad", routeName: "bad-route", sort: 10 },
              { label: "Off", routeName: "patient-doctors", enabled: false },
            ],
          },
        ],
      },
      staticPages: {
        pages: [
          { routeName: "bad-route", title: "Bad" },
          { routeName: "about-news", title: "News", enabled: false },
          { routeName: "about-contact", title: "Contact", points: [{ title: "A", text: "B" }] },
        ],
      },
    });

    expect(config.nav.brand.homeRoute).toBe("patient-home");
    expect(config.nav.menus.length).toBeGreaterThan(1);
    const customMenu = config.nav.menus.find((menu) => menu.key === "x");
    expect(customMenu?.links).toHaveLength(1);
    expect(customMenu?.links?.[0].label).toBe("Good");
    expect(config.staticPages.pages.some((page) => page.routeName === "bad-route")).toBe(false);
    expect(config.staticPages.pages.some((page) => page.routeName === "about-news")).toBe(false);
    expect(config.staticPages.pages.some((page) => page.routeName === "about-contact")).toBe(true);
  });

  it("sorts nav links and filters quick action routes", () => {
    const config = normalizeConfig({
      home: {
        hero: { title: "Custom" },
        modules: [
          {
            type: "quick_actions",
            key: "actions",
            content: {
              items: [
                { label: "Second", routeName: "patient-triage", sort: 20 },
                { label: "Bad", routeName: "bad-route", sort: 5 },
                { label: "First", routeName: "patient-doctors", sort: 10 },
              ],
            },
          },
        ],
      },
    });

    const module = config.home.modules.find((item) => item.key === "actions");
    const actions = module?.content?.items as Array<{ label: string }>;
    expect(config.home.hero.title).toBe("Custom");
    expect(actions.map((item) => item.label)).toEqual(["First", "Second"]);
  });

  it("keeps default sections when published config is only a partial patch", () => {
    const config = normalizeConfig({
      nav: {
        menus: [
          {
            key: "home",
            label: "Custom home",
            links: [{ label: "Only custom link", routeName: "patient-home" }],
          },
        ],
      },
      home: {
        modules: [{ type: "notice", key: "emergency_notice", content: { text: "Custom notice" } }],
      },
      staticPages: {
        pages: [{ routeName: "about-contact", title: "Custom contact" }],
      },
    });

    expect(config.nav.menus.length).toBe(patientSiteConfigTemplates.patient_nav.menus.length);
    expect(config.nav.menus.find((menu) => menu.key === "home")?.label).toBe("Custom home");
    expect(config.nav.menus.find((menu) => menu.key === "home")?.links?.[0].label).toBe("Only custom link");
    expect(config.nav.menus.some((menu) => menu.key === "care")).toBe(true);
    expect(config.home.modules.find((module) => module.key === "emergency_notice")?.content?.text).toBe("Custom notice");
    expect(config.home.modules.some((module) => module.key === "quick_actions")).toBe(true);
    expect(config.staticPages.pages.length).toBe(patientSiteConfigTemplates.patient_static_pages.pages.length);
    expect(config.staticPages.pages.find((page) => page.routeName === "about-contact")?.title).toBe("Custom contact");
  });

  it("uses enabled false as explicit disable semantics", () => {
    const rendered = normalizeConfig({
      nav: { menus: [{ key: "care", enabled: false }] },
    });
    const stored = resolvePatientSiteConfigSection(
      "patient_nav",
      { menus: [{ key: "care", enabled: false }] },
      { preserveDisabled: true },
    ) as typeof patientSiteConfigTemplates.patient_nav;

    expect(rendered.nav.menus.some((menu) => menu.key === "care")).toBe(false);
    expect(stored.menus.find((menu) => menu.key === "care")?.enabled).toBe(false);
  });

  it("uses the same full templates as the patient default pages", () => {
    expect(patientSiteConfigTemplates.patient_nav.menus).toEqual(defaultPatientSiteConfig.nav.menus);
    expect(patientSiteConfigTemplates.patient_home.modules).toEqual(defaultPatientSiteConfig.home.modules);
    expect(patientSiteConfigTemplates.patient_static_pages.pages).toEqual(defaultPatientSiteConfig.staticPages.pages);
  });

  it("reloads published config instead of keeping the first response forever", async () => {
    const patientSiteConfig = vi.mocked(api.patientSiteConfig);
    patientSiteConfig
      .mockResolvedValueOnce({ home: { hero: { title: "First version" } } })
      .mockResolvedValueOnce({ home: { hero: { title: "Published version" } } });

    const { config, load } = usePatientSiteConfig();
    await load();
    expect(config.value.home.hero.title).toBe("First version");

    await load();
    expect(patientSiteConfig).toHaveBeenCalledTimes(2);
    expect(config.value.home.hero.title).toBe("Published version");
  });
});
