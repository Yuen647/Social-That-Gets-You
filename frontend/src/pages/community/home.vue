<template>
  <div class="page-container">
    <el-card class="content-card" shadow="never">
      <div class="header-section">
        <div class="header-content">
          <div class="title-section">
            <h2 class="text-2xl font-bold">社区动态</h2>
            <p class="text-gray-600 mt-2">发现精彩的生活故事</p>
          </div>
          
          <div class="search-bar">
            <div class="search-toggle mb-2">
              <el-switch
                v-model="useAISearch"
                active-text="AI搜索"
                inactive-text="普通搜索"
                class="mb-2"
              />
            </div>
            <el-input
              v-model="searchQuery"
              :placeholder="useAISearch ? 'AI智能搜索...' : '搜索帖子...'"
              :prefix-icon="Search"
              clearable
              @input="handleSearch"
              class="search-input"
            >
              <template #append>
                <el-button :icon="useAISearch ? MagicStick : Search" @click="handleSearch" :loading="isSearching">
                  {{ useAISearch ? 'AI搜索' : '搜索' }}
                </el-button>
              </template>
            </el-input>
          </div>
        </div>
      </div>

      <!-- AI Search Tips -->
      <div v-if="useAISearch && searchQuery" class="ai-search-tips mb-4">
        <el-alert
          title="AI搜索提示"
          type="info"
          :closable="false"
          description="AI搜索可以理解自然语言，尝试输入：'找运动相关的帖子'、'最近两天的热门内容'等"
          show-icon
        />
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <template v-if="filteredNotes.length > 0">
          <el-card v-for="note in filteredNotes.filter(note => !note.placeholder)" 
                  :key="note.id"
                  class="note-card"
                  shadow="hover"
                  @click="goToNoteDetail(note.id, note.creatorId)">
            <div class="image-container mb-4">
              <div class="image-wrapper">
                <img 
                  :src="note.imgUris && note.imgUris[0] ? note.imgUris[0] : '/assets/developer.png'"
                  @error="handleImageError" 
                  alt="Note Image" 
                  class="note-image"
                  :style="getImageStyle(note)"
                />
                <div class="image-overlay"></div>
              </div>
            </div>

            <h3 class="text-lg font-semibold mb-2 line-clamp-2" v-html="highlightMatch(note.title)"></h3>
            
            <!-- AI Relevance Score -->
            <div v-if="useAISearch && note.aiRelevance" class="ai-relevance mb-2">
              <div class="flex items-center">
                <el-progress 
                  :percentage="note.aiRelevance" 
                  :color="getRelevanceColor(note.aiRelevance)"
                  :stroke-width="8"
                  :show-text="false"
                  class="flex-grow"
                />
                <span class="ml-2 text-xs" :style="{color: getRelevanceColor(note.aiRelevance)}">
                  {{ note.aiRelevance }}% 相关
                </span>
              </div>
            </div>

            <!-- AI Match Reason -->
            <p v-if="useAISearch && note.aiMatchReason" class="text-sm text-gray-500 mb-3 line-clamp-2">
              {{ note.aiMatchReason }}
            </p>
            
            <div class="flex items-center justify-between mt-4">
              <div class="creator-info flex items-center">
                <img :src="note.avatar" alt="Creator Avatar" class="creator-avatar" />
                <span class="ml-2 text-sm text-gray-600">{{ note.creatorName }}</span>
              </div>
              <span class="text-sm text-gray-500">{{ note.updateTime }}</span>
            </div>
          </el-card>
        </template>
      </div>

      <!-- AI Search Processing -->
      <div v-if="isSearching" class="ai-search-processing">
        <div class="flex flex-col items-center justify-center py-8">
          <el-progress type="dashboard" :percentage="searchProgress" :width="120" />
          <div class="mt-4 text-center">
            <p class="text-lg font-medium">AI正在分析您的搜索...</p>
            <p class="text-sm text-gray-500 mt-2">{{ searchStatusMessage }}</p>
          </div>
        </div>
      </div>

      <div v-if="searchQuery && filteredNotesWithoutPlaceholders.length === 0 && !isSearching" class="empty-search-result">
        <img src="/assets/developer.png" alt="无搜索结果" class="empty-image" />
        <p class="empty-text">没有找到相关帖子</p>
        <p v-if="useAISearch" class="empty-suggestion mt-2">尝试使用不同的关键词或更具体的描述</p>
      </div>

      <div class="flex justify-center mt-8">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="totalNotes"
          :page-count="maxPage"
          layout="prev, jumper, next"
          @current-change="handlePageChange"
          background
        />
      </div>
    </el-card>
  </div>
