import { ref } from "vue";
import { adminApi, ApiError, type AssetUploadPolicyResponse } from "@smart-cloud-brain/shared-api";

export type UploadedImageAsset = {
  url: string;
  objectKey: string;
};

const maxClientSizeBytes = 5 * 1024 * 1024;
const allowedTypes = new Set(["image/jpeg", "image/png", "image/webp"]);

export function useOssImageUpload() {
  const uploading = ref(false);
  const error = ref("");

  async function uploadImage(file: File): Promise<UploadedImageAsset | null> {
    error.value = "";
    const validationError = validateFile(file);
    if (validationError) {
      error.value = validationError;
      return null;
    }
    uploading.value = true;
    try {
      const policy = await adminApi.assetUploadPolicy({
        scene: "patient-site",
        fileName: file.name,
        contentType: file.type,
        size: file.size,
      });
      await uploadToObjectStorage(policy, file);
      return { url: policy.publicUrl, objectKey: policy.objectKey };
    } catch (err) {
      error.value = messageFrom(err);
      return null;
    } finally {
      uploading.value = false;
    }
  }

  return { uploading, error, uploadImage };
}

function validateFile(file: File) {
  if (!allowedTypes.has(file.type)) return "仅支持 JPG、PNG、WebP 图片";
  if (file.size <= 0) return "图片文件不能为空";
  if (file.size > maxClientSizeBytes) return "图片不能超过 5MB";
  return "";
}

async function uploadToObjectStorage(policy: AssetUploadPolicyResponse, file: File) {
  if (policy.uploadMethod === "POST") {
    const formData = new FormData();
    Object.entries(policy.formData ?? {}).forEach(([key, value]) => formData.append(key, value));
    formData.append("file", file);
    const response = await fetch(policy.uploadUrl, { method: "POST", body: formData });
    if (!response.ok) throw new Error("对象存储上传失败");
    return;
  }
  if (policy.uploadMethod === "PUT") {
    const response = await fetch(policy.uploadUrl, {
      method: "PUT",
      headers: policy.headers ?? {},
      body: file,
    });
    if (!response.ok) throw new Error("对象存储上传失败");
    return;
  }
  throw new Error(`不支持的上传方式：${policy.uploadMethod}`);
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError) return err.message;
  if (err instanceof Error && err.message) return err.message;
  return "图片上传失败";
}
