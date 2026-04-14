<template>
  <div class="robot-form-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <el-button link @click="handleBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <el-divider direction="vertical" />
        <span class="page-title">{{ isEdit ? '编辑机器人' : '新增机器人' }}</span>
      </div>
      <div class="header-right">
        <el-button @click="handleBack">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
          确定
        </el-button>
      </div>
    </div>

    <!-- 表单区域 -->
    <div class="form-section">
      <el-card class="form-card" v-loading="loading">
        <el-form 
          ref="formRef"
          :model="robotForm" 
          :rules="formRules" 
          label-width="120px"
          label-position="left"
          class="robot-form"
        >
          <el-form-item label="机器人编码" prop="robotCode" required>
            <el-input 
              v-model="robotForm.robotCode" 
              placeholder="请输入机器人编码"
              :disabled="isEdit"
            />
          </el-form-item>

          <el-form-item label="机器人名称" prop="name" required>
            <el-input 
              v-model="robotForm.name" 
              placeholder="请输入机器人名称"
            />
          </el-form-item>

          <el-form-item label="类型">
            <el-input 
              v-model="robotForm.type" 
              placeholder="可选：用于区分不同用途的机器人"
            />
          </el-form-item>

          <el-form-item label="描述">
            <el-input 
              v-model="robotForm.description" 
              type="textarea"
              :rows="4"
              placeholder="请输入机器人描述"
            />
          </el-form-item>

          <el-form-item label="状态">
            <el-radio-group v-model="robotForm.status">
              <el-radio value="online">在线</el-radio>
              <el-radio value="offline">离线</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <!-- 底部操作栏 -->
    <div class="footer-actions">
      <el-button @click="handleBack">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
        确定
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getRobotById, createRobot, updateRobot } from '../../api/robot.js'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const loading = ref(false)
const submitLoading = ref(false)

const robotId = computed(() => route.params.id)
const isEdit = computed(() => !!robotId.value)

const robotForm = reactive({
  robotCode: '',
  name: '',
  type: '',
  description: '',
  status: 'offline'
})

const formRules = {
  robotCode: [
    { required: true, message: '请输入机器人编码', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入机器人名称', trigger: 'blur' }
  ]
}

// 加载机器人数据（编辑模式）
const loadRobotData = async () => {
  if (!robotId.value) return
  
  loading.value = true
  try {
    const res = await getRobotById(robotId.value)
    if (res.code === 200) {
      const robot = res.data
      robotForm.robotCode = robot.robotCode || ''
      robotForm.name = robot.name || ''
      robotForm.type = robot.type || ''
      robotForm.description = robot.description || ''
      robotForm.status = robot.status || 'offline'
    } else {
      ElMessage.error(res.message || '获取机器人信息失败')
      handleBack()
    }
  } catch (error) {
    console.error('加载机器人数据失败:', error)
    ElMessage.error('加载机器人数据失败')
    handleBack()
  } finally {
    loading.value = false
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    let res
    if (isEdit.value) {
      res = await updateRobot(robotId.value, robotForm)
    } else {
      res = await createRobot(robotForm)
    }
    
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
      router.push('/robot/list')
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  } finally {
    submitLoading.value = false
  }
}

// 返回列表
const handleBack = () => {
  router.push('/robot/list')
}

onMounted(() => {
  loadRobotData()
})
</script>

<style scoped lang="scss">
.robot-form-page {
  min-height: calc(100vh - 60px);
  background: #f5f7fa;
  padding-bottom: 70px;

  // 页面头部
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 15px 20px;
    background: #fff;
    border-bottom: 1px solid #e8e8e8;
    position: sticky;
    top: 0;
    z-index: 100;

    .header-left {
      display: flex;
      align-items: center;
      gap: 10px;

      .page-title {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }
    }

    .header-right {
      display: flex;
      gap: 10px;
    }
  }

  // 表单区域
  .form-section {
    padding: 20px;

    .form-card {
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
      max-width: 800px;
    }
  }

  // 底部操作栏
  .footer-actions {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 15px 20px;
    background: #fff;
    border-top: 1px solid #e8e8e8;
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    z-index: 100;
  }
}

// 表单样式
:deep(.robot-form) {
  .el-form-item {
    margin-bottom: 24px;
    
    &:last-child {
      margin-bottom: 0;
    }
  }
  
  .el-form-item__label {
    font-weight: 500;
    color: #606266;
    padding-right: 12px;
    
    &::before {
      color: #f56c6c;
      margin-right: 4px;
    }
  }
  
  .el-input__inner {
    border-radius: 4px;
    
    &::placeholder {
      color: #c0c4cc;
    }
  }
  
  .el-textarea__inner {
    border-radius: 4px;
    
    &::placeholder {
      color: #c0c4cc;
    }
  }
  
  .el-radio-group {
    .el-radio {
      margin-right: 24px;
      
      .el-radio__label {
        color: #606266;
      }
      
      &.is-checked {
        .el-radio__inner {
          background-color: #409eff;
          border-color: #409eff;
        }
        
        .el-radio__label {
          color: #409eff;
        }
      }
    }
  }
}

// Element Plus 按钮样式
:deep(.el-button) {
  border-radius: 4px;
  padding: 9px 20px;
  
  &.el-button--primary {
    background-color: #409eff;
    border-color: #409eff;
    
    &:hover {
      background-color: #66b1ff;
      border-color: #66b1ff;
    }
  }
  
  &:not(.el-button--primary):not(.el-button--text) {
    &:hover {
      color: #409eff;
      border-color: #c6e2ff;
      background-color: #ecf5ff;
    }
  }
}
</style>