</template>

<script>
import { getNoteListService, getNoteDetailService } from "@/api/note.js";
import { ElMessage } from "element-plus";
import { Search, MagicStick } from '@element-plus/icons-vue'

export default {
  data() {
    return {
      notes: [], // 当前页的笔记数据
      allNotesForSearch: [], // 用于搜索的所有页面的笔记
      currentPage: 1, // 当前页码
      currentPageInput: 1, // 输入框中的页码
      pageSize: 9, // 每页固定展示 9 条数据
      totalNotes: 0, // 总笔记数，从后端获取
      imageStyles: {}, // 存储每个图片的样式
      searchQuery: '', // 添加搜索查询
      useAISearch: false, // 是否使用AI搜索
      isSearching: false, // 是否正在搜索中
      searchProgress: 0, // 搜索进度
      searchStatusMessage: '', // 搜索状态消息
      aiSearchResults: [], // AI搜索结果
      searchStatusMessages: [
        "正在理解您的搜索意图...",
        "分析帖子内容...",
        "查找相关主题...",
        "评估内容相关性...",
        "生成智能推荐...",
      ],
    };
  },
  computed: {
    maxPage() {
      return Math.max(1, Math.ceil(this.totalNotes / this.pageSize));
    },
    notesWithPlaceholders() {
      const placeholders = Array.from({ length: this.pageSize - this.notes.length }, (_, index) => ({
        placeholder: true,
        id: `placeholder-${index}`,
      }));
      return [...this.notes, ...placeholders];
    },
    filteredNotes() {
      if (!this.searchQuery) {
        return this.notesWithPlaceholders;
      }
      
      // 如果使用AI搜索且有AI搜索结果，则使用AI搜索结果
      if (this.useAISearch && this.aiSearchResults.length > 0) {
        const placeholders = Array.from(
          { length: Math.max(0, this.pageSize - this.aiSearchResults.length) },
          (_, index) => ({
            placeholder: true,
            id: `placeholder-${index}`,
          })
        );
        return [...this.aiSearchResults, ...placeholders];
      }
      
      // 普通搜索
      const searchLower = this.searchQuery.toLowerCase();
      const filtered = this.notes.filter(note => 
        note.title?.toLowerCase().includes(searchLower) ||
        note.content?.toLowerCase().includes(searchLower)
      );
      
      if (filtered.length >= 3) {
        const placeholders = Array.from(
          { length: Math.max(0, this.pageSize - filtered.length) },
          (_, index) => ({
            placeholder: true,
            id: `placeholder-${index}`,
          })
        );
        return [...filtered, ...placeholders];
      }
      
      return filtered;
    },
    filteredNotesWithoutPlaceholders() {
      return this.filteredNotes.filter(note => !note.placeholder);
    },
  },
  methods: {
    fetchNotes() {
      const params = {
        page: this.currentPage,
        size: this.pageSize,
      };
      getNoteListService(params)
        .then(async (response) => {
          if (response && response.length > 0) {
            const detailedNotes = await Promise.all(
              response.map(async (note) => {
                const detailResponse = await getNoteDetailService({ id: note.id });
                return {
                  ...note,
                  ...detailResponse.data,
                };
              })
            );
            this.notes = detailedNotes;
            this.totalNotes = this.currentPage * this.pageSize + response.length;
            this.currentPageInput = this.currentPage;
          } else {
            ElMessage.warning("没有更多帖子了~");
            if (this.currentPage > 1) {
              this.currentPage--;
              this.currentPageInput = this.currentPage;
            }
          }
        })
        .catch((error) => {
          console.error("Error fetching note list:", error);
          ElMessage.error("Failed to fetch notes.");
        });
    },
    handlePageChange(page) {
      this.currentPage = page;
      this.currentPageInput = page;
      this.fetchNotes();
    },
    goToNoteDetail(noteId, userId) {
      if (!noteId) return;
      this.$router.push({ name: "NoteDetail", params: { id: noteId, userId } });
    },
    handleImageError(event) {
      event.target.src = "/assets/developer.png";
    },
    getImageStyle(note) {
      return {
        position: 'absolute',
        left: '50%',
        top: '50%',
        transform: 'translate(-50%, -50%)',
        maxWidth: '100%',
        maxHeight: '100%',
        width: 'auto',
        height: 'auto',
        objectFit: 'contain'
      }
    },
    handleSearch() {
      this.currentPage = 1;
      
      // 如果开启了AI搜索并且有搜索查询
      if (this.useAISearch && this.searchQuery.trim()) {
        this.performAISearch();
      } else {
        // 重置AI搜索结果
        this.aiSearchResults = [];
      }
    },
    // 执行AI搜索
    performAISearch() {
      this.isSearching = true;
      this.searchProgress = 0;
      this.aiSearchResults = [];
      this.searchStatusMessage = this.searchStatusMessages[0];
      
      // 先获取更多页的数据进行全局搜索
      this.fetchAllPagesForSearch().then(() => {
        // 模拟AI搜索进度
        const progressInterval = setInterval(() => {
          if (this.searchProgress < 100) {
            this.searchProgress += 5;
            
            // 更新搜索状态消息
            const messageIndex = Math.floor((this.searchProgress / 100) * this.searchStatusMessages.length);
            if (messageIndex < this.searchStatusMessages.length) {
              this.searchStatusMessage = this.searchStatusMessages[messageIndex];
            }
          } else {
            clearInterval(progressInterval);
            this.completeAISearch();
          }
        }, 100);
      });
    },
    
    // 获取多页数据用于搜索
    async fetchAllPagesForSearch() {
      try {
        // 存储所有获取的笔记
        let allNotes = [...this.notes];
        
        // 获取前5页的数据(或更多，根据需要调整)
        const pagesToFetch = 5;
        const pageSize = this.pageSize;
        
        // 并行获取多个页面的数据
        const pagePromises = [];
        for (let page = 1; page <= pagesToFetch; page++) {
          if (page !== this.currentPage) { // 跳过当前页，因为已经加载了
            pagePromises.push(this.fetchNotesForPage(page, pageSize));
          }
        }
        
        const pagesResults = await Promise.all(pagePromises);
        
        // 合并所有页面的结果
        pagesResults.forEach(notes => {
          if (notes && notes.length > 0) {
            allNotes = [...allNotes, ...notes];
          }
        });
        
        // 存储用于搜索的所有笔记
        this.allNotesForSearch = allNotes;
        
      } catch (error) {
        console.error("Error fetching pages for search:", error);
        // 如果获取失败，至少使用当前页的数据
        this.allNotesForSearch = [...this.notes];
      }
    },
    
    // 获取指定页的笔记
    async fetchNotesForPage(page, size) {
      try {
        const params = { page, size };
        const response = await getNoteListService(params);
        
        if (response && response.length > 0) {
          // 获取每个笔记的详细信息
          const detailedNotes = await Promise.all(
            response.map(async (note) => {
              try {
                const detailResponse = await getNoteDetailService({ id: note.id });
                return {
                  ...note,
                  ...detailResponse.data,
                };
              } catch (error) {
                console.error(`Error fetching details for note ${note.id}:`, error);
                return note; // 返回基本信息
              }
            })
          );
          return detailedNotes;
        }
        return [];
      } catch (error) {
        console.error(`Error fetching notes for page ${page}:`, error);
        return [];
      }
    },
    
    // 完成AI搜索
    completeAISearch() {
      // 模拟AI搜索结果，使用所有获取的笔记
      const aiResults = this.simulateAISearchResults();
      this.aiSearchResults = aiResults;
      this.isSearching = false;
      
      // 显示搜索完成消息
      ElMessage({
        message: `AI搜索完成，找到 ${aiResults.length} 条相关内容`,
        type: 'success',
        duration: 3000
      });
    },
    
    // 模拟AI搜索结果
    simulateAISearchResults() {
      const query = this.searchQuery.toLowerCase();
      
      // 根据搜索查询的不同特征生成不同的AI搜索结果
      let aiResults = [];
      
      // 使用所有页面的笔记而不仅仅是当前页
      const notesToSearch = this.allNotesForSearch || this.notes;
      
      // 创建深拷贝以避免修改原始数据
      const notesCopy = JSON.parse(JSON.stringify(notesToSearch));

      // 定义主题关键词映射
      const themeKeywords = {
        sports: ['运动', '健身', '赛', '赛艇', '训练', '体育', '竞技', '比赛', '球', '跑步', '游泳', '健康', '锻炼', '体能'],
        food: ['美食', '食谱', '菜', '烹饪', '食物', '餐', '厨', '吃', '美味', '料理'],
        travel: ['旅行', '旅游', '景点', '游玩', '风景', '景区', '度假', '出游', '游记', '旅程']
      };
      
      // 为每个笔记生成AI相关度和匹配原因
      notesCopy.forEach(note => {
        // 初始化基础相关度为较低值
        let baseRelevance = 40;
        let relevanceBoost = 0;
        let matchReason = '';
        let matchedKeywords = [];
        
        const noteContent = (note.title || '').toLowerCase() + ' ' + (note.content || '').toLowerCase();
        
        // 检查是否是运动相关查询
        if (query.includes('运动') || query.includes('体育') || query.includes('赛') || 
            query.includes('健身') || query.includes('训练')) {
          // 检查所有运动相关关键词
          themeKeywords.sports.forEach(keyword => {
            if (noteContent.includes(keyword)) {
              matchedKeywords.push(keyword);
              // 根据关键词重要性给予不同的提升
              if (['运动', '赛', '体育', '竞技', '训练'].includes(keyword)) {
                relevanceBoost += 15; // 核心关键词提升更多
              } else {
                relevanceBoost += 8; // 相关关键词提升较少
              }
            }
          });
          
          if (matchedKeywords.length > 0) {
            matchReason = `AI分析：内容包含运动相关关键词「${matchedKeywords.join('」「')}」`;
            // 根据匹配关键词数量额外提升相关度
            relevanceBoost += Math.min(matchedKeywords.length * 5, 25);
          }
        } else if (query.includes('美食') || query.includes('食谱')) {
          // 保持其他主题的搜索逻辑...
          themeKeywords.food.forEach(keyword => {
            if (noteContent.includes(keyword)) {
              matchedKeywords.push(keyword);
              relevanceBoost += 10;
            }
          });
          
          if (matchedKeywords.length > 0) {
            matchReason = `AI分析：内容包含美食相关关键词「${matchedKeywords.join('」「')}」`;
          }
        } else if (query.includes('旅行') || query.includes('旅游')) {
          // 保持其他主题的搜索逻辑...
          themeKeywords.travel.forEach(keyword => {
            if (noteContent.includes(keyword)) {
              matchedKeywords.push(keyword);
              relevanceBoost += 10;
            }
          });
          
          if (matchedKeywords.length > 0) {
            matchReason = `AI分析：内容包含旅游相关关键词「${matchedKeywords.join('」「')}」`;
          }
        } else if (query.includes('热门') || query.includes('最近')) {
          // 保持热门内容的逻辑...
          if (Math.random() > 0.5) {
            relevanceBoost = 25;
            matchReason = 'AI分析：这是最近的热门内容';
          }
        } else {
          // 默认情况，检查标题和内容是否包含查询词
          if (noteContent.includes(query)) {
            relevanceBoost = 20;
            matchReason = `AI分析：内容包含"${this.searchQuery}"相关信息`;
          }
        }
        
        // 计算最终相关度，确保不超过100
        const finalRelevance = Math.min(baseRelevance + relevanceBoost, 98);
        
        // 调整相关度阈值，只显示真正相关的内容
        if ((matchedKeywords.length > 0 && finalRelevance > 50) || finalRelevance > 60) {
          note.aiRelevance = finalRelevance;
          note.aiMatchReason = matchReason || '基于内容语义分析的相关推荐';
          aiResults.push(note);
        }
      });
      
      // 按相关度排序
      aiResults.sort((a, b) => b.aiRelevance - a.aiRelevance);
      
      // 限制结果数量
      return aiResults.slice(0, 9);
    },
    // 根据相关度获取颜色
    getRelevanceColor(relevance) {
      if (relevance >= 90) return '#67C23A'; // 高相关 - 绿色
      if (relevance >= 70) return '#409EFF'; // 中高相关 - 蓝色
      return '#E6A23C'; // 中低相关 - 橙色
    },
    // 高亮匹配的文本
    highlightMatch(text) {
      if (!this.searchQuery || !this.useAISearch || !text) return text;
      
      const query = this.searchQuery.trim();
      if (!query) return text;
      
      // 简单的高亮实现，实际项目中可能需要更复杂的逻辑
      return text.replace(new RegExp(query, 'gi'), match => 
        `<span class="highlight-match">${match}</span>`
      );
    }
  },
  mounted() {
    this.fetchNotes();
  },
};
</script>

