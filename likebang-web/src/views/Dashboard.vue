<template>
  <div>
    <el-card>
      <h2>仪表盘</h2>
      <p>欢迎使用 LikeBang 管理系统！</p>
      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="8">
          <el-card shadow="hover">
            <div style="text-align: center;">
              <div style="font-size: 28px; color: #409EFF;">用户总数</div>
              <div style="font-size: 36px; margin-top: 10px;">-</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover">
            <div style="text-align: center;">
              <div style="font-size: 28px; color: #67C23A;">启用用户</div>
              <div style="font-size: 36px; margin-top: 10px;">-</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover">
            <div style="text-align: center;">
              <div style="font-size: 28px; color: #E6A23C;">今日访问</div>
              <div style="font-size: 36px; margin-top: 10px;">-</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
</script>
