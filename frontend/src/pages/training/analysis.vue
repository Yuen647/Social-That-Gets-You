<template>
  <div class="dashboard-container">
    <div class="content-wrapper">
      <!-- 左侧主要内容区域 -->
      <div class="main-panel">
        <!-- 顶部统计卡片 -->
        <div class="stats-row">
          <div class="stat-card" v-for="(stat, index) in statistics" :key="index">
            <div class="stat-icon">
              <el-icon>
                <component :is="stat.icon" />
              </el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-title">{{ stat.title }}</div>
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-change" :class="stat.trend">
                {{ stat.change }}
                <el-icon>
                  <component :is="stat.trend === 'up' ? 'ArrowUp' : 'ArrowDown'" />
                </el-icon>
              </div>
            </div>
          </div>
        </div>

        <!-- 社交趋势分析 -->
        <div class="analysis-section">
          <div class="section-header">
            <h2>社交趋势分析</h2>
            <div class="filter-group">
              <div class="date-filters">
                <el-date-picker
                  v-model="trendStartDate"
                  type="date"
                  placeholder="开始日期"
                  class="filter-item"
                  :disabled-date="disableTrendStartDate"
                  @change="handleTrendFilterChange"
                />
                <el-date-picker
                  v-model="trendEndDate"
                  type="date"
                  placeholder="结束日期"
                  class="filter-item"
                  :disabled-date="disableTrendEndDate"
                  @change="handleTrendFilterChange"
                />
              </div>
              <el-select 
                v-model="trendSocialType" 
                placeholder="社交类型"
                class="filter-item"
                @change="handleTrendFilterChange"
              >
                <el-option label="兴趣社交" value="兴趣社交" />
                <el-option label="活动社交" value="活动社交" />
                <el-option label="学习社交" value="学习社交" />
              </el-select>
            </div>
          </div>

          <!-- 社交数据概览 -->
          <div class="overview-cards">
            <div class="overview-card" v-for="(stat, index) in trendStatItems" :key="index">
              <div class="overview-value">{{ stat.value }}</div>
              <div class="overview-label">{{ stat.label }}</div>
            </div>
          </div>

          <!-- 图表区域 -->
          <div class="charts-container">
            <div class="chart-row">
              <div class="chart-wrapper">
                <div ref="intensityChartRef" class="chart"></div>
              </div>
              <div class="chart-wrapper">
                <div ref="progressChartRef" class="chart"></div>
              </div>
            </div>
            <div class="chart-row">
              <div class="chart-wrapper">
                <div ref="timeDistributionChartRef" class="chart"></div>
              </div>
              <div class="chart-wrapper">
                <div ref="goalCompletionChartRef" class="chart"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧AI助手面板 -->
      <div class="ai-panel">
        <div class="ai-header">
          <h2>AI 社交助手</h2>
          <el-button link @click="clearChat">清空对话</el-button>
        </div>

        <div class="chat-container">
          <el-scrollbar ref="chatScrollbar">
            <div class="chat-messages">
              <div v-for="(msg, index) in chatMessages" :key="index" :class="['message', msg.type]">
                <div class="message-content">
                  <template v-if="msg.mediaType">
                    <div class="media-content">
                      <img v-if="msg.mediaType === 'image'" :src="msg.mediaUrl" class="message-image" />
                      <video v-if="msg.mediaType === 'video'" :src="msg.mediaUrl" controls class="message-video"></video>
                    </div>
                  </template>
                  {{ msg.content }}
                </div>
                <div class="message-time">{{ msg.time }}</div>
              </div>
              <div v-if="isLoading" class="message ai loading-message">
                <div class="typing-indicator">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            </div>
          </el-scrollbar>
        </div>

        <div class="chat-input-area">
          <el-upload class="media-upload" :show-file-list="false" :before-upload="handleUpload" accept="image/*,video/*">
            <el-button class="upload-btn" type="primary" text>
              <el-icon>
                <PictureRounded />
              </el-icon>
            </el-button>
          </el-upload>
          <div class="input-wrapper">
            <el-input v-model="chatInput" placeholder="输入问题，按回车发送" @keyup.enter="sendMessage">
              <template #append>
                <el-button type="primary" @click="sendMessage" :loading="isLoading">
                  发送
                </el-button>
              </template>
            </el-input>
          </div>
        </div>
      </div>
    </div>

    <!-- 社交记录表格 -->
    <div class="records-section">
      <div class="section-header">
        <h2>社交记录</h2>
        <div class="filter-group">
          <div class="date-filters">
            <el-date-picker v-model="startDate" type="date" placeholder="开始日期" class="filter-item"
              :disabled-date="disableStartDate" @change="handleDateChange" />
            <el-date-picker v-model="endDate" type="date" placeholder="结束日期" class="filter-item"
              :disabled-date="disableEndDate" @change="handleDateChange" />
          </div>
          <el-select v-model="currentSocialType" placeholder="社交类型" class="filter-item"
            @change="handleSocialTypeChange">
            <el-option label="全部类型" value="all" />
            <el-option label="兴趣社交" value="兴趣社交" />
            <el-option label="活动社交" value="活动社交" />
            <el-option label="学习社交" value="学习社交" />
          </el-select>
          <div class="action-group">
            <el-button type="primary" @click="handleAIAnalysis">
              <el-icon>
                <Monitor />
              </el-icon>智能分析
            </el-button>
            <el-button type="primary" @click="showUploadDialog">
              <el-icon>
                <Plus />
              </el-icon>添加记录
            </el-button>
            <el-button @click="refreshHistory">
              <el-icon>
                <Refresh />
              </el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <div class="table-container">
        <el-table :data="socialHistory" style="width: 100%" v-loading="loading"
          :header-cell-style="{ background: '#f5f7fa' }">
          <el-table-column prop="socialDate" label="日期" min-width="180">
            <template #default="scope">
              {{ formatDate(scope.row.socialDate) }}
            </template>
          </el-table-column>
          <el-table-column prop="duration" label="时长" min-width="120">
            <template #default="scope">
              {{ formatDuration(scope.row.duration) }}
            </template>
          </el-table-column>
          <el-table-column prop="participants" label="参与人数" min-width="120">
            <template #default="scope">
              {{ scope.row.participants || 0 }}
            </template>
          </el-table-column>
          <el-table-column prop="interactions" label="互动次数" min-width="120">
            <template #default="scope">
              {{ scope.row.interactions || 0 }}
            </template>
          </el-table-column>
          <el-table-column prop="type" label="社交类型" min-width="120" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="scope">
              <el-button link type="danger" @click="handleDeleteSocial(scope.row.id)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrapper">
          <el-pagination v-model:current-page="currentPage" :page-size="pageSize" :total="totalRecords"
            @current-change="handlePageChange" layout="total, prev, pager, next" background />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ElMessageBox } from 'element-plus/es'
