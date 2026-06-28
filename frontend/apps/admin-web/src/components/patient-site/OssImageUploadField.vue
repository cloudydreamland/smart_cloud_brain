<script setup lang="ts">
import { computed, ref } from "vue";
import { useOssImageUpload, type UploadedImageAsset } from "../../composables/useOssImageUpload";

const props = defineProps<{
  imageUrl?: string;
  imageAlt?: string;
  objectKey?: string;
  label: string;
  altLabel?: string;
  disabled?: boolean;
}>();

const emit = defineEmits<{
  uploaded: [value: UploadedImageAsset];
  "update:imageAlt": [value: string];
  cleared: [];
}>();

const inputRef = ref<HTMLInputElement | null>(null);
const { uploading, error, uploadImage } = useOssImageUpload();
const hasImage = computed(() => Boolean(props.imageUrl));

function chooseFile() {
  if (props.disabled || uploading.value) return;
  inputRef.value?.click();
}

async function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = "";
  if (!file) return;
  const asset = await uploadImage(file);
  if (asset) emit("uploaded", asset);
}

function clearImage() {
  if (props.disabled || uploading.value) return;
  emit("cleared");
}
</script>

<template>
  <div class="oss-image-upload">
    <div class="oss-image-upload__head">
      <span>{{ label }}</span>
      <span v-if="objectKey" class="oss-image-upload__key">{{ objectKey }}</span>
    </div>

    <div class="oss-image-upload__body">
      <div class="oss-image-upload__preview" :class="{ 'is-empty': !hasImage }">
        <img v-if="imageUrl" :src="imageUrl" :alt="imageAlt || label">
        <span v-else>未上传图片</span>
      </div>
      <div class="oss-image-upload__actions">
        <input ref="inputRef" type="file" accept="image/jpeg,image/png,image/webp" :disabled="disabled || uploading" @change="onFileChange">
        <button type="button" class="topbar-refresh" :disabled="disabled || uploading" @click="chooseFile">
          {{ uploading ? "上传中" : "选择图片" }}
        </button>
        <button v-if="imageUrl" type="button" class="danger-link" :disabled="disabled || uploading" @click="clearImage">清空图片</button>
      </div>
    </div>

    <label class="oss-image-upload__alt">
      <span>{{ altLabel || "图片说明" }}</span>
      <input :value="imageAlt" type="text" :disabled="disabled" @input="emit('update:imageAlt', ($event.target as HTMLInputElement).value)">
    </label>
    <p v-if="error" class="oss-image-upload__error">{{ error }}</p>
  </div>
</template>

<style scoped>
.oss-image-upload {
  display: grid;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--color-white);
}

.oss-image-upload__head,
.oss-image-upload__body,
.oss-image-upload__actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.oss-image-upload__head {
  justify-content: space-between;
  color: var(--ink);
  font-size: 13px;
  font-weight: 700;
}

.oss-image-upload__key {
  max-width: 48%;
  overflow: hidden;
  color: var(--muted);
  font-size: 12px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.oss-image-upload__body {
  align-items: stretch;
}

.oss-image-upload__preview {
  display: grid;
  width: 132px;
  min-height: 82px;
  place-items: center;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface-alt);
  color: var(--muted);
  font-size: 12px;
}

.oss-image-upload__preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.oss-image-upload__preview.is-empty {
  border-style: dashed;
}

.oss-image-upload__actions {
  flex-wrap: wrap;
  align-content: center;
}

.oss-image-upload__actions input {
  display: none;
}

.oss-image-upload__alt {
  display: grid;
  gap: 6px;
}

.oss-image-upload__error {
  margin: 0;
  color: var(--color-red-600);
  font-size: 12px;
}
</style>
