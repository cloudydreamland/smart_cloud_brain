import { computed, onMounted, ref } from "vue";
import {
  adminApi,
  ApiError,
  type DataRow,
  type PatientRecommendation,
  type PatientRecommendationSaveRequest,
  type PatientRecommendationType,
} from "@smart-cloud-brain/shared-api";

type TargetOption = { id: number; label: string };

const emptyRecommendation = (type: PatientRecommendationType): PatientRecommendationSaveRequest => ({
  recommendType: type,
  targetId: 0,
  title: "",
  description: "",
  imageUrl: "",
  imageObjectKey: "",
  sort: 0,
  status: "ENABLED",
});

export function usePatientRecommendationEditor() {
  const activeType = ref<PatientRecommendationType>("DEPARTMENT");
  const recommendations = ref<PatientRecommendation[]>([]);
  const editing = ref<PatientRecommendationSaveRequest | null>(null);
  const departmentTargets = ref<TargetOption[]>([]);
  const doctorTargets = ref<TargetOption[]>([]);
  const loading = ref(false);
  const saving = ref(false);
  const status = ref("");
  const error = ref("");

  const targetOptions = computed(() => (activeType.value === "DEPARTMENT" ? departmentTargets.value : doctorTargets.value));

  async function load() {
    loading.value = true;
    error.value = "";
    try {
      const [rows] = await Promise.all([adminApi.patientSiteRecommendations(activeType.value), loadTargets()]);
      recommendations.value = rows;
    } catch (err) {
      error.value = messageFrom(err);
      recommendations.value = [];
    } finally {
      loading.value = false;
    }
  }

  async function loadTargets() {
    const [departments, doctors] = await Promise.all([
      adminApi.departments().catch(() => []),
      adminApi.doctors().catch(() => []),
    ]);
    departmentTargets.value = departments.map((row) => ({ id: numberValue(row.id), label: textValue(row.name, "未命名科室") })).filter((item) => item.id > 0);
    doctorTargets.value = doctors.map(doctorOption).filter((item) => item.id > 0);
  }

  function switchType(type: PatientRecommendationType) {
    activeType.value = type;
    editing.value = null;
    void load();
  }

  function startCreate() {
    editing.value = emptyRecommendation(activeType.value);
    status.value = "";
    error.value = "";
  }

  function startEdit(row: PatientRecommendation) {
    editing.value = { ...emptyRecommendation(activeType.value), ...row, recommendType: activeType.value };
    status.value = "";
    error.value = "";
  }

  function cancelEdit() {
    editing.value = null;
  }

  async function save() {
    if (!editing.value) return;
    saving.value = true;
    error.value = "";
    try {
      await adminApi.savePatientSiteRecommendation({ ...editing.value, recommendType: activeType.value, sort: Number(editing.value.sort || 0) });
      editing.value = null;
      status.value = "推荐内容已保存，患者端会优先读取启用项";
      await load();
    } catch (err) {
      error.value = messageFrom(err);
    } finally {
      saving.value = false;
    }
  }

  async function remove(id?: number) {
    if (!id || !window.confirm("确认删除该推荐内容？删除后患者端热门科室或推荐医生列表将不再展示该内容。")) return;
    saving.value = true;
    error.value = "";
    try {
      await adminApi.deletePatientSiteRecommendation(id);
      status.value = "推荐内容已删除";
      await load();
    } catch (err) {
      error.value = messageFrom(err);
    } finally {
      saving.value = false;
    }
  }

  async function toggleStatus(row: PatientRecommendation) {
    if (!row.id) return;
    saving.value = true;
    error.value = "";
    try {
      await adminApi.updatePatientSiteRecommendationStatus({
        id: row.id,
        status: row.status === "DISABLED" ? "ENABLED" : "DISABLED",
      });
      await load();
    } catch (err) {
      error.value = messageFrom(err);
    } finally {
      saving.value = false;
    }
  }

  onMounted(load);

  return {
    activeType,
    recommendations,
    editing,
    targetOptions,
    loading,
    saving,
    status,
    error,
    load,
    switchType,
    startCreate,
    startEdit,
    cancelEdit,
    save,
    remove,
    toggleStatus,
  };
}

function doctorOption(row: DataRow): TargetOption {
  const name = textValue(row.name, textValue(row.doctorName, "未命名医生"));
  const department = textValue(row.departmentName, "");
  return { id: numberValue(row.id || row.doctorId), label: department ? `${name} · ${department}` : name };
}

function textValue(value: unknown, fallback: string) {
  return typeof value === "string" && value.trim() ? value.trim() : fallback;
}

function numberValue(value: unknown) {
  return typeof value === "number" && Number.isFinite(value) ? value : Number(value || 0);
}

function messageFrom(err: unknown) {
  return err instanceof ApiError ? err.message : err instanceof Error ? err.message : "操作失败";
}