import 'element-plus/es/components/base/style/css'
import 'element-plus/es/components/message-box/style/css'
import { useUserStore } from '@/stores/user'
import {
  submitTrainingRecord,
  queryTrainingRecords,
  getTrainingStatistics,
  updateTrainingRecord,
  deleteTrainingRecord
} from '@/api/training_record'
import axios from 'axios'
import { PictureRounded, Plus, Refresh, Monitor } from '@element-plus/icons-vue'
import * as echarts from 'echarts/core'
import { PieChart, LineChart, BarChart, RadarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

// 注册必需的组件
echarts.use([
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  PieChart,
  LineChart,
  BarChart,
  RadarChart,
  CanvasRenderer
])

// 获取用户状态
const userStore = useUserStore()

// 社交历史记录
const socialHistory = ref([])
const uploadForm = ref({
  type: '兴趣社交',
  socialDate: new Date().toISOString().slice(0, 19).replace('T', ' '),
  duration: 30,
  participants: 5,
  interactions: 10
})
const chatMessages = ref([])
const chatInput = ref('')
const analysisContent = ref('')
const chatScrollbar = ref(null)
const isLoading = ref(false)

// 新增的状态变量
const uploadDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const statistics = ref([
  {
    title: '社交次数',
    value: '15次',
    change: '较上周 +20%',
    trend: 'up',
    icon: 'Calendar'
  },
  {
    title: '社交时长',
    value: '12小时30分',
    change: '较上周 +15%',
    trend: 'up',
    icon: 'Timer'
  },
  {
    title: '互动次数',
    value: '128次',
    change: '较上周 +25%',
    trend: 'up',
    icon: 'ChatDotRound'
  },
  {
    title: '新增好友',
    value: '8人',
    change: '较上周 +40%',
    trend: 'up',
    icon: 'User'
  }
])
const currentPage = ref(1)
const pageSize = ref(10)
const totalRecords = ref(0)
const selectedTraining = ref(null)
const formRef = ref(null)
const currentSocialType = ref('all')  // 默认显示所有类型
const startDate = ref(null)
const endDate = ref(null)
const loading = ref(false)

// 添加趋势统计数据
const trendStats = ref({})

// 添加趋势分析筛选相关的响应式变量
const trendSocialType = ref('兴趣社交') // 默认选择兴趣社交
const trendStartDate = ref(null)
const trendEndDate = ref(null)

// 添加图表引用
const intensityChartRef = ref(null)
const progressChartRef = ref(null)
const timeDistributionChartRef = ref(null)
const goalCompletionChartRef = ref(null)
const comparisonChartRef = ref(null)

// 图表实例
let intensityChart = null
let progressChart = null
let timeDistributionChart = null
let goalCompletionChart = null
let comparisonChart = null

// 添加日期验证和处理函数
const disableStartDate = (time) => {
  if (endDate.value) {
    return time.getTime() > new Date(endDate.value).getTime()
  }
  return false
}

const disableEndDate = (time) => {
  if (startDate.value) {
    return time.getTime() < new Date(startDate.value).getTime()
  }
  return false
}

const handleDateChange = () => {
  // 验证日期是否有效
  if (startDate.value && endDate.value) {
    const start = new Date(startDate.value).getTime()
    const end = new Date(endDate.value).getTime()

    if (start > end) {
      ElMessage.warning('开始日期不能大于结束日期')
      return
    }
  }

  refreshHistory()
}

// 获取统计数据
const fetchStatistics = async () => {
  try {
    const queryParams = {
      type: currentSocialType.value === 'all' ? undefined : currentSocialType.value,
      startDate: startDate.value || undefined,
      endDate: endDate.value || undefined
    }

    const res = await getTrainingStatistics(queryParams)

    if (res.success && res.data) {
      const stats = res.data
      statistics.value = [
        {
          title: '社交次数',
          value: `${stats.totalSessions || 73}次`,
          change: `较上周 ${stats.sessionChange > 0 ? '+' : ''}${stats.sessionChange || 0}%`,
          trend: (stats.sessionChange || 0) >= 0 ? 'up' : 'down',
          icon: 'Calendar'
        },
        {
          title: '社交时长',
          value: `${Math.floor((stats.totalDuration || 0) / 60)}小时${(stats.totalDuration || 0) % 60}分`,
          change: `较上周 ${stats.durationChange > 0 ? '+' : ''}${stats.durationChange || 0}%`,
          trend: (stats.durationChange || 0) >= 0 ? 'up' : 'down',
          icon: 'Timer'
        },
        {
          title: '互动次数',
          value: `${stats.totalInteractions || 0}次`,
          change: `较上周 ${stats.interactionChange > 0 ? '+' : ''}${stats.interactionChange || 0}%`,
          trend: (stats.interactionChange || 0) >= 0 ? 'up' : 'down',
          icon: 'ChatDotRound'
        },
        {
          title: '新增好友',
          value: `${stats.newFriends || 0}人`,
          change: `较上周 ${stats.friendChange > 0 ? '+' : ''}${stats.friendChange || 0}%`,
          trend: (stats.friendChange || 0) >= 0 ? 'up' : 'down',
          icon: 'User'
        }
      ]
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
    ElMessage.error('获取统计数据失败')
  }
}

// 获取社交历史记录
const refreshHistory = async () => {
  loading.value = true
  try {
    const queryParams = {
      page: currentPage.value,
      size: pageSize.value,
      type: currentSocialType.value === 'all' ? undefined : currentSocialType.value,
      startDate: startDate.value || undefined,
      endDate: endDate.value || undefined
    }

    console.log('Sending query params:', queryParams)

    const res = await queryTrainingRecords(queryParams)
    console.log('Query response:', res)

    if (res && res.data) {
      console.log('Processing training data:', res.data)
      // 将数据按时间倒序排列
      const sortedList = [...res.data.list].sort((a, b) => {
        return new Date(b.socialDate) - new Date(a.socialDate)
      })

      socialHistory.value = sortedList.map(record => {
        console.log('Processing record:', record)
        return {
          id: record.id,
          socialDate: record.socialDate,
          duration: Number(record.duration || 0),
          participants: Number(record.participants || 0),
          interactions: Number(record.interactions || 0),
          type: record.type || '未知类型'
        }
      })

      totalRecords.value = Number(res.data.total || 0)
      pageSize.value = Number(res.data.size || 10)
      currentPage.value = Number(res.data.page || 1)

      console.log('Processed training history:', socialHistory.value)

      if (socialHistory.value.length === 0) {
        ElMessage.info('暂无社交记录')
      }
    } else {
      console.warn('Invalid response format:', res)
      socialHistory.value = []
      totalRecords.value = 0
      ElMessage.warning('获取社交记录失败')
    }
  } catch (error) {
    console.error('获取历史记录失败:', error)
    ElMessage.error(error.message || '获取历史记录失败')
    socialHistory.value = []
    totalRecords.value = 0
  } finally {
    loading.value = false
  }
}

// 处理分页变化
const handlePageChange = async (page) => {
  currentPage.value = page
  await refreshHistory()
}

// 处理AI分析
const handleAIAnalysis = async () => {
  try {
    isLoading.value = true
    // 获取社交统计数据
    const statsParams = {
      type: currentSocialType.value === 'all' ? undefined : currentSocialType.value,
      startDate: startDate.value || undefined,
      endDate: endDate.value || undefined
    }

    const statsRes = await getTrainingStatistics(statsParams)

    if (!statsRes.success) {
      throw new Error(statsRes.message || '获取社交统计数据失败')
    }

    // 构建发送给AI的消息
    const statsData = statsRes.data
    const aiPrompt = `
      请根据以下社交数据进行分析并给出建议：
      总社交时长：${Math.floor((statsData.totalDuration || 0) / 60)}小时${(statsData.totalDuration || 0) % 60}分钟
      总互动次数：${statsData.totalInteractions || 0}次
      与上周相比：
      - 社交次数变化：${statsData.sessionChange > 0 ? '+' : ''}${statsData.sessionChange || 0}%
      - 社交时长变化：${statsData.durationChange > 0 ? '+' : ''}${statsData.durationChange || 0}%
      - 互动次数变化：${statsData.interactionChange > 0 ? '+' : ''}${statsData.interactionChange || 0}%
    `

    // 调用AI接口
    const response = await axios.post('/ai/chat', {
      user_id: userStore.userInfo.id,
      question: aiPrompt
    })

    if (response.data.success) {
      // 添加AI回复到聊天记录
      chatMessages.value.push({
        content: response.data.data,
        type: 'ai',
        time: new Date().toLocaleTimeString()
      })
      scrollToBottom()
    } else {
      throw new Error(response.data.message || '获取AI分析失败')
    }
  } catch (error) {
    console.error('AI分析失败:', error)
    ElMessage.error(error.message || 'AI分析失败，请稍后重试')
  } finally {
    isLoading.value = false
  }
}

// 提交社交记录
const handleSubmitTraining = async () => {
  try {
    if (!formRef.value) return
    await formRef.value.validate()

    const trainingData = {
      ...uploadForm.value,
      status: 1,
      type: uploadForm.value.type || 'rowing'
    }

    let res
    if (selectedTraining.value) {
      res = await updateTrainingRecord({
        id: selectedTraining.value.id,
        ...trainingData
      })
    } else {
      res = await submitTrainingRecord(trainingData)
    }

    if (res && (res.success || typeof res === 'string')) {
      ElMessage.success(selectedTraining.value ? '更新成功' : '添加成功')
      uploadDialogVisible.value = false
      // 重置表单
      uploadForm.value = {
        type: '兴趣社交',
        socialDate: new Date().toISOString().slice(0, 19).replace('T', ' '),
        duration: 30,
        participants: 5,
        interactions: 10
      }
      formRef.value.resetFields()
      selectedTraining.value = null
      currentSocialType.value = 'all'  // 重置社交类型筛选
      await refreshHistory()
      await fetchStatistics()
    } else {
      throw new Error(res?.message || (selectedTraining.value ? '更新失败' : '添加失败'))
    }
  } catch (error) {
    console.error('提交社交记录失败:', error)
    ElMessage.error(error.message || '提交失败')
  }
}

// 添加文件预览URL的响应式变量
const previewUrl = ref('')
const previewType = ref('')  // 'image' 或 'video'

// 修改handleUpload函数
const handleUpload = async (file) => {
  try {
    // 只检查文件类型
    const isImage = file.type.startsWith('image/')
    const isVideo = file.type.startsWith('video/')

    if (!isImage && !isVideo) {
      ElMessage.error('只支持图片或视频文件')
      return false
    }

    // 创建本地预览URL
    previewUrl.value = URL.createObjectURL(file)
    previewType.value = isImage ? 'image' : 'video'

    // 显示预览消息
    chatMessages.value.push({
      content: '',
      type: 'user',
      time: new Date().toLocaleTimeString(),
      mediaType: previewType.value,
      mediaUrl: previewUrl.value
    })
    scrollToBottom()

    // 自动触发AI分析
    await sendMessage(true)
  } catch (error) {
    console.error('文件预览失败:', error)
    ElMessage.error('文件预览失败，请重试')
  }
  return false
}

// 修改sendMessage函数
const sendMessage = async (isMediaAnalysis = false) => {
  if ((!chatInput.value.trim() && !isMediaAnalysis) || isLoading.value) return

  if (!isMediaAnalysis) {
    // 普通文本消息
    chatMessages.value.push({
      content: chatInput.value,
      type: 'user',
      time: new Date().toLocaleTimeString()
    })
  }

  // 构造用户问题
  let userQuestion = ''
  if (isMediaAnalysis) {
    userQuestion = '作为一名专业的社交教练，请对以下社交行为进行分析和指导：\n\n' +
      '社交者目前的社交行为：\n' +
      '1. 兴趣社交：\n' +
      '   - 参与兴趣小组或活动\n' +
      '   - 与志同道合的人交流\n' +
      '   - 分享兴趣爱好\n' +
      '   - 建立长期稳定的社交关系\n\n' +
      '2. 活动社交：\n' +
      '   - 参加团队活动或聚会\n' +
      '   - 与不同背景的人交流\n' +
      '   - 扩大社交圈子\n' +
      '   - 享受社交活动带来的乐趣\n\n' +
      '3. 学习社交：\n' +
      '   - 加入学习小组或课程\n' +
      '   - 与专业人士交流\n' +
      '   - 获取新知识和技能\n' +
      '   - 建立专业网络\n\n' +
      '请从专业角度分析这些行为要点，指出存在的问题，并给出具体的改进建议。'
  } else {
    userQuestion = chatInput.value
  }
  chatInput.value = ''

  try {
    isLoading.value = true
    
    // 调用AI接口
    const response = await axios.post('/ai/chat', {
      user_id: userStore.userInfo.id,
      question: userQuestion
    })

    if (!response.data.success) {
      throw new Error(response.data.message || '获取AI回复失败')
    }

    chatMessages.value.push({
      content: response.data.data,
      type: 'ai',
      time: new Date().toLocaleTimeString()
    })
    scrollToBottom()

    // 清理预览URL
    if (previewUrl.value) {
      URL.revokeObjectURL(previewUrl.value)
      previewUrl.value = ''
      previewType.value = ''
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送消息失败，请稍后重试')
  } finally {
    isLoading.value = false
  }
}

// 滚动到底部
const scrollToBottom = () => {
  setTimeout(() => {
    const scrollbar = chatScrollbar.value
    if (scrollbar) {
      scrollbar.setScrollTop(scrollbar.wrapRef.scrollHeight)
    }
  }, 100)
}

// 清空聊天记录
const clearChat = () => {
  chatMessages.value = []
}

// 刷新分析报告
const refreshAnalysis = async () => {
  try {
    const res = await getTrainingStatistics({
      type: 'rowing',
      startDate: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],  // 最近7天
      endDate: new Date().toISOString().split('T')[0]
    })

    if (res.success && res.data) {
      // 使用实际的统计数据生成报告
      // ... 处理统计数据的逻辑
    } else {
      analysisContent.value = `
            <h3>本周社交分析报告</h3>
            <p>您本周共参与了15次社交活动，总时长12小时30分钟，平均每次活动时长0.82小时。相比上周：</p>
            <ul>
              <li>社交次数增加1次</li>
              <li>总时长提升15%</li>
              <li>平均每次活动时长提升3分钟</li>
            </ul>
            <h4>技术分析</h4>
            <p>根据您的社交数据，我们发现：</p>
            <ul>
              <li>兴趣社交参与度最高</li>
              <li>活动社交和学习社交参与度相近</li>
              <li>长社交时间中后半程参与度波动减小</li>
            </ul>
            <h4>建议</h4>
            <ol>
              <li>可以适当增加高强度社交活动的时间</li>
              <li>建议每周安排1-2次专门的学习社交活动</li>
              <li>注意恢复社交的质量，避免过度疲劳</li>
            </ol>
        `
    }
  } catch (error) {
    ElMessage.error('获取分析报告失败')
  }
}

// 添加社交类型名称映射函数
const getSocialTypeName = (type) => {
  return type || '未知类型'
}

// 修改日期格式化函数
const formatDate = (dateString) => {
  if (!dateString) return ''
  try {
    const date = new Date(dateString)
    if (isNaN(date.getTime())) return dateString

    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    }).replace(/\//g, '-')
  } catch (error) {
    console.error('日期格式化错误:', error)
    return dateString
  }
}

// 显示上传对话框
const showUploadDialog = () => {
  uploadDialogVisible.value = true
}

// 删除社交记录
const handleDeleteSocial = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条社交记录吗？', '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })

    loading.value = true
    const res = await deleteTrainingRecord(id)

    if (res.success) {
      ElMessage.success(res.data || '删除成功')
      // 刷新数据
      await refreshHistory()
      await fetchStatistics()
    } else {
      throw new Error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除社交记录失败:', error)
      ElMessage.error(typeof error === 'string' ? error : error.message || '删除失败')
    }
  } finally {
    loading.value = false
  }
}

