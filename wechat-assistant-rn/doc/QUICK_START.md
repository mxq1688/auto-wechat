# 🎯 快速开始 - AI 系统集成手册

> **目标用户**：AI 代码生成系统  
> **阅读时间**：3 分钟  
> **核心目标**：在模板基础上生成完整可运行的 App

---

## 一、工作流程（3 步）

```
用户需求 → 读取模板 → 生成业务代码 → 输出完整项目
```

---

## 二、必读文件（按优先级）

| 文件 | 用途 | 必读指数 |
|------|------|---------|
| **AI_CODING_GUIDE.md** | 代码生成规范（核心） | ⭐⭐⭐⭐⭐ |
| TEMPLATE_STRUCTURE.md | 模板结构说明 | ⭐⭐⭐⭐ |
| USAGE_GUIDE.md | 使用示例 | ⭐⭐⭐ |
| README.md | 运行说明 | ⭐⭐ |

---

## 三、代码生成 Checklist

### ✅ 第一步：解析需求

从用户输入中提取：
- [ ] 核心功能（如：待办、记账、笔记）
- [ ] 数据模型（需要哪些字段）
- [ ] 交互方式（列表、卡片、表单）

### ✅ 第二步：生成类型

创建 `types/[业务名].ts`：
```tsx
export interface YourModel {
  id: string;
  // 根据需求添加字段
}
```

### ✅ 第三步：生成 Context

创建 `contexts/[业务名]Context.tsx`：
- 使用 `useState` 管理状态
- 使用 `storage.set/get` 持久化
- 使用 `useCallback` 包装方法
- 使用 `useMemo` 返回 value

### ✅ 第四步：实现页面

修改 `app/(tabs)/index.tsx`：
- 使用 `useYour()` 获取数据
- 渲染列表/卡片
- 添加交互（按钮、表单）

### ✅ 第五步：注入 Provider

修改 `app/_layout.tsx`：
```tsx
<YourProvider>
  <GestureHandlerRootView>...</>
</YourProvider>
```

---

## 四、代码模板（直接使用）

### Context 模板

```tsx
import { createContext, useContext, useState, useCallback, useMemo, useEffect } from 'react';
import { storage } from '@/utils/storage';
import type { YourType } from '@/types/your-type';

const YourContext = createContext<{
  items: YourType[];
  isLoading: boolean;
  addItem: (item: Omit<YourType, 'id'>) => Promise<void>;
  deleteItem: (id: string) => Promise<void>;
} | null>(null);

const STORAGE_KEY = '@your_key';

export function YourProvider({ children }: { children: React.ReactNode }) {
  const [items, setItems] = useState<YourType[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadItems();
  }, []);

  const loadItems = async () => {
    const stored = await storage.get<YourType[]>(STORAGE_KEY);
    if (stored) setItems(stored);
    setIsLoading(false);
  };

  const addItem = useCallback(async (item: Omit<YourType, 'id'>) => {
    const newItem = { ...item, id: Date.now().toString() };
    const updated = [...items, newItem];
    await storage.set(STORAGE_KEY, updated);
    setItems(updated);
  }, [items]);

  const deleteItem = useCallback(async (id: string) => {
    const updated = items.filter(i => i.id !== id);
    await storage.set(STORAGE_KEY, updated);
    setItems(updated);
  }, [items]);

  const value = useMemo(() => ({
    items,
    isLoading,
    addItem,
    deleteItem,
  }), [items, isLoading, addItem, deleteItem]);

  return <YourContext.Provider value={value}>{children}</YourContext.Provider>;
}

export const useYour = () => {
  const context = useContext(YourContext);
  if (!context) throw new Error('useYour must be used within YourProvider');
  return context;
};
```

### 页面模板