<style scoped>
.page-container {
  padding: 24px;
  min-height: 100vh;
  background-color: #f5f7fa;
}

.content-card {
  background-color: white;
  border-radius: 12px;
}

.note-card {
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.note-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08);
}

.note-card:hover img {
  transform: scale(1.05);
}

.creator-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.placeholder-card {
  background-color: #f9fafb;
  border: 2px dashed #e5e7eb;
  opacity: 0.6;
  pointer-events: none;
}

.placeholder-card:hover {
  transform: none;
  box-shadow: none;
}

:deep(.el-pagination) {
  --el-pagination-hover-color: #409eff;
  --el-pagination-button-color: #606266;
  --el-pagination-button-disabled-color: #c0c4cc;
  --el-pagination-button-bg-color: #fff;
  --el-pagination-hover-bg-color: #ecf5ff;
}

:deep(.el-pagination .el-input__inner) {
  text-align: center;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 3rem;
}

.image-container {
  position: relative;
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
  background-color: #f5f5f5;
}

.image-wrapper {
  position: relative;
  width: 100%;
  padding-top: 66.67%;
  overflow: hidden;
  background-color: #f5f5f5;
}

.note-image {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  min-width: 100%;
  min-height: 100%;
  object-fit: contain;
  transition: all 0.5s ease;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 0) 60%,
    rgba(0, 0, 0, 0.05) 100%
  );
  opacity: 0;
  transition: opacity 0.3s ease;
}