// 添加表单验证规则
const formRules = {
  type: [{ required: true, message: '请选择社交类型', trigger: 'change' }],
  socialDate: [{ required: true, message: '请选择社交日期', trigger: 'change' }],
  duration: [{ required: true, message: '请输入社交时长', trigger: 'blur' }],
  participants: [{ required: true, message: '请输入参与人数', trigger: 'blur' }]
}

// 在对话框关闭时重置表单
const handleDialogClose = () => {
  formRef.value?.resetFields()
  selectedTraining.value = null
}

// 添加趋势分析日期验证函数
const disableTrendStartDate = (time) => {
  if (trendEndDate.value) {
    return time.getTime() > new Date(trendEndDate.value).getTime()
  }
  return false
}

const disableTrendEndDate = (time) => {
  if (trendStartDate.value) {
    return time.getTime() < new Date(trendStartDate.value).getTime()
  }
  return false
}

// 修改趋势分析筛选条件变化处理函数
const handleTrendFilterChange = async () => {
  try {
    // 验证日期是否有效
    if (trendStartDate.value && trendEndDate.value) {
      const start = new Date(trendStartDate.value).getTime()
      const end = new Date(trendEndDate.value).getTime()
      
      if (start > end) {
        ElMessage.warning('开始日期不能大于结束日期')
        return
      }
    }

    const params = {
      type: trendSocialType.value,
      startDate: trendStartDate.value || undefined,
      endDate: trendEndDate.value || undefined
    }

    console.log('Fetching trend stats with params:', params)

    const res = await getTrainingStatistics(params)
    console.log('Trend stats response:', res)

    if (res && res.success) {
      console.log('Setting trend stats:', res.data)
      trendStats.value = res.data || getDefaultTrendStats()
      // 更新图表
      initCharts()
    } else {
      console.warn('Invalid trend stats response:', res)
      trendStats.value = getDefaultTrendStats()
      if (!res.success) {
        throw new Error(res.message || '获取统计数据失败')
      }
    }
  } catch (error) {
    console.error('获取趋势统计数据失败:', error)
    ElMessage.error(error.message || '获取趋势统计数据失败')
    trendStats.value = getDefaultTrendStats()
  }
}

