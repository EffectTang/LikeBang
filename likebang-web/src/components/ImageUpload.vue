<template>
  <div class="image-upload">
    <!-- 已有图：缩略图 + 悬浮移除，点击看大图（自带预览弹层，父组件无需接线） -->
    <div v-if="modelValue" class="thumb-wrap">
      <img :src="resolveImage(modelValue)" class="thumb" alt="" @click="preview" />
      <div class="thumb-actions">
        <el-button link type="danger" size="small" :icon="Delete" @click="clear">移除</el-button>
      </div>
    </div>
    <!-- 无图：自定义上传触发器，不走 el-upload 默认列表，状态完全由 v-model 驱动 -->
    <el-upload
      v-else
      :show-file-list="false"
      :disabled="disabled || uploading"
      :before-upload="beforeUpload"
      :http-request="doUpload"
      accept="image/jpeg,image/png,image/gif,image/webp"
    >
      <div class="uploader" :class="{ 'is-disabled': disabled }" v-loading="uploading">
        <el-icon v-if="!uploading"><Picture /></el-icon>
        <span>{{ uploading ? '上传中' : label }}</span>
      </div>
    </el-upload>

    <el-dialog v-model="previewVisible" width="600px" append-to-body>
      <img :src="resolveImage(modelValue)" style="width: 100%" alt="预览" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete, Picture } from '@element-plus/icons-vue'
import { uploadImage } from '@/api/file'
import { resolveImage } from '@/utils/image'

const props = defineProps({
  // 后端返回的相对路径（/uploads/xxx）；空串表示无图
  modelValue: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  label: { type: String, default: '添加配图' },
  // 提交前的额外校验（返回 false 中止上传）
  beforeCheck: { type: Function, default: null }
})
const emit = defineEmits(['update:modelValue'])

const uploading = ref(false)
const previewVisible = ref(false)

function beforeUpload(file) {
  if (!/^image\/(jpeg|png|gif|webp)$/.test(file.type)) {
    ElMessage.warning('仅支持 jpg/png/gif/webp 格式图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('单张图片不能超过 5MB')
    return false
  }
  if (props.beforeCheck && !props.beforeCheck(file)) return false
  return true
}

async function doUpload(req) {
  uploading.value = true
  try {
    const res = await uploadImage(req.file)
    emit('update:modelValue', res.data)
  } catch (e) {
    // 失败保持无图状态可重试（request 拦截器已弹错误提示）
  } finally {
    uploading.value = false
  }
}

function preview() {
  previewVisible.value = true
}

function clear() {
  // 置空串：提交时后端按“清空配图”处理
  emit('update:modelValue', '')
}
</script>

<style scoped>
.image-upload {
  display: flex;
  align-items: center;
  gap: 10px;
}
.thumb-wrap {
  position: relative;
}
.thumb {
  display: block;
  width: 96px;
  height: 96px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  cursor: zoom-in;
}
.thumb-actions {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: center;
  padding: 2px 0;
  background: rgba(255, 255, 255, 0.85);
  border-radius: 0 0 8px 8px;
}
.uploader {
  width: 96px;
  height: 96px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  color: #909399;
  font-size: 12px;
  cursor: pointer;
  transition: border-color 0.2s;
}
.uploader:hover {
  border-color: #409eff;
  color: #409eff;
}
.uploader.is-disabled {
  cursor: not-allowed;
  background: #f5f7fa;
}
</style>
