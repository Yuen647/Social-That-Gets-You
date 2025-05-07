import { useState } from 'react'
import './App.css'

function App() {
  const [count, setCount] = useState(0)

  return (
    <>
      <div className="container">
        <h1>欢迎来到知你社交</h1>
        <p className="subtitle">发现志同道合的朋友，分享精彩生活</p>
        
        <div className="features">
          <div className="feature-card">
            <h3>智能匹配</h3>
            <p>基于兴趣和性格的智能社交匹配</p>
          </div>
          <div className="feature-card">
            <h3>兴趣社区</h3>
            <p>加入你感兴趣的社区，结识新朋友</p>
          </div>
          <div className="feature-card">
            <h3>实时互动</h3>
            <p>随时随地与好友保持联系</p>
          </div>
        </div>

        <div className="cta-section">
          <button className="primary-button">立即加入</button>
          <button className="secondary-button">了解更多</button>
        </div>
      </div>
    </>
  )
}

export default App 