// 获取默认的趋势统计数据
const getDefaultTrendStats = () => ({
  totalDuration: 0,
  avgDistance: 0,
  minDuration: 0,
  minDistance: 0,
  maxDistance: 0,
  avgCalories: 0,
  totalCalories: 0,
  totalDistance: 0,
  maxDuration: 0,
  avgDuration: 0
})

// 初始化训练强度分布图
const initIntensityChart = (data) => {
  if (!intensityChartRef.value) return

  intensityChart = echarts.init(intensityChartRef.value)
  const option = {
    title: {
      text: '社交类型分布',
      left: 'center'
    },
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '社交类型',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: '20',
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: [
          { value: data.lowIntensity || 20, name: '低强度' },
          { value: data.mediumIntensity || 40, name: '中等强度' },
          { value: data.highIntensity || 30, name: '高强度' }
        ]
      }
    ]
  }
  intensityChart.setOption(option)
}

// 初始化训练进展趋势图
const initProgressChart = (data) => {
  if (!progressChartRef.value) return

  progressChart = echarts.init(progressChartRef.value)
  const option = {
    title: {
      text: '社交活跃度趋势',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#6a7985'
        }
      }
    },
    legend: {
      data: ['互动次数', '参与人数'],
      top: 30
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: [
      {
        type: 'category',
        boundaryGap: false,
        data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
      }
    ],
    yAxis: [
      {
        type: 'value',
        name: '互动次数',
        position: 'left'
      },
      {
        type: 'value',
        name: '参与人数',
        position: 'right'
      }
    ],
    series: [
      {
        name: '互动次数',
        type: 'line',
        smooth: true,
        data: [15, 20, 18, 25, 22, 30, 28]
      },
      {
        name: '参与人数',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: [5, 7, 6, 8, 9, 12, 10]
      }
    ]
  }
  progressChart.setOption(option)
}

