# 模板结构说明

## 📐 架构设计

本模板采用 **分层架构设计**，将框架层与业务逻辑层分离：

```
┌─────────────────────────────────────┐
│        展示层 (Presentation)        │  ← 业务逻辑层（待生成）
│      app/(tabs)/index.tsx          │
├─────────────────────────────────────┤
│      业务逻辑层 (Business)          │  ← 业务逻辑层（待生成）
│  contexts/, hooks/, services/      │
├─────────────────────────────────────┤
│        框架层 (Framework)           │  ← 模板提供
│  app/_layout.tsx, Router, Query    │
├─────────────────────────────────────┤
│      基础设施层 (Infrastructure)    │  ← 模板提供
│  utils/, constants/, types/        │
└─────────────────────────────────────┘
```

## 📂 目录职责

### ✅ 框架层（模板已实现）

| 目录/文件 | 职责 | 可修改性 |
|----------|------|---------|
| `app/_layout.tsx` | 全局 Provider 配置、路由容器 | ⚠️ 谨慎修改 |
| `app/(tabs)/_layout.tsx` | Tab 导航配置 | ✅ 可扩展 |
| `app/+not-found.tsx` | 404 页面 | ⚠️ 建议保留 |
| `constants/` | 颜色、布局常量 | ✅ 可扩展 |
| `utils/storage.ts` | 本地存储工具 | ✅ 可扩展 |
| `types/index.ts` | 基础类型定义 | ✅ 可扩展 |

### 🔧 业务层（需要生成）

| 目录 | 用途 | 示例 |
|------|------|------|
| `contexts/` | 全局状态管理 | `UserContext.tsx`, `ThemeContext.tsx` |
| `types/` | 业务类型定义 | `user.ts`, `product.ts` |
| `hooks/` | 自定义 Hook | `useAuth.ts`, `useApi.ts` |
| `services/` | API 服务层 | `api.ts`, `auth.ts` |
| `components/` | 可复用组件 | `Button.tsx`, `Card.tsx` |

### 📄 页面层（需要生成）

| 文件 | 职责 |
|------|------|
| `app/(tabs)/index.tsx` | 主页内容（当前为空白模板） |
| `app/(tabs)/settings.tsx` | 设置页内容（当前为基础框架） |
| 新增页面 | 根据需求添加 |

## 🔑 关键设计模式

### 1. Provider 注入顺序

```tsx
// app/_layout.tsx
<QueryClientProvider>        // 1. 网络请求层
  <YourBusinessProvider>     // 2. 业务状态层（待添加）
    <GestureHandlerRootView> // 3. 手势交互层
      <Router />             // 4. 路由导航层
    </GestureHandlerRootView>
  </YourBusinessProvider>
</QueryClientProvider>
```

### 2. 数据流设计

```
用户交互 → 触发 Action → 更新 State → 持久化 Storage
   ↓                                        ↓
UI 组件 ← 订阅 Context ← State 变更 ←────────┘
```

### 3. 文件命名规范

- **页面组件**：`PascalCase` + `Screen` 后缀（如 `HomeScreen`）
- **业务组件**：`PascalCase`（如 `UserCard`）
- **工具函数**：`camelCase`（如 `formatDate`）
- **Context**：`PascalCase` + `Context` 后缀（如 `AuthContext`）
- **类型定义**：`PascalCase` + `interface/type`（如 `User`, `ApiResponse`）

## 🚀 扩展指南

### 添加业务状态管理

```tsx
// contexts/YourContext.tsx
import { createContext, useContext, useState } from 'react';

const YourContext = createContext(null);

export function YourProvider({ children }) {
  const [state, setState] = useState(initialState);
  return (
    <YourContext.Provider value={{ state, setState }}>
      {children}
    </YourContext.Provider>
  );
}

export const useYour = () => useContext(YourContext);
```

### 添加业务类型

```tsx
// types/your-model.ts
import { BaseModel } from './index';

export interface YourModel extends BaseModel {
  name: string;
  description?: string;
  // 添加字段...
}
```

### 添加新页面

```tsx
// app/new-page.tsx
export default function NewPageScreen() {
  return <View><Text>新页面</Text></View>;
}
```

## ⚠️ 注意事项

1. **不要修改** `app/_layout.tsx` 的基础结构
2. **保持** Expo Router 的文件命名约定
3. **遵循** TypeScript 严格模式
4. **使用** 提供的 `storage` 工具进行数据持久化
5. **参考** 现有的 `index.tsx` 和 `settings.tsx` 的组件结构

## 📊 模板完整度

- ✅ 路由框架：100%
- ✅ 基础配置：100%
- ✅ 工具函数：80%
- ⏳ UI 组件库：0%（按需添加）
- ⏳ 业务逻辑：0%（待生成）

