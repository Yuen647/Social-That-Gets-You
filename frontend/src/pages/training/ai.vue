<template>
  <div class="ai-container">
    <!-- 数字人聊天板块 (放在上面) -->
    <div class="digital-human-section">
      <div class="section-header">
        <h2 class="section-title">
          <el-icon class="mr-2"><ChatDotRound /></el-icon>
          数字人聊天
        </h2>
        <div class="header-controls">
          <el-radio-group v-model="zoomLevel" size="small" class="zoom-controls">
            <el-radio-button label="0.5">50%</el-radio-button>
            <el-radio-button label="0.75">75%</el-radio-button>
            <el-radio-button label="1">100%</el-radio-button>
          </el-radio-group>
          <el-button 
            type="primary" 
            size="small"
            @click="refreshChat"
            class="refresh-btn"
          >
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </div>
      <div class="iframe-container">
        <iframe 
          ref="chatIframe"
          :src="chatUrl" 
          frameborder="0" 
          class="chat-iframe"
          :style="{ zoom: zoomLevel }"
          sandbox="allow-same-origin allow-scripts allow-popups allow-forms"
        ></iframe>
      </div>
    </div>

    <!-- 智能推荐板块 (放在下面) -->
    <div class="recommendation-section mt-4">
      <div class="section-header">
        <h2 class="section-title">
          <el-icon class="mr-2"><MagicStick /></el-icon>
          智能推荐
        </h2>
        <div class="header-controls">
          <el-select v-model="filterType" size="small" placeholder="筛选类型">
            <el-option label="全部" value="all" />
            <el-option label="热门" value="hot" />
            <el-option label="最新" value="new" />
          </el-select>
        </div>
      </div>
      <div class="recommendation-content">
        <div class="posts-list">
          <div v-for="post in mockPosts" :key="post.id" class="post-card">
            <div class="post-header">
              <div class="post-author">
                <el-avatar :size="32" :src="post.author.avatar" />
                <span class="author-name">{{ post.author.name }}</span>
              </div>
              <el-tag size="small" :type="post.type === 'hot' ? 'danger' : 'info'">
                {{ post.type === 'hot' ? '热门' : '推荐' }}
              </el-tag>
            </div>
            <div class="post-content">
              <h3 class="post-title">{{ post.title }}</h3>
              <p class="post-text">{{ post.content }}</p>
              <div class="post-images" v-if="post.images && post.images.length">
                <el-image 
                  v-for="(img, index) in post.images" 
                  :key="index"
                  :src="img"
                  :preview-src-list="post.images"
                  fit="cover"
                  class="post-image"
                />
              </div>
            </div>
            <div class="post-footer">
              <div class="post-stats">
                <span class="stat-item">
                  <el-icon><View /></el-icon>
                  {{ post.stats.views }}
                </span>
                <span class="stat-item">
                  <el-icon><ChatDotRound /></el-icon>
                  {{ post.stats.comments }}
                </span>
                <span class="stat-item">
                  <el-icon><Star /></el-icon>
                  {{ post.stats.likes }}
                </span>
              </div>
              <div class="post-actions">
                <el-button size="small" type="primary" plain>查看详情</el-button>
                <el-button size="small" type="success" plain>收藏</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { MagicStick, ChatDotRound, Refresh, View, Star } from '@element-plus/icons-vue'

// 数字人聊天页面的URL
const chatUrl = 'https://www.baidu.com' // 示例使用百度链接
const chatIframe = ref(null)
const zoomLevel = ref('1') // 默认100%缩放

// 刷新聊天iframe
const refreshChat = () => {
  if (chatIframe.value) {
    chatIframe.value.src = chatUrl
  }
}

const filterType = ref('all')

// 模拟帖子数据
const mockPosts = ref([
  {
    id: 1,
    type: 'hot',
    author: {
      name: 'AI助手',
      avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
    },
    title: '如何提高社交媒体互动率？',
    content: '通过分析大量数据，我们发现以下几个关键点可以显著提升社交媒体互动率：1. 发布时间的选择 2. 内容形式的多样性 3. 互动引导的重要性...',
    images: [
      'https://fuss10.elemecdn.com/e/5d/4a731a90594a4af544c0c25941171jpeg.jpeg',
      'https://fuss10.elemecdn.com/1/34/19aa98b1fcb2781c4fba33d850549jpeg.jpeg'
    ],
    stats: {
      views: 1234,
      comments: 56,
      likes: 789
    }
  },
  {
    id: 2,
    type: 'recommended',
    author: {
      name: '数据分析师',
      avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
    },
    title: '社交媒体内容策略指南',
    content: '基于最新的用户行为分析，我们总结出了一套完整的社交媒体内容策略，包括内容规划、发布频率、互动管理等关键要素...',
    images: [
      'https://fuss10.elemecdn.com/9/bb/e27858e973f5d7d3904835f46abbdjpeg.jpeg'
    ],
    stats: {
      views: 856,
      comments: 34,
      likes: 567
    }
  }
])
</script>

<style scoped>
.ai-container {
  padding: 24px;
  min-height: 100vh;
  background-color: #f5f7fa;
  max-width: 1400px;
  margin: 0 auto;
  overflow-y: auto; /* 允许垂直滚动 */
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 20px;
  background-color: white;
  border-radius: 4px 4px 0 0;
  border-bottom: 1px solid #ebeef5;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.digital-human-section {
  height: 80vh;
  background-color: white;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  margin-bottom: 24px; /* 添加底部间距 */
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.zoom-controls {
  margin-right: 8px;
}

.iframe-container {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.chat-iframe {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border: none;
}

.recommendation-section {
  min-height: 300px; /* 设置最小高度 */
  background-color: white;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
}

.recommendation-content {
  flex: 1;
  overflow: auto;
  padding: 16px;
}

.posts-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-card {
  background: white;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.post-author {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author-name {
  font-weight: 500;
  color: #303133;
}

.post-content {
  margin-bottom: 16px;
}

.post-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.post-text {
  margin: 0 0 12px 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
}

.post-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.post-image {
  width: 120px;
  height: 120px;
  border-radius: 4px;
  object-fit: cover;
}

.post-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.post-stats {
  display: flex;
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 13px;
}

.post-actions {
  display: flex;
  gap: 8px;
}

.refresh-btn {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.mr-2 {
  margin-right: 8px;
}

.mt-4 {
  margin-top: 16px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .ai-container {
    padding: 12px;
  }
  
  .digital-human-section {
    height: 80vh;
  }
  
  .recommendation-section {
    height: calc(15vh - 80px);
  }
  
  .post-footer {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  
  .post-actions {
    width: 100%;
    justify-content: space-between;
  }
  
  .post-image {
    width: 100px;
    height: 100px;
  }
}
</style> 