// 初始化训练时间分布图
const initTimeDistributionChart = (data) => {
  if (!timeDistributionChartRef.value) return

  timeDistributionChart = echarts.init(timeDistributionChartRef.value)
  const option = {
    title: {
      text: '社交时间分布',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['6-9点', '9-12点', '12-15点', '15-18点', '18-21点', '21-24点']
    },
    yAxis: {
      type: 'value',
      name: '社交次数'
    },
    series: [
      {
        name: '社交频次',
        type: 'bar',
        barWidth: '60%',
        data: [2, 4, 3, 5, 8, 4]
      }
    ]
  }
  timeDistributionChart.setOption(option)
}

// 初始化目标完成度图
const initGoalCompletionChart = (data) => {
  if (!goalCompletionChartRef.value) return

  goalCompletionChart = echarts.init(goalCompletionChartRef.value)
  const option = {
    title: {
      text: '社交目标完成度',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    radar: {
      indicator: [
        { name: '社交时长', max: 100 },
        { name: '互动次数', max: 100 },
        { name: '社交频次', max: 100 },
        { name: '参与人数', max: 100 },
        { name: '新增好友', max: 100 }
      ]
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: [85, 90, 75, 80, 70],
            name: '目标完成度',
            areaStyle: {
              color: 'rgba(64,158,255,0.3)'
            }
          }
        ]
      }
    ]
  }
  goalCompletionChart.setOption(option)
}

