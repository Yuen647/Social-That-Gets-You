<template>
    <div class="welcome-page mt-[10px]">
        <!-- 顶部大图 Banner，添加视差滚动效果 -->
        <div class="hero-section relative h-[600px] overflow-hidden" ref="heroSection">
            <div class="absolute inset-0 bg-gradient-to-r from-blue-900/80 to-transparent z-10"></div>
            <div class="parallax-bg" 
                 :style="{ transform: `translateY(${scrollY * 0.5}px)` }">
                <img src="@/assets/images/social-banner.jpg" alt="Social Platform" class="w-full h-[700px] object-cover">
            </div>
            <div class="absolute top-1/2 left-20 transform -translate-y-1/2 z-20 text-white"
                 :style="{ transform: `translate3d(0, ${-scrollY * 0.2}px, 0)` }">
                <h1 class="text-6xl font-bold mb-6 animate-fade-in-up">发现志同道合的朋友，分享精彩生活</h1>
   
                <el-button type="primary" size="large" class="animate-fade-in-up animation-delay-400 hover:scale-105 transition-transform"
                    @click="goToCommunity">
                    立即加入
                </el-button>
            </div>
        </div>

        <!-- 统计数据展示 -->
        <div class="stats-section relative -mt-20 z-20 mb-20">
            <div class="max-w-7xl mx-auto px-8">
                <div class="grid grid-cols-1 md:grid-cols-4 gap-8">
                    <div v-for="stat in stats" 
                         :key="stat.label" 
                         class="stat-card bg-white rounded-xl p-6 shadow-xl"
                         v-motion
                         :initial="{ opacity: 0, y: 50 }"
                         :enter="{ opacity: 1, y: 0 }"
                         :delay="200"
                         :when-in-view="true">
                        <div class="flex items-center justify-center mb-4">
                            <div class="stat-icon">
                                <el-icon class="text-3xl text-blue-600">
                                    <component :is="stat.icon" />
                                </el-icon>
                            </div>
                        </div>
                        <div class="text-4xl font-bold mb-2 text-center text-gray-800">
                            <CountUp :endVal="stat.value" :duration="2.5" />{{ stat.suffix }}
                        </div>
                        <div class="text-center text-gray-600">{{ stat.label }}</div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 特色功能区 -->
        <div class="features-section py-20 px-8 bg-gradient-to-b from-white to-gray-50">
            <h2 class="text-4xl font-bold text-center mb-16 relative">
                平台特色
                <div class="absolute w-20 h-1 bg-blue-600 bottom-0 left-1/2 transform -translate-x-1/2 mt-4"></div>
            </h2>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-12 max-w-7xl mx-auto">
                <div v-for="(feature, index) in features" 
                     :key="index"
                     class="feature-card text-center p-8 rounded-xl bg-white"
                     v-motion
                     :initial="{ opacity: 0, y: 100 }"
                     :enter="{ opacity: 1, y: 0, scale: 1 }"
                     :delay="index * 200">
                    <div class="icon-wrapper mb-6 mx-auto">
                        <el-icon class="text-7xl text-blue-600">
                            <component :is="feature.icon"></component>
                        </el-icon>
                    </div>
                    <h3 class="text-2xl font-semibold mb-4">{{ feature.title }}</h3>
                    <p class="text-gray-600 leading-relaxed">{{ feature.description }}</p>
                </div>
            </div>
        </div>

        <!-- 最新动态区 -->
        <div class="news-section py-20 px-8 bg-gray-50">
            <h2 class="text-4xl font-bold text-center mb-16 relative">
                社区动态
                <div class="absolute w-20 h-1 bg-blue-600 bottom-0 left-1/2 transform -translate-x-1/2 mt-4"></div>
            </h2>
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8 max-w-7xl mx-auto">
                <div v-for="(news, index) in latestNews" 
                     :key="index" 
                     class="news-card bg-white rounded-xl overflow-hidden shadow-lg cursor-pointer"
                     @click="openNewsLink(news.link)"
                     v-motion
                     :initial="{ opacity: 0, scale: 0.8 }"
                     :enter="{ opacity: 1, scale: 1 }"
                     :delay="index * 200">
                    <div class="relative overflow-hidden group">
                        <img :src="news.image" :alt="news.title" class="w-full h-56 object-cover transform group-hover:scale-110 transition-transform duration-500">
                        <div class="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent opacity-0 group-hover:opacity-100 transition-opacity"></div>
                    </div>
                    <div class="p-6">
                        <h3 class="text-xl font-semibold mb-3 hover:text-blue-600 transition-colors">{{ news.title }}</h3>
                        <p class="text-gray-600">{{ news.description }}</p>
                        <div class="mt-4 flex items-center justify-between">
                            <div class="flex items-center text-sm text-gray-500">
                                <el-icon class="mr-2"><Calendar /></el-icon>
                                {{ news.date }}
                            </div>
                            <el-button type="text" class="text-blue-600 hover:text-blue-800">
                                阅读更多
                                <el-icon class="ml-1">
                                    <ArrowRight />
                                </el-icon>
                            </el-button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { 
    Trophy, UserFilled, Histogram, Calendar,
    User, Medal, DataLine, Star, ArrowRight
} from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import CountUp from 'vue-countup-v3'