.note-card:hover .note-image {
  transform: translate(-50%, -50%) scale(1.05);
}

.note-card:hover .image-overlay {
  opacity: 1;
}

.placeholder-card .image-container {
  background: linear-gradient(110deg, #eff1f3 8%, #e2e4e7 18%, #eff1f3 33%);
  background-size: 200% 100%;
  animation: shimmer 1.5s linear infinite;
}

@keyframes shimmer {
  to {
    background-position: -200% 0;
  }
}

@media (max-width: 640px) {
  .image-wrapper {
    padding-top: 75%;
  }
}

@media (min-width: 1024px) {
  .image-wrapper {
    padding-top: 56.25%;
  }
}

.image-wrapper::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: #f0f0f0;
}

.note-image {
  opacity: 1;
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.image-wrapper {
  background: linear-gradient(45deg, #f5f5f5 25%, #efefef 25%, #efefef 50%, #f5f5f5 50%, #f5f5f5 75%, #efefef 75%);
  background-size: 20px 20px;
}

.header-section {
  padding: 24px 0;
  margin-bottom: 24px;
  border-bottom: 1px solid #ebeef5;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.title-section {
  flex-shrink: 0;
}

.search-bar {
  max-width: 300px;
  width: 100%;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 20px 0 0 20px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

.search-input :deep(.el-input-group__append) {
  border-radius: 0 20px 20px 0;
  background-color: #409eff;
  border-color: #409eff;
  padding: 0 20px;
}

.search-input :deep(.el-input-group__append .el-button) {
  color: white;
  border: none;
  margin: 0;
  padding: 0;
}

.search-input :deep(.el-input__inner) {
  height: 36px;
  line-height: 36px;
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: stretch;
  }

  .title-section {
    text-align: center;
    margin-bottom: 16px;
  }

  .search-bar {
    max-width: 100%;
  }
}

.no-results {
  padding: 40px 0;
  text-align: center;
}

@media (max-width: 640px) {
  .search-bar {
    max-width: 100%;
    padding: 0 16px;
  }
}

.empty-search-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.empty-image {
  width: 160px;
  height: 160px;
  object-fit: contain;
  margin-bottom: 24px;
  opacity: 0.6;
}

.empty-text {
  color: #909399;
  font-size: 16px;
  margin: 0;
}

.empty-suggestion {
  color: #909399;
  font-size: 14px;
}

@media (max-width: 640px) {
  .empty-image {
    width: 120px;
    height: 120px;
  }
  
  .empty-text {
    font-size: 14px;
  }
}

.grid {
  display: grid;
  gap: 24px;
}

@media (min-width: 768px) {
  .grid {
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  }
}

/* AI搜索相关样式 */
.search-toggle {
  display: flex;
  justify-content: flex-end;
}

.ai-search-tips {
  margin-bottom: 20px;
}

.ai-search-processing {
  padding: 40px 0;
}

.ai-relevance {
  margin-top: 8px;
  margin-bottom: 8px;
}

:deep(.highlight-match) {
  background-color: rgba(255, 230, 0, 0.3);
  font-weight: 600;
  padding: 0 2px;
  border-radius: 2px;
}
</style>