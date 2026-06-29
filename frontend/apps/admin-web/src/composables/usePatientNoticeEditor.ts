import { onMounted, ref } from "vue";
import { adminApi, ApiError, type PatientNotice, type PatientNoticeSaveRequest } from "@smart-cloud-brain/shared-api";

const emptyNotice = (): PatientNoticeSaveRequest => ({
  title: "",
  content: "",
  linkType: "NONE",
  linkUrl: "",
  startTime: "",
  endTime: "",
  pinned: false,
  sort: 0,
  status: "ENABLED",
});

export function usePatientNoticeEditor() {
  const notices = ref<PatientNotice[]>([]);
  const editing = ref<PatientNoticeSaveRequest | null>(null);
  const loading = ref(false);
  const saving = ref(false);
  const status = ref("");
  const error = ref("");

  async function load() {
    loading.value = true;
    error.value = "";
    try {
      notices.value = await adminApi.patientSiteNotices();
    } catch (err) {
      error.value = messageFrom(err);
      notices.value = [];
    } finally {
      loading.value = false;
    }
  }

  function startCreate() {
    editing.value = emptyNotice();
    status.value = "";
    error.value = "";
  }

  function startEdit(notice: PatientNotice) {
    editing.value = { ...emptyNotice(), ...notice };
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
      await adminApi.savePatientSiteNotice(normalizeNotice(editing.value));
      editing.value = null;
      status.value = "公告已保存，患者端公开接口会自动过滤禁用和过期公告";
      await load();
    } catch (err) {
      error.value = messageFrom(err);
    } finally {
      saving.value = false;
    }
  }

  async function remove(id?: number) {
    if (!id || !window.confirm("确认删除该公告？")) return;
    saving.value = true;
    error.value = "";
    try {
      await adminApi.deletePatientSiteNotice(id);
      status.value = "公告已删除";
      await load();
    } catch (err) {
      error.value = messageFrom(err);
    } finally {
      saving.value = false;
    }
  }

  async function toggleStatus(notice: PatientNotice) {
    if (!notice.id) return;
    saving.value = true;
    error.value = "";
    try {
      await adminApi.updatePatientSiteNoticeStatus({
        id: notice.id,
        status: notice.status === "DISABLED" ? "ENABLED" : "DISABLED",
      });
      await load();
    } catch (err) {
      error.value = messageFrom(err);
    } finally {
      saving.value = false;
    }
  }

  onMounted(load);

  return { notices, editing, loading, saving, status, error, load, startCreate, startEdit, cancelEdit, save, remove, toggleStatus };
}

function normalizeNotice(notice: PatientNoticeSaveRequest): PatientNoticeSaveRequest {
  return {
    ...notice,
    linkUrl: notice.linkUrl?.trim() || "",
    startTime: notice.startTime || undefined,
    endTime: notice.endTime || undefined,
    sort: Number(notice.sort || 0),
  };
}

function messageFrom(err: unknown) {
  return err instanceof ApiError ? err.message : err instanceof Error ? err.message : "操作失败";
}