const router = useRouter()
const scrollY = ref(0)
const heroSection = ref(null)

const handleScroll = () => {
    scrollY.value = window.scrollY
}

onMounted(() => {
    window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
    window.removeEventListener('scroll', handleScroll)
})

const goToCommunity = () => {
    router.push('/main/community/home')
}

const features = [
    {
        icon: 'UserFilled',
        title: '智能匹配',
        description: '基于兴趣和性格的智能社交匹配，帮你找到志同道合的朋友。'
    },
    {
        icon: 'Trophy',
        title: '兴趣社区',
        description: '加入你感兴趣的社区，结识新朋友，分享生活点滴。'
    },
    {
        icon: 'Histogram',
        title: '实时互动',
        description: '随时随地与好友保持联系，分享精彩瞬间。'
    }
]

const latestNews = ref([
    {
        title: '2024春季社交活动预告',
        description: '即将开启的春季社交活动，带来更多精彩互动',
        image: new URL('@/assets/images/news-1.jpg', import.meta.url).href,
        date: '2024-12-31',
        link: '#'
    },
    {
        title: '社交技巧分享',
        description: '专业导师为您解析社交技巧要点',
        image: new URL('@/assets/images/news-2.jpg', import.meta.url).href,
        date: '2024-11-11',
        link: '#'
    },
    {
        title: '社区活动回顾',
        description: '回顾上周末精彩的社区联谊活动',
        image: new URL('@/assets/images/news-3.jpg', import.meta.url).href,
        date: '2024-12-05',
        link: '#'
    }
])

const stats = [
    { 
        value: 200, 
        suffix: '+', 
        label: '活跃用户',
        icon: User
    },
    { 
        value: 20, 
        suffix: '+', 
        label: '兴趣社区',
        icon: Medal
    },
    { 
        value: 1000, 
        suffix: '+', 
        label: '社交互动',
        icon: DataLine
    },
    { 
        value: 95, 
        suffix: '%', 
        label: '用户好评',
        icon: Star
    }
]

const openNewsLink = (link) => {
    if (link) {
        window.open(link, '_blank')
    }
}
</script>

<style scoped>
.parallax-bg {
    position: absolute;
    top: -50px;
    left: 0;
    right: 0;
    bottom: -50px;
    z-index: 1;
}

.feature-card {
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
    transition: all 0.3s ease;
}

.feature-card:hover {
    transform: translateY(-10px);
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
}

.icon-wrapper {
    width: 80px;
    height: 80px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: linear-gradient(135deg, #e6f0ff 0%, #f0f7ff 100%);
    box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.icon-wrapper .el-icon {
    transform: scale(1.2);
    transition: transform 0.3s ease;
}

.feature-card:hover .icon-wrapper .el-icon {
    transform: scale(1.4);
}

@keyframes fadeInUp {
    from {
        opacity: 0;
        transform: translateY(20px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.animate-fade-in-up {
    animation: fadeInUp 0.6s ease-out forwards;
}

.animation-delay-200 {
    animation-delay: 0.2s;
}

.animation-delay-400 {
    animation-delay: 0.4s;
}

/* 添加平滑滚动 */
.welcome-page {
    scroll-behavior: smooth;
}

.stat-card {
    backdrop-filter: blur(10px);
    background-color: rgba(255, 255, 255, 0.95);
    transition: all 0.3s ease;
}

.stat-card:hover {
    transform: translateY(-5px);
    box-shadow: 0 25px 50px rgba(0, 0, 0, 0.15);
}

.stat-icon {
    width: 60px;
    height: 60px;
    border-radius: 50%;
    background: linear-gradient(135deg, #e6f0ff 0%, #f0f7ff 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    transition: all 0.3s ease;
}

.stat-card:hover .stat-icon {
    transform: scale(1.1);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

.stat-icon .el-icon {
    font-size: 1.8rem;
}

/* 确保数字动画在视图中时触发 */
.stat-card {
    opacity: 0;
    transform: translateY(20px);
    animation: fadeInUp 0.6s ease-out forwards;
}

@keyframes fadeInUp {
    from {
        opacity: 0;
        transform: translateY(20px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

/* 添加按钮悬停效果 */
.el-button.text-blue-600:hover {
    transform: translateX(5px);
    transition: transform 0.3s ease;
}
</style> 