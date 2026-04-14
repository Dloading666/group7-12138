<template>
  <div class="robot-management-page">
    <!-- 统计卡片区域 -->
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-label">总机器人数</div>
      </div>
      <div class="stat-card online">
        <div class="stat-value">{{ stats.online }}</div>
        <div class="stat-label">在线</div>
      </div>
      <div class="stat-card working">
        <div class="stat-value">{{ stats.working }}</div>
        <div class="stat-label">工作中</div>
      </div>
      <div class="stat-card offline">
        <div class="stat-value">{{ stats.offline }}</div>
        <div class="stat-label">离线</div>
      </div>
    </div>

    <!-- 页面头部 -->
    <div class="page-header">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增机器人
      </el-button>
    </div>

    <!-- 查询筛选区 -->
    <div class="search-area">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="机器人名称">
          <el-input 
            v-model="searchForm.name" 
            placeholder="请输入" 
            clearable
            style="width: 200px;"
          />
        </el-form-item>
        <el-form-item label="机器人编码">
          <el-input 
            v-model="searchForm.code" 
            placeholder="请输入" 
            clearable
            style="width: 200px;"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select 
            v-model="searchForm.status" 
            placeholder="请选择" 
            clearable
            style="width: 150px;"
          >
            <el-option label="在线" value="online" />
            <el-option label="离线" value="offline" />
            <el-option label="运行中" value="running" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 数据表格 -->
    <div class="table-area">
      <el-table 
        :data="tableData" 
        v-loading="loading"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        
        <el-table-column prop="robotCode" label="机器人编码" width="150">
          <template #default="{ row }">
            <span class="code-text">{{ row.robotCode || '-' }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="name" label="机器人名称" width="180">
          <template #default="{ row }">
            <span class="name-text">{{ row.name }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="type" label="类型" width="120" align="center">
          <template #default="{ row }">
            <span class="type-text">{{ getTypeText(row.type) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="currentTaskId" label="当前任务ID" width="120" align="center">
          <template #default="{ row }">
            <span class="task-text">{{ row.currentTaskId || '空闲' }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="lastHeartbeat" label="最后心跳" width="160">
          <template #default="{ row }">
            <span class="time-text">{{ row.lastHeartbeat ? formatDateTime(row.lastHeartbeat) : '-' }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="updateTime" label="更新时间" width="180">
          <template #default="{ row }">
            <span class="time-text">{{ formatDateTime(row.updateTime) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" link size="small" @click="handleView(row)">
                查看
              </el-button>
              <el-button type="primary" link size="small" @click="handleEdit(row)">
                编辑
              </el-button>
              <el-button type="danger" link size="small" @click="handleDelete(row)">
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-area">
        <div class="pagination-total">Total {{ total }}</div>
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 新增/编辑机器人弹窗 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑机器人' : '新增机器人'" 
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form 
        ref="formRef"
        :model="robotForm" 
        :rules="formRules" 
        label-width="100px"
      >
        <el-form-item label="机器人编码" prop="robotCode">
          <el-input 
            v-model="robotForm.robotCode" 
            placeholder="如 ROBOT_001"
            :disabled="isEdit"
          />
        </el-form-item>
        
        <el-form-item label="机器人名称" prop="name">
          <el-input 
            v-model="robotForm.name" 
            placeholder="请输入机器人名称"
          />
        </el-form-item>
        
        <el-form-item label="机器人类型" prop="type">
          <el-select 
            v-model="robotForm.type" 
            placeholder="请选择类型"
            style="width: 100%;"
          >
            <el-option label="数据采集机器人" value="data_collector" />
            <el-option label="报表生成机器人" value="report_generator" />
            <el-option label="任务调度机器人" value="task_scheduler" />
            <el-option label="消息通知机器人" value="notification" />
            <el-option label="文件处理机器人" value="file_processor" />
            <el-option label="数据同步机器人" value="data_sync" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="描述" prop="description">
          <el-input 
            v-model="robotForm.description" 
            type="textarea"
            :rows="3"
            placeholder="请输入机器人描述"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAllRobots, deleteRobot, createRobot, updateRobot } from '../../api/robot.js'

const router = useRouter()

// 统计卡片数据
const stats = ref({
  total: 0,
  online: 0,
  working: 0,
  offline: 0
})

// 搜索表单
const searchForm = ref({
  name: '',
  code: '',
  status: ''
})

// 表格数据
const tableData = ref([])
const loading = ref(false)

// 分页
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 弹窗相关
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const submitLoading = ref(false)

// 机器人表单
const robotForm = ref({
  id: null,
  robotCode: '',
  name: '',
  type: '',
  description: ''
})

// 表单验证规则
const formRules = {
  robotCode: [
    { required: true, message: '请输入机器人编码', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入机器人名称', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择机器人类型', trigger: 'change' }
  ]
}

// 加载机器人列表
const loadRobotList = async () => {
  loading.value = true
  try {
    const res = await getAllRobots()
    if (res.code === 200) {
      let data = res.data || []
      
      // 前端筛选
      if (searchForm.value.name) {
        data = data.filter(item => 
          item.name && item.name.toLowerCase().includes(searchForm.value.name.toLowerCase())
        )
      }
      if (searchForm.value.code) {
        data = data.filter(item => 
          item.robotCode && item.robotCode.toLowerCase().includes(searchForm.value.code.toLowerCase())
        )
      }
      if (searchForm.value.status) {
        data = data.filter(item => item.status === searchForm.value.status)
      }
      
      // 计算统计数据
      stats.value = {
        total: data.length,
        online: data.filter(item => item.status === 'online').length,
        working: data.filter(item => item.status === 'running').length,
        offline: data.filter(item => item.status === 'offline').length
      }
      
      // 前端分页
      total.value = data.length
      const start = (pageNum.value - 1) * pageSize.value
      const end = start + pageSize.value
      tableData.value = data.slice(start, end)
    }
  } catch (error) {
    console.error('获取机器人列表失败:', error)
    ElMessage.error('获取机器人列表失败')
  } finally {
    loading.value = false
  }
}

// 
const handleAdd = () => {
  router.push('/robot/config')
}

// 编辑机器人
const handleEdit = (robot) => {
  dialogTitle.value = '编辑机器人'
  Object.assign(robotForm, {
    id: robot.id,
    name: robot.name,
    type: robot.type,
    description: robot.description
  })
  dialogVisible.value = true
}

// 配置机器人
const handleConfig = (robot) => {
  router.push(`/robot/config/${robot.id}`)
}

// 启动机器人
const handleStart = async (robot) => {
  try {
    const res = await startRobot(robot.id)
    if (res.code === 200) {
      ElMessage.success(`${robot.robotCode} 启动成功`)
      loadRobotList()
    }
  } catch (error) {
    ElMessage.error('启动失败')
  }
}

// 停止机器人
const handleStop = async (robot) => {
  try {
    const res = await stopRobot(robot.id)
    if (res.code === 200) {
      ElMessage.success(`${robot.robotCode} 已停止`)
      loadRobotList()
    }
  } catch (error) {
    ElMessage.error('停止失败')
  }
}

// 删除机器人
const handleDelete = (robot) => {
  ElMessageBox.confirm(`确定要删除机器人 "${robot.robotCode}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteRobot(robot.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        loadRobotList()
      }
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

// 提交表单
const handleSubmit = async () => {
  const valid = await robotFormRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    let res
    if (robotForm.id) {
      res = await updateRobot(robotForm.id, robotForm)
    } else {
      res = await createRobot(robotForm)
    }

    if (res.code === 200) {
      ElMessage.success(robotForm.id ? '更新成功' : '创建成功')
      dialogVisible.value = false
      loadRobotList()
    }
  } catch (error) {
    ElMessage.error(robotForm.id ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

// 关闭对话框
const handleDialogClose = () => {
  robotFormRef.value?.resetFields()
  resetForm()
}

// 重置表单
const resetForm = () => {
  Object.assign(robotForm, {
    id: null,
    name: '',
    type: '',
    description: ''
  })
}

// 初始化
onMounted(() => {
  loadRobotList()
})
</script>

<style scoped lang="scss">
.robot-management {
  padding: 20px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    h2 {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
      color: #1a1a2e;
    }
  }

  .robot-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 20px;
  }

  .robot-card {
    position: relative;
    background: #fff;
    border-radius: 12px;
    padding: 24px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    transition: all 0.3s;
    display: flex;
    flex-direction: column;
    align-items: center;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);

      .more-actions {
        opacity: 1;
      }
    }

    .robot-status {
      position: absolute;
      top: 12px;
      right: 12px;

      .status-badge {
        padding: 4px 12px;
        border-radius: 12px;
        font-size: 12px;
        font-weight: 500;

        &.online {
          background: #e6f7ed;
          color: #52c41a;
        }

        &.offline {
          background: #fff1f0;
          color: #ff4d4f;
        }

        &.running {
          background: #e6f7ff;
          color: #1890ff;
        }
      }
    }

    .robot-avatar {
      width: 80px;
      height: 80px;
      border-radius: 50%;
      background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      margin-bottom: 16px;
    }

    .robot-info {
      text-align: center;
      margin-bottom: 16px;

      .robot-code {
        margin: 0;
        font-size: 18px;
        font-weight: 600;
        color: #1a1a2e;
      }

      .robot-type {
        margin: 4px 0 0;
        font-size: 13px;
        color: #8e8e8e;
      }
    }

    .robot-stats {
      display: flex;
      justify-content: center;
      gap: 32px;
      padding: 16px 0;
      border-top: 1px solid #f0f0f0;
      border-bottom: 1px solid #f0f0f0;
      width: 100%;
      margin-bottom: 16px;

      .stat-item {
        display: flex;
        flex-direction: column;
        align-items: center;

        .stat-label {
          font-size: 12px;
          color: #8e8e8e;
          margin-bottom: 4px;
        }

        .stat-value {
          font-size: 18px;
          font-weight: 600;
          color: #1a1a2e;
        }
      }
    }

    .robot-actions {
      display: flex;
      gap: 8px;
      width: 100%;

      .el-button {
        flex: 1;
      }
    }

    .more-actions {
      position: absolute;
      bottom: 12px;
      right: 12px;
      opacity: 0;
      cursor: pointer;
      padding: 4px;
      color: #8e8e8e;
      transition: all 0.3s;

      &:hover {
        color: #11998e;
      }
    }
  }
}
</style>