// 初始化训练数据对比图
const initComparisonChart = (data) => {
  if (!comparisonChartRef.value) return

  comparisonChart = echarts.init(comparisonChartRef.value)
  const option = {
    title: {
      text: '训练数据周期对比',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['本周期', '上个周期'],
      top: 30
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['训练时长', '训练距离', '消耗卡路里', '平均配速', '训练次数']
    },
    yAxis: {
      type: 'value',
      name: '完成百分比'
    },
    series: [
      {
        name: '本周期',
        type: 'bar',
        data: [120, 110, 105, 95, 115]
      },
      {
        name: '上个周期',
        type: 'bar',
        data: [100, 100, 100, 100, 100]
      }
    ]
  }
  comparisonChart.setOption(option)
}

// 初始化所有图表
const initCharts = () => {
  nextTick(() => {
    initIntensityChart(trendStats.value)
    initProgressChart(trendStats.value)
    initTimeDistributionChart(trendStats.value)
    initGoalCompletionChart(trendStats.value)
    initComparisonChart(trendStats.value)
  })
}

// 监听窗口大小变化，重绘图表
window.addEventListener('resize', () => {
  intensityChart?.resize()
  progressChart?.resize()
  timeDistributionChart?.resize()
  goalCompletionChart?.resize()
  comparisonChart?.resize()
})

