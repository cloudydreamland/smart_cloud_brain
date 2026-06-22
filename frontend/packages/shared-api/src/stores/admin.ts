import { defineStore } from "pinia";
import { ref } from "vue";
import { adminApi } from "../api";
import { formatApiError } from "../formatters";
import type { DataRow } from "../types";

export const useAdminWorkflowStore = defineStore("adminWorkflow", () => {
  const departments = ref<DataRow[]>([]);
  const doctors = ref<DataRow[]>([]);
  const drugs = ref<DataRow[]>([]);
  const prompts = ref<DataRow[]>([]);
  const knowledge = ref<DataRow[]>([]);
  const dicts = ref<DataRow[]>([]);
  const suggestions = ref<DataRow[]>([]);
  const schedules = ref<DataRow[]>([]);
  const triageDesk = ref<DataRow[]>([]);
  const selectedScheduleSuggestion = ref<DataRow | null>(null);
  const selectedTriage = ref<DataRow | null>(null);
  const refreshErrors = ref<Record<string, string>>({});

  async function refresh(token: string) {
    const nextErrors: Record<string, string> = {};
    const keepCurrent = <T>(key: string, fallbackMessage: string, current: T) => (error: unknown) => {
      nextErrors[key] = formatApiError(error, fallbackMessage);
      return current;
    };
    const [departmentList, doctorList, drugList, promptList, knowledgeList, dictList, scheduleList, triageList] = await Promise.all([
      adminApi.departments(token).catch(keepCurrent("departments", "科室列表加载失败", departments.value)),
      adminApi.publicDoctors().catch(keepCurrent("doctors", "医生列表加载失败", doctors.value)),
      adminApi.drugs(token).catch(keepCurrent("drugs", "药品列表加载失败", drugs.value)),
      adminApi.prompts(token).catch(keepCurrent("prompts", "提示词模板加载失败", prompts.value)),
      adminApi.knowledgeEntries(token).catch(keepCurrent("knowledge", "知识库加载失败", knowledge.value)),
      adminApi.dicts(token).catch(keepCurrent("dicts", "字典加载失败", dicts.value)),
      adminApi.schedules(token).catch(keepCurrent("schedules", "排班列表加载失败", schedules.value)),
      adminApi.triageDesk(token).catch(keepCurrent("triageDesk", "分诊台加载失败", [] as DataRow[])),
    ]);
    departments.value = departmentList;
    doctors.value = doctorList;
    drugs.value = drugList;
    prompts.value = promptList;
    knowledge.value = knowledgeList;
    dicts.value = dictList;
    schedules.value = scheduleList;
    triageDesk.value = triageList;
    refreshErrors.value = nextErrors;
  }

  return { departments, doctors, drugs, prompts, knowledge, dicts, suggestions, schedules, triageDesk, selectedScheduleSuggestion, selectedTriage, refreshErrors, refresh };
});
