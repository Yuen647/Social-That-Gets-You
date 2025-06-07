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
      </div>
      <div class="recommendation-content">
        <!-- 智能推荐内容将在这里实现 -->
        <div class="placeholder-content">
          智能推荐功能开发中...
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { MagicStick, ChatDotRound, Refresh } from '@element-plus/icons-vue'

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
</script>

<style scoped>
.ai-container {
  padding: 24px;
  min-height: 100vh;
  background-color: #f5f7fa;
  max-width: 1400px;
  margin: 0 auto;
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
  height: calc(10vh - 80px);
  background-color: white;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
}

.recommendation-content {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.placeholder-content {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 16px;
  background-color: #f8f9fa;
  border-radius: 0 0 4px 4px;
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
}
</style> 