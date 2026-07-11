<script setup lang="ts">
import { Delete, Picture, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onBeforeUnmount, ref } from 'vue'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    maxSizeMB?: number
    hint?: string
  }>(),
  {
    modelValue: '',
    maxSizeMB: 2,
    hint: '当前后端未提供文件上传接口，本地选择仅用于预览，请继续填写可访问的图片 URL。',
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const inputRef = ref<HTMLInputElement>()
const localPreviewUrl = ref('')

const previewUrl = computed(() => localPreviewUrl.value || props.modelValue)

function revokeLocalPreview() {
  if (localPreviewUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(localPreviewUrl.value)
  }
  localPreviewUrl.value = ''
}

function triggerChoose() {
  inputRef.value?.click()
}

function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.warning('只能选择图片文件')
    target.value = ''
    return
  }

  const maxSize = props.maxSizeMB * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.warning(`图片大小不能超过 ${props.maxSizeMB}MB`)
    target.value = ''
    return
  }

  revokeLocalPreview()
  localPreviewUrl.value = URL.createObjectURL(file)
  ElMessage.info('已完成本地预览，请继续填写图片 URL 后再提交')
  target.value = ''
}

function clearUrl() {
  emit('update:modelValue', '')
}

onBeforeUnmount(() => {
  revokeLocalPreview()
})
</script>

<template>
  <div class="upload-assist">
    <div class="upload-actions">
      <el-button :icon="Upload" @click="triggerChoose">选择本地图片预览</el-button>
      <el-button v-if="modelValue" text :icon="Delete" @click="clearUrl">清空图片地址</el-button>
      <el-button v-if="localPreviewUrl" text :icon="Delete" @click="revokeLocalPreview">清空本地预览</el-button>
    </div>

    <input ref="inputRef" class="hidden-input" type="file" accept="image/*" @change="handleFileChange" />

    <div v-if="previewUrl" class="preview-panel">
      <el-image :src="previewUrl" fit="cover" class="preview-image" preview-teleported />
    </div>
    <div v-else class="empty-panel">
      <el-icon><Picture /></el-icon>
      <span>可先选择本地图片预览，最终仍以表单中的图片 URL 为准</span>
    </div>

    <div class="upload-hint">{{ hint }}</div>
  </div>
</template>

<style scoped>
.upload-assist {
  width: 100%;
  border-radius: 14px;
  padding: 12px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
}

.upload-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hidden-input {
  display: none;
}

.preview-panel,
.empty-panel {
  margin-top: 12px;
  border-radius: 14px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.preview-panel {
  padding: 10px;
}

.preview-image {
  width: 180px;
  height: 120px;
  border-radius: 10px;
  display: block;
}

.empty-panel {
  min-height: 88px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #64748b;
  font-size: 13px;
}

.upload-hint {
  margin-top: 10px;
  font-size: 12px;
  line-height: 1.7;
  color: #64748b;
}
</style>
