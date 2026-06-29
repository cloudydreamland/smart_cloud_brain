import { defineStore } from "pinia";
import { ref } from "vue";
import { adminApi } from "../api";
import { formatApiError } from "../formatters";
import type {
  AiLog,
  Department,
  Doctor,
  Drug,
  KnowledgeEntry,
  PromptTemplate,
  Schedule,
  ScheduleSuggestion,
  SystemDict,
  TriageRecord,
} from "../types";

export const useAdminWorkflowStore = defineStore("adminWorkflow", () => {
  const departments = ref<Department[]>([]);
  const doctors = ref<Doctor[]>([]);
  const drugs = ref<Drug[]>([]);
  const prompts = ref<PromptTemplate[]>([]);
  const knowledge = ref<KnowledgeEntry[]>([]);
  const dicts = ref<SystemDict[]>([]);
  const suggestions = ref<ScheduleSuggestion[]>([]);
  const schedules = ref<Schedule[]>([]);
  const triageDesk = ref<TriageRecord[]>([]);
  const aiLogs = ref<AiLog[]>([]);
  const selectedScheduleSuggestion = ref<ScheduleSuggestion | null>(null);
  const selectedTriage = ref<TriageRecord | null>(null);
  const refreshErrors = ref<Record<string, string>>({});

  async function refresh() {
    const nextErrors: Record<string, string> = {};
    const keepCurrent = <T>(key: string, fallbackMessage: string, current: T) => (error: unknown) => {
      nextErrors[key] = formatApiError(error, fallbackMessage);
      return current;
    };
    const [departmentList, doctorList, drugList, promptList, knowledgeList, dictList, scheduleList, triageList, aiLogList] = await Promise.all([
      adminApi.departments().catch(keepCurrent("departments", "科室列表加载失败", departments.value)),
      adminApi.doctors().catch(keepCurrent("doctors", "医生列表加载失败", doctors.value)),
      adminApi.drugs().catch(keepCurrent("drugs", "药品列表加载失败", drugs.value)),
      adminApi.prompts().catch(keepCurrent("prompts", "提示词模板加载失败", prompts.value)),
      adminApi.knowledgeEntries().catch(keepCurrent("knowledge", "知识库加载失败", knowledge.value)),
      adminApi.dicts().catch(keepCurrent("dicts", "字典加载失败", dicts.value)),
      adminApi.schedules().catch(keepCurrent("schedules", "排班列表加载失败", schedules.value)),
      adminApi.triageDesk().catch(keepCurrent("triageDesk", "分诊台加载失败", [] as TriageRecord[])),
      adminApi.aiLogs().catch(keepCurrent("aiLogs", "AI 日志加载失败", aiLogs.value)),
    ]);
    departments.value = departmentList as Department[];
    doctors.value = doctorList as Doctor[];
    drugs.value = drugList as Drug[];
    prompts.value = promptList as PromptTemplate[];
    knowledge.value = knowledgeList as KnowledgeEntry[];
    dicts.value = dictList as SystemDict[];
    schedules.value = scheduleList as Schedule[];
    triageDesk.value = triageList as TriageRecord[];
    aiLogs.value = aiLogList as AiLog[];
    refreshErrors.value = nextErrors;
  }

  return { departments, doctors, drugs, prompts, knowledge, dicts, suggestions, schedules, triageDesk, aiLogs, selectedScheduleSuggestion, selectedTriage, refreshErrors, refresh };
});
