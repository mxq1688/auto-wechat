# 📑 文档索引

> **快速导航**：根据您的角色选择对应的文档

---

## 🤖 AI 代码生成系统

**必读文档**（按顺序）：

1. **[QUICK_START.md](QUICK_START.md)** ⭐⭐⭐⭐⭐  
   ⏱️ 3 分钟 | 📌 工作流程、代码模板、Checklist

2. **[AI_CODING_GUIDE.md](AI_CODING_GUIDE.md)** ⭐⭐⭐⭐⭐  
   ⏱️ 10 分钟 | 📌 完整编码规范、模式、约束

**可选文档**：

3. [TEMPLATE_STRUCTURE.md](TEMPLATE_STRUCTURE.md) ⭐⭐⭐⭐  
   📌 架构设计、目录职责、扩展指南

4. [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) ⭐⭐⭐  
   📌 项目总览、技术栈、质量清单

---

## 👨‍💻 开发者

**快速上手**：

1. [README.md](README.md) ⭐⭐⭐⭐  
   📌 安装依赖、运行项目、快速开始

2. [USAGE_GUIDE.md](USAGE_GUIDE.md) ⭐⭐⭐⭐  
   📌 使用场景、故障排查、最佳实践

**深入了解**：

3. [TEMPLATE_STRUCTURE.md](TEMPLATE_STRUCTURE.md) ⭐⭐⭐⭐  
   📌 架构设计、扩展指南

4. [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) ⭐⭐⭐  
   📌 技术栈、性能指标、已知限制

---

## 📁 项目文件结构

```
codebanana_app_templates/
│
├── 📚 文档系统 (6 个文档)
│   ├── INDEX.md                    ← 当前文件（文档索引）
│   ├── QUICK_START.md              ← AI 系统快速开始
│   ├── AI_CODING_GUIDE.md          ← AI 编程规范（核心）
│   ├── TEMPLATE_STRUCTURE.md       ← 模板架构说明
│   ├── USAGE_GUIDE.md              ← 使用指南
│   ├── PROJECT_SUMMARY.md          ← 项目总结
│   └── README.md                   ← 项目运行指南
│
├── ⚙️ 配置文件 (5 个)
│   ├── package.json                ← 依赖管理
│   ├── tsconfig.json               ← TypeScript 配置
│   ├── app.json                    ← Expo 应用配置
│   ├── eslint.config.js            ← 代码规范
│   └── expo-env.d.ts               ← 类型声明
│
├── 📱 应用代码
│   ├── app/                        ← Expo Router 路由
│   │   ├── _layout.tsx             ← 根布局（Provider 容器）
│   │   ├── (tabs)/                 ← Tab 导航组
│   │   │   ├── _layout.tsx         ← Tab 配置
│   │   │   ├── index.tsx           ← 首页模板
│   │   │   └── settings.tsx        ← 设置页模板
│   │   └── +not-found.tsx          ← 404 页面
│   │
│   ├── constants/                  ← 常量配置
│   │   ├── colors.ts               ← 颜色主题
│   │   └── layout.ts               ← 布局常量
│   │
│   ├── utils/                      ← 工具函数
│   │   └── storage.ts              ← 本地存储工具
│   │
│   ├── types/                      ← 类型定义
│   │   └── index.ts                ← 基础类型
│   │
│   └── assets/                     ← 静态资源
│       └── .gitkeep
│
└── 📦 待生成内容（AI 系统负责）
    ├── contexts/                   ← 业务状态管理
    ├── components/                 ← 可复用组件
    ├── services/                   ← API 服务层
    └── hooks/                      ← 自定义 Hook
```

---

## 🎯 快速查找

### 如何开始使用模板？
→ 阅读 [QUICK_START.md](QUICK_START.md)

### 如何编写符合规范的代码？
→ 阅读 [AI_CODING_GUIDE.md](AI_CODING_GUIDE.md)

### 如何理解模板架构？
→ 阅读 [TEMPLATE_STRUCTURE.md](TEMPLATE_STRUCTURE.md)

### 如何运行项目？
→ 阅读 [README.md](README.md)

### 如何解决问题？
→ 阅读 [USAGE_GUIDE.md](USAGE_GUIDE.md) 的故障排查章节

### 如何了解技术栈？
→ 阅读 [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)

---

## 📊 文档对比

| 文档 | 目标读者 | 篇幅 | 重要性 | 核心内容 |
|------|---------|------|--------|---------|
| **QUICK_START.md** | AI 系统 | 中 | ⭐⭐⭐⭐⭐ | 3 步流程 + 代码模板 |
| **AI_CODING_GUIDE.md** | AI 系统 | 长 | ⭐⭐⭐⭐⭐ | 编码规范 + 模式 + 约束 |
| TEMPLATE_STRUCTURE.md | 开发者/AI | 中 | ⭐⭐⭐⭐ | 架构设计 + 扩展指南 |
| USAGE_GUIDE.md | 开发者/AI | 长 | ⭐⭐⭐ | 场景示例 + 故障排查 |
| PROJECT_SUMMARY.md | 所有人 | 长 | ⭐⭐⭐ | 项目总览 + 技术栈 |
| README.md | 开发者 | 短 | ⭐⭐ | 快速运行 + 基本说明 |

---

## 🚀 推荐阅读路径

### 路径 1：AI 系统集成（5 分钟）
```
QUICK_START.md → AI_CODING_GUIDE.md → 开始生成代码
```

### 路径 2：开发者快速上手（10 分钟）
```
README.md → USAGE_GUIDE.md → TEMPLATE_STRUCTURE.md
```

### 路径 3：全面了解（30 分钟）
```
PROJECT_SUMMARY.md → TEMPLATE_STRUCTURE.md → 
AI_CODING_GUIDE.md → USAGE_GUIDE.md
```

---

## 📞 获取帮助

1. **基础问题**：查看 [README.md](README.md)
2. **使用问题**：查看 [USAGE_GUIDE.md](USAGE_GUIDE.md)
3. **架构问题**：查看 [TEMPLATE_STRUCTURE.md](TEMPLATE_STRUCTURE.md)
4. **规范问题**：查看 [AI_CODING_GUIDE.md](AI_CODING_GUIDE.md)

---

**最后更新**：2024年  
**模板版本**：1.0.0