// 在组件卸载时销毁图表实例
onUnmounted(() => {
  intensityChart?.dispose()
  progressChart?.dispose()
  timeDistributionChart?.dispose()
  goalCompletionChart?.dispose()
  comparisonChart?.dispose()
})

// 修改表格列的显示
const formatDuration = (minutes) => {
  if (!minutes) return '0分钟'
  // 将分钟数四舍五入到两位小数
  minutes = Number(minutes.toFixed(2))
  const hours = Math.floor(minutes / 60)
  const remainingMinutes = (minutes % 60).toFixed(2)
  return hours > 0 ? `${hours}小时${remainingMinutes}分钟` : `${remainingMinutes}分钟`
}

// 修改页面初始化逻辑
onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([
      refreshHistory(),
      fetchStatistics(),
      handleTrendFilterChange()
    ])
    // 添加初始AI欢迎消息
    chatMessages.value = [
      {
        content: "你好！我是你的AI社交助手。我可以帮你分析社交数据，提供社交建议，帮助你更好地进行社交活动。有什么我可以帮你的吗？",
        type: "ai",
        time: new Date().toLocaleTimeString()
      }
    ]
    // 初始化图表
    initCharts()
  } catch (error) {
    console.error('初始化失败:', error)
    ElMessage.error('数据加载失败，请刷新页面重试')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.dashboard-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 24px;
  background: #f0f2f5;
  min-height: 100vh;
}

.content-wrapper {
  display: flex;
  gap: 24px;
  min-height: 600px; /* 设置最小高度，确保内容对齐 */
}

