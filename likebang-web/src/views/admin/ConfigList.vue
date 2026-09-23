<template>
  <div>
    <el-card>
      <div style="margin-bottom: 20px; display: flex; justify-content: space-between;">
        <div>
          <el-input
            v-model="keyword"
            placeholder="搜索配置名称/键名"
            style="width: 260px; margin-right: 10px;"
            clearable
            @keyup.enter="handleSearch">
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select
            v-model="group"
            placeholder="全部分组"
            clearable
            style="width: 140px; margin-right: 10px;"
            @change="handleSearch">
            <el-option v-for="g in groupOptions" :key="g.value" :label="g.label" :value="g.value" />
          </el-select>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="configName" label="配置名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="configKey" label="配置键" min-width="220" show-overflow-tooltip />
        <el-table-column label="当前值" width="140">
          <template #default="scope">
            <el-tag type="info">{{ displayValue(scope.row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="valueType" label="类型" width="90" />
        <el-table-column prop="configGroup" label="分组" width="110" />
        <el-table-column label="范围" width="120">
          <template #default="scope">
            <span v-if="scope.row.valueType === 'int'">
              {{ scope.row.minValue ?? '-' }} ~ {{ scope.row.maxValue ?? '-' }}
            </span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.editable === 1 ? 'success' : 'info'">
              {{ scope.row.editable === 1 ? '可编辑' : '只读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="scope">
            <el-button
              size="small"
              type="primary"
              link
              :disabled="scope.row.editable !== 1"
              @click="handleEdit(scope.row)">修改</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px;"
        @size-change="loadList"
        @current-change="loadList" />
    </el-card>

    <el-dialog v-model="dialogVisible" title="修改配置" width="480px" @closed="resetForm">
      <el-form label-width="80px">
        <el-form-item label="配置名称">
          <span>{{ current.configName }}</span>
        </el-form-item>
        <el-form-item label="配置键">
          <span style="color: #909399; font-size: 13px;">{{ current.configKey }}</span>
        </el-form-item>
        <el-form-item v-if="current.description" label="说明">
          <span style="color: #909399; font-size: 12px;">{{ current.description }}</span>
        </el-form-item>
        <el-form-item label="配置值">
          <!-- int：数字输入框，带 min/max 约束 -->
          <el-input-number
            v-if="current.valueType === 'int'"
            v-model="form.value"
            :min="toNum(current.minValue, -Infinity)"
            :max="toNum(current.maxValue, Infinity)" />
          <!-- boolean：下拉 -->
          <el-select v-else-if="current.valueType === 'boolean'" v-model="form.value" style="width: 160px;">
            <el-option label="true" :value="true" />
            <el-option label="false" :value="false" />
          </el-select>
          <!-- 其它：文本 -->
          <el-input v-else v-model="form.value" maxlength="512" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { pageConfigs, updateConfig } from '@/api/config'

const loading = ref(false)
const submitting = ref(false)
const keyword = ref('')
const group = ref('')
const tableData = ref([])
const dialogVisible = ref(false)
const current = ref({})

const groupOptions = [
  { label: '榜单', value: 'ranking' },
  { label: '展示', value: 'display' },
  { label: '系统', value: 'system' }
]

const pagination = reactive({ current: 1, size: 10, total: 0 })

// 编辑草稿：value 依 valueType 采用 number/boolean/string
const form = reactive({ value: undefined })

function toNum(v, fallback) {
  const n = Number(v)
  return v == null || v === '' || Number.isNaN(n) ? fallback : n
}

// 表格"当前值"列：int 显示数字，boolean 显示 true/false，其余原样
function displayValue(row) {
  return row.configValue ?? ''
}

// 载入时把字符串值转成表单所需的类型
function parseFormValue(row) {
  if (row.valueType === 'int') return toNum(row.configValue, undefined)
  if (row.valueType === 'boolean') return row.configValue === 'true'
  return row.configValue ?? ''
}

// 提交时把表单值统一转回字符串（后端 configValue 为字符串）
function serializeValue(row, value) {
  if (value == null) return ''
  if (row.valueType === 'int') return String(value)
  if (row.valueType === 'boolean') return String(value)
  return String(value)
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await pageConfigs({
      current: pagination.current,
      size: pagination.size,
      keyword: keyword.value || undefined,
      group: group.value || undefined
    })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadList()
}

const handleEdit = (row) => {
  current.value = row
  form.value = parseFormValue(row)
  dialogVisible.value = true
}

const submitForm = async () => {
  const value = serializeValue(current.value, form.value)
  if (value === '') {
    ElMessage.warning('配置值不能为空')
    return
  }
  submitting.value = true
  try {
    await updateConfig(current.value.id, value)
    ElMessage.success('修改成功，最迟 5 分钟内全站生效')
    dialogVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  form.value = undefined
  current.value = {}
}

onMounted(() => {
  loadList()
})
</script>