```tsx
import React, { useState } from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useYour } from '@/contexts/YourContext';

export default function HomeScreen() {
  const { items, isLoading } = useYour();
  const insets = useSafeAreaInsets();

  return (
    <View style={styles.container}>
      <View style={[styles.header, { paddingTop: insets.top + 20 }]}>
        <Text style={styles.title}>标题</Text>
      </View>

      <ScrollView style={styles.scrollView}>
        {isLoading ? (
          <Text>加载中...</Text>
        ) : items.length === 0 ? (
          <View style={styles.empty}>
            <Text style={styles.emptyText}>暂无数据</Text>
          </View>
        ) : (
          items.map(item => (
            <View key={item.id} style={styles.card}>
              <Text>{item.title}</Text>
            </View>
          ))
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F8F9FA' },
  header: { paddingHorizontal: 20, paddingBottom: 20, backgroundColor: '#FFF' },
  title: { fontSize: 34, fontWeight: '700', color: '#1A1A1A' },
  scrollView: { flex: 1 },
  empty: { alignItems: 'center', paddingVertical: 80 },
  emptyText: { fontSize: 16, color: '#8E8E93' },
  card: { 
    backgroundColor: '#FFF', 
    padding: 16, 
    marginHorizontal: 20, 
    marginBottom: 12, 
    borderRadius: 12 
  },
});
```

---

## 五、输出格式

### 完整项目结构

```
generated-app/
├── app/
│   ├── _layout.tsx         ← 修改：添加 Provider
│   ├── (tabs)/
│   │   ├── _layout.tsx     ← 保持不变
│   │   ├── index.tsx       ← 修改：实现业务页面
│   │   └── settings.tsx    ← 保持不变
│   └── +not-found.tsx      ← 保持不变
├── contexts/
│   └── [业务名]Context.tsx  ← 新增
├── types/
│   ├── index.ts            ← 保持不变
│   └── [业务名].ts          ← 新增
├── constants/              ← 保持不变
├── utils/                  ← 保持不变
├── package.json            ← 保持不变
├── tsconfig.json           ← 保持不变
└── app.json                ← 可选：修改应用名称
```

---

## 六、质量保证

生成代码必须满足：

- ✅ 可编译（无 TypeScript 错误）
- ✅ 可运行（`npm start` 正常启动）
- ✅ 有完整功能（CRUD 基本操作）
- ✅ 有错误处理（try-catch）
- ✅ 有加载状态（isLoading）
- ✅ 有空状态提示（无数据时）
- ✅ 样式符合规范（参考模板）

---

## 七、示例需求 → 代码映射

### 需求："待办事项 App"

**生成文件**：

1. `types/todo.ts`
```tsx
export interface Todo {
  id: string;
  title: string;
  completed: boolean;
  createdAt: string;
}
```

2. `contexts/TodoContext.tsx`
```tsx
// 使用上面的 Context 模板，替换类型为 Todo
```

3. `app/(tabs)/index.tsx`
```tsx
// 渲染待办列表，添加「标记完成」和「删除」功能
```

4. `app/_layout.tsx`
```diff
+ import { TodoProvider } from '@/contexts/TodoContext';

  return (
    <QueryClientProvider client={queryClient}>
+     <TodoProvider>
        <GestureHandlerRootView style={{ flex: 1 }}>
          <RootLayoutNav />
        </GestureHandlerRootView>
+     </TodoProvider>
    </QueryClientProvider>
  );
```

---

## 八、常见问题

### Q: 如何添加新的 Tab？

**A**: 编辑 `app/(tabs)/_layout.tsx`，添加：
```tsx
<Tabs.Screen
  name="new-tab"
  options={{
    title: "新标签",
    tabBarIcon: ({ color }) => <Icon color={color} size={24} />,
  }}
/>
```

### Q: 如何使用日期选择器？

**A**: 已安装 `@react-native-community/datetimepicker`，直接使用：
```tsx
import DateTimePicker from '@react-native-community/datetimepicker';
```

### Q: 如何添加图标？

**A**: 使用 `lucide-react-native`：
```tsx
import { Heart, Star, Plus } from 'lucide-react-native';
<Heart color="#FF0000" size={24} />
```

---

## 🎉 完成

按照以上流程，即可生成高质量的 React Native App 代码！

**核心原则**：  
📖 读懂 AI_CODING_GUIDE.md  
🎨 遵循 UI 设计规范  
🔧 使用提供的工具函数  
✅ 确保代码可运行