/* 左侧主面板样式 */
.main-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 24px;
  min-width: 0; /* 防止flex子项溢出 */
}

/* 右侧AI助手面板样式 */
.ai-panel {
  width: 360px;
  background: #fff;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  height: 1030px; /* 固定高度，与左侧内容对齐 */
  flex-shrink: 0; /* 防止被压缩 */
}

/* 社交记录表格区域样式 */
.records-section {
  width: 100%; /* 横向铺满 */
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  margin-top: auto; /* 自动调整顶部间距 */
}

@media (max-width: 1200px) {
  .content-wrapper {
    flex-direction: column;
  }

  .ai-panel {
    width: 100%;
    height: 500px;
  }
}

/* 统计卡片样式 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: var(--el-color-primary-light-9);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-color-primary);
  font-size: 24px;
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-title {
  color: #909399;
  font-size: 14px;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stat-change {
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-change.up {
  color: #67c23a;
}

.stat-change.down {
  color: #f56c6c;
}

/* 分析区域样式 */
.analysis-section,
.records-section {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.section-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.filter-group {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.date-filters {
  display: flex;
  gap: 12px;
}

.action-group {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-left: auto; /* 将按钮组推到右侧 */
}

.filter-item {
  min-width: 140px;
}

/* 概览卡片样式 */
.overview-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.overview-card {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
}

.overview-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--el-color-primary);
  margin-bottom: 8px;
}

.overview-label {
  color: #606266;
  font-size: 14px;
}

/* 图表区域样式 */
.charts-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.chart-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.chart-wrapper {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.chart-wrapper h3 {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px 0;
  text-align: center;
}

.chart {
  height: 300px;
}

/* 表格区域样式 */
.table-container {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.pagination-wrapper {
  padding: 16px;
  display: flex;
  justify-content: flex-end;
  background: #fff;
  border-top: 1px solid #ebeef5;
}

.ai-header {
  padding: 16px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.ai-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.chat-container {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message {
  max-width: 85%;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message.user {
  margin-left: auto;
  align-items: flex-end;
}

.message.ai {
  margin-right: auto;
  align-items: flex-start;
}

.message-content {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  position: relative;
}

.message.user .message-content {
  background: var(--el-color-primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message.ai .message-content {
  background: #f5f7fa;
  color: #303133;
  border-bottom-left-radius: 4px;
}

.message-time {
  font-size: 12px;
  color: #909399;
}

.chat-input-area {
  padding: 12px;
  border-top: 1px solid #ebeef5;
  background: #fff;
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.media-upload {
  flex-shrink: 0;
}

.input-wrapper {
  flex: 1;
}

.input-wrapper :deep(.el-input__wrapper) {
  box-shadow: none;
  border: 1px solid #dcdfe6;
}

.input-wrapper :deep(.el-input__wrapper):hover {
  border-color: var(--el-color-primary);
}

.input-wrapper :deep(.el-input__wrapper.is-focus) {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 1px var(--el-color-primary) inset;
}

.upload-btn {
  height: 32px;
  width: 32px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #dcdfe6;
  transition: all 0.3s;
}

.upload-btn:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}

.typing-indicator {
  display: inline-flex;
  gap: 4px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 12px;
  border-bottom-left-radius: 4px;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  background-color: #909399;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) {
  animation-delay: -0.32s;
}

.typing-indicator span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes bounce {
  0%,
  80%,
  100% {
    transform: scale(0);
  }

  40% {
    transform: scale(1);
  }
}

/* 媒体内容样式 */
.media-content {
  margin-bottom: 8px;
}

.message-image {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
}

.message-video {
  max-width: 240px;
  max-height: 180px;
  border-radius: 8px;
}

/* 响应式布局 */
@media (max-width: 1400px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .chart-row {
    grid-template-columns: 1fr;
  }

  .overview-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 1200px) {
  .content-wrapper {
    flex-direction: column;
  }

  .ai-panel {
    width: 100%;
    height: 500px;
  }
}

@media (max-width: 768px) {
  .dashboard-container {
    padding: 16px;
  }

  .stats-row,
  .overview-cards {
    grid-template-columns: 1fr;
  }

  .filter-group {
    flex-direction: column;
    align-items: stretch;
  }

  .date-filters {
    flex-direction: column;
  }

  .filter-item {
    width: 100%;
  }

  .action-group {
    width: 100%;
    justify-content: flex-end;
    margin-top: 12px;
    gap: 8px;
  }
}
</style>