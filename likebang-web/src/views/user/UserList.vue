<template>
  <div>
    <el-card>
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="query.keyword"
            placeholder="搜索用户名/昵称/邮箱"
            style="width: 260px; margin-right: 10px;"
            clearable
            @keyup.enter="reload"
            @clear="reload">
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px; margin-right: 10px;" @change="reload">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-select v-model="query.role" placeholder="角色" clearable style="width: 130px; margin-right: 10px;" @change="reload">
            <el-option label="普通用户" :value="0" />
            <el-option label="管理员" :value="1" />
            <el-option label="运营管理员" :value="2" />
          </el-select>
          <el-button type="primary" @click="reload">搜索</el-button>
          <el-button
            v-if="userStore.isAdmin"
            type="success"
            :icon="Plus"
            style="margin-left: 10px;"
            @click="openCreate">新增用户</el-button>
        </div>
        <el-alert
          v-if="!userStore.isAdmin"
          type="info"
          :closable="false"
          show-icon
          title="您是运营管理员：可查询与维护普通用户，不能变更角色、不能操作管理员、不能删除用户"
          style="width: auto; margin: 0; padding: 4px 12px;" />
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="170" show-overflow-tooltip />
        <el-table-column prop="username" label="用户名" min-width="110" show-overflow-tooltip />
        <el-table-column prop="nickname" label="昵称" min-width="110" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" min-width="170" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="130">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" effect="plain">{{ roleText(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后登录" width="150">
          <template #default="{ row }">{{ fmtDate(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ fmtDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canEditRow(row)" size="small" type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="canEditRow(row) && !isSelf(row)"
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              link
              @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button v-if="userStore.isAdmin && !isSelf(row)" size="small" type="primary" link @click="openRole(row)">授权</el-button>
            <el-button v-if="userStore.isAdmin && !isSelf(row)" size="small" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="loadList"
          @size-change="onSizeChange" />
      </div>
    </el-card>

    <!-- 编辑用户（基本信息 + 状态） -->
    <el-dialog v-model="editVisible" title="编辑用户" width="480px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="用户名">
          <el-input :model-value="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" placeholder="不修改请留空" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="不修改请留空" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" placeholder="不修改请留空" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 变更角色（仅管理员） -->
    <el-dialog v-model="roleVisible" title="变更角色" width="440px">
      <p class="role-target">
        用户：<b>{{ roleRow?.username }}</b>（{{ roleRow?.nickname }}）
      </p>
      <el-radio-group v-model="roleValue">
        <el-radio :value="0">普通用户</el-radio>
        <el-radio :value="2">运营管理员</el-radio>
        <el-radio :value="1">管理员</el-radio>
      </el-radio-group>
      <p class="role-hint">运营管理员 = 被授权管理普通用户，但不能变更任何角色、不能删除用户</p>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitRole">确认变更</el-button>
      </template>
    </el-dialog>

    <!-- 新增用户（仅管理员） -->
    <el-dialog v-model="createVisible" title="新增用户" width="480px" @closed="resetCreate">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="createForm.username" placeholder="字母/数字/下划线，4~32位" maxlength="32" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="createForm.nickname" placeholder="请输入昵称" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" placeholder="6~32位" maxlength="32" show-password />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="createForm.email" placeholder="请输入邮箱" maxlength="128" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="createForm.phone" placeholder="可选" maxlength="20" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="createForm.role">
            <el-radio :value="0">普通用户</el-radio>
            <el-radio :value="2">运营管理员</el-radio>
            <el-radio :value="1">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="createForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)

const query = reactive({
  current: 1,
  size: 10,
  keyword: '',
  status: null,
  role: null
})

// 后端全局 Long→String 序列化，分页元数据须归一化为数字
async function loadList() {
  loading.value = true
  try {
    const res = await request.get('/users', {
      params: {
        current: query.current,
        size: query.size,
        keyword: query.keyword || undefined,
        status: query.status ?? undefined,
        role: query.role ?? undefined
      }
    })
    tableData.value = res.data.records
    total.value = Number(res.data.total) || 0
  } finally {
    loading.value = false
  }
}

function reload() {
  query.current = 1
  loadList()
}

function onSizeChange() {
  query.current = 1
  loadList()
}

function roleText(role) {
  return { 0: '普通用户', 1: '管理员', 2: '运营管理员' }[role] ?? '未知'
}

function roleTagType(role) {
  return { 0: 'info', 1: 'danger', 2: 'warning' }[role] ?? 'info'
}

function fmtDate(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}

// ---- 行级权限（渲染层；权威判定在后端 Service）----
// 运营管理员仅能操作普通用户；管理员可操作所有人（超管保护等由后端 403 兜底）
function canEditRow(row) {
  return userStore.isAdmin || row.role === 0
}

// 不提供对自己的破坏性操作入口（禁自己/删自己/改自己角色均由后端同规则拦截）
function isSelf(row) {
  return String(row.id) === String(userStore.userInfo?.id)
}

// ---- 编辑 ----
const editVisible = ref(false)
const editFormRef = ref(null)
const editForm = reactive({
  id: null,
  username: '',
  nickname: '',
  email: '',
  phone: '',
  status: 1
})
const editRules = {
  nickname: [{ max: 32, message: '长度不能超过32个字符', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

function openEdit(row) {
  Object.assign(editForm, {
    id: row.id,
    username: row.username,
    nickname: '',
    email: '',
    phone: '',
    status: row.status
  })
  editVisible.value = true
}

async function submitEdit() {
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    // null/空 = 不修改（与后端局部更新语义一致），状态始终提交
    await request.put(`/users/${editForm.id}`, {
      nickname: editForm.nickname || null,
      email: editForm.email || null,
      phone: editForm.phone || null,
      status: editForm.status
    })
    ElMessage.success('保存成功')
    editVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

// ---- 启用/禁用快捷操作 ----
function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  const word = next === 1 ? '启用' : '禁用'
  ElMessageBox.confirm(
    next === 0
      ? `禁用后 ${row.username} 的登录状态将立即失效，确认${word}？`
      : `确认${word}用户 ${row.username}？`,
    '提示',
    { type: 'warning' }
  ).then(async () => {
    // 只提交 status，null=不修改的局部更新语义
    await request.put(`/users/${row.id}`, { status: next })
    ElMessage.success(`已${word}`)
    loadList()
  }).catch(() => {})
}

// ---- 变更角色 ----
const roleVisible = ref(false)
const roleRow = ref(null)
const roleValue = ref(0)

function openRole(row) {
  roleRow.value = row
  roleValue.value = row.role
  roleVisible.value = true
}

async function submitRole() {
  if (!roleRow.value || roleValue.value === roleRow.value.role) {
    roleVisible.value = false
    return
  }
  submitting.value = true
  try {
    await request.patch(`/users/${roleRow.value.id}/role`, { role: roleValue.value })
    ElMessage.success('角色已变更')
    roleVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

// ---- 删除 ----
async function handleDelete(row) {
  await ElMessageBox.confirm(
    `确定删除用户 ${row.username} 吗？删除后其登录状态立即失效（逻辑删除，数据保留）`,
    '危险操作',
    { type: 'warning' }
  ).then(async () => {
    await request.delete(`/users/${row.id}`)
    ElMessage.success('删除成功')
    loadList()
  }).catch(() => {})
}

// ---- 新增用户（仅管理员，后端 POST /users 限 ADMIN）----
const createVisible = ref(false)
const createFormRef = ref(null)
const createForm = reactive({
  username: '',
  nickname: '',
  password: '',
  email: '',
  phone: '',
  role: 0,
  status: 1
})
const createRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{4,32}$/, message: '字母/数字/下划线，4~32位', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 32, message: '昵称长度不能超过32个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在6~32之间', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
    { max: 128, message: '邮箱长度不能超过128个字符', trigger: 'blur' }
  ],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

function openCreate() {
  createVisible.value = true
}

function resetCreate() {
  createFormRef.value?.resetFields()
  Object.assign(createForm, {
    username: '', nickname: '', password: '', email: '', phone: '', role: 0, status: 1
  })
}

async function submitCreate() {
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await request.post('/users', {
      username: createForm.username,
      nickname: createForm.nickname,
      password: createForm.password,
      email: createForm.email,
      phone: createForm.phone || null,
      role: createForm.role,
      status: createForm.status
    })
    ElMessage.success('用户创建成功')
    createVisible.value = false
    reload()
  } finally {
    submitting.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  gap: 16px;
}
.toolbar-left {
  display: flex;
  align-items: center;
  white-space: nowrap;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.role-target {
  margin: 0 0 12px;
  color: #303133;
}
.role-hint {
  margin: 12px 0 0;
  color: #909399;
  font-size: 12px;
}
</style>
