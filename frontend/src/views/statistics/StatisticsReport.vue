<template>
  <div class="statistics-report">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>统计报表</span>
              <div>
                <el-date-picker
                  v-model="dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                />
                <el-button type="primary" @click="handleGenerate">生成报表</el-button>
              </div>
            </div>
          </template>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>任务执行趋势</span>
          </template>
          <div ref="lineChart" style="height: 350px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>任务类型分布</span>
          </template>
          <div ref="pieChart" style="height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>机器人执行统计</span>
          </template>
          <div ref="barChart" style="height: 350px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>成功率统计</span>
          </template>
          <div ref="successChart" style="height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="table-row">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>详细数据统计</span>
              <el-button type="success" @click="handleExport">
                <el-icon><Download /></el-icon>
                导出报表
              </el-button>
            </div>
          </template>
          <el-table :data="tableData" border>
            <el-table-column prop="date" label="日期" width="150" />
            <el-table-column prop="totalTasks" label="总任务数" width="120" />
            <el-table-column prop="successTasks" label="成功任务" width="120" />
            <el-table-column prop="failedTasks" label="失败任务" width="120" />
            <el-table-column prop="successRate" label="成功率" width="120" />
            <el-table-column prop="avgDuration" label="平均耗时" width="120" />
            <el-table-column prop="robotCount" label="在线机器人" width="120" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'

const dateRange = ref([])
const lineChart = ref(null)
const pieChart = ref(null)
const barChart = ref(null)
const successChart = ref(null)

const tableData = ref([
  { date: '2024-01-15', totalTasks: 56, successTasks: 54, failedTasks: 2, successRate: '96.4%', avgDuration: '3.2分', robotCount: 8 },
  { date: '2024-01-14', totalTasks: 48, successTasks: 47, failedTasks: 1, successRate: '97.9%', avgDuration: '2.8分', robotCount: 7 },
  { date: '2024-01-13', totalTasks: 62, successTasks: 60, failedTasks: 2, successRate: '96.8%', avgDuration: '3.5分', robotCount: 9 },
  { date: '2024-01-12', totalTasks: 51, successTasks: 50, failedTasks: 1, successRate: '98.0%', avgDuration: '2.9分', robotCount: 8 }
])

const handleGenerate = () => {
  ElMessage.success('报表生成成功')
}

const handleExport = () => {
  ElMessage.success('报表导出成功')
}

onMounted(() => {
  // 折线图
  const lineInstance = echarts.init(lineChart.value)
  lineInstance.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['任务总数', '成功数'] },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: { type: 'value' },
    series: [
      { name: '任务总数', type: 'line', data: [120, 132, 101, 134, 90, 230, 210] },
      { name: '成功数', type: 'line', data: [118, 130, 98, 132, 88, 225, 205] }
    ]
  })

  // 饼图
  const pieInstance = echarts.init(pieChart.value)
  pieInstance.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: '50%',
      data: [
        { value: 1048, name: '数据采集' },
        { value: 735, name: '报表生成' },
        { value: 580, name: '文件处理' },
        { value: 484, name: '数据同步' }
      ]
    }]
  })

  // 柱状图
  const barInstance = echarts.init(barChart.value)
  barInstance.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: ['Robot-01', 'Robot-02', 'Robot-03', 'Robot-04']
    },
    yAxis: { type: 'value' },
    series: [{
      data: [120, 200, 150, 80],
      type: 'bar'
    }]
  })

  // 成功率图
  const successInstance = echarts.init(successChart.value)
  successInstance.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: { type: 'value', max: 100 },
    series: [{
      data: [98, 99, 97, 98, 98, 99, 98],
      type: 'line',
      smooth: true,
      areaStyle: { color: 'rgba(24, 144, 255, 0.3)' }
    }]
  })

  // 响应式
  window.addEventListener('resize', () => {
    lineInstance.resize()
    pieInstance.resize()
    barInstance.resize()
    successInstance.resize()
  })
})
</script>

<style scoped lang="scss">
.statistics-report {
  .chart-row {
    margin-top: 20px;
  }

  .table-row {
    margin-top: 20px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
