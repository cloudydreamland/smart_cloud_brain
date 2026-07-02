import { inject, provide, reactive, type InjectionKey } from "vue";

export type PatientSiteConfirmTone = "primary" | "danger" | "warning";

export type PatientSiteConfirmOptions = {
  title: string;
  message: string;
  confirmText: string;
  tone?: PatientSiteConfirmTone;
};

export type PatientSiteConfirm = (options: PatientSiteConfirmOptions) => Promise<boolean>;

export const patientSiteConfirmKey: InjectionKey<PatientSiteConfirm> = Symbol("patientSiteConfirm");

export function usePatientSiteConfirmHost() {
  let resolvePending: ((confirmed: boolean) => void) | null = null;
  const dialog = reactive({
    open: false,
    title: "",
    message: "",
    confirmText: "确认",
    tone: "primary" as PatientSiteConfirmTone,
  });

  function closeWithResult(confirmed: boolean) {
    dialog.open = false;
    resolvePending?.(confirmed);
    resolvePending = null;
  }

  const confirm: PatientSiteConfirm = (options) => {
    if (resolvePending) closeWithResult(false);
    dialog.title = options.title;
    dialog.message = options.message;
    dialog.confirmText = options.confirmText;
    dialog.tone = options.tone || "primary";
    dialog.open = true;
    return new Promise((resolve) => {
      resolvePending = resolve;
    });
  };

  provide(patientSiteConfirmKey, confirm);

  return {
    dialog,
    confirm,
    confirmDialog: () => closeWithResult(true),
    closeDialog: () => closeWithResult(false),
  };
}

export function usePatientSiteConfirm() {
  return inject<PatientSiteConfirm>(patientSiteConfirmKey, async () => false);
}
