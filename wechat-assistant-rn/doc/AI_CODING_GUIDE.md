# AI App 代码生成指南

## 🎯 目标

根据用户的功能需求描述，在现有 React Native + Expo 模板基础上生成完整的 App 代码。

---

## 📥 输入

1. **模板代码**：包含框架层和基础设施的完整项目结构
2. **用户需求**：自然语言描述的 App 功能（如："开发一个待办事项 App"）

---

## 📤 输出

生成完整可运行的 App 代码，包括：
- 业务状态管理（`contexts/`）
- 类型定义（`types/`）
- 页面实现（`app/(tabs)/`）
- 可选：组件库（`components/`）、服务层（`services/`）

---

## 📋 编码规范

### 1️⃣ 文件组织

```
必须创建的文件：
✅ contexts/[业务名]Context.tsx    - 状态管理
✅ types/[业务名].ts                - 业务类型
✅ app/(tabs)/index.tsx            - 主页实现（覆盖模板）
⚠️ 不要修改：app/_layout.tsx       - 保持框架结构
```

### 2️⃣ Context 模式（必须遵循）

```tsx
import { createContext, useContext, useState, useMemo, useCallback } from 'react';
import { storage } from '@/utils/storage';

// 1. 定义 Context
const YourContext = createContext<YourContextType | null>(null);

// 2. Provider 实现
export function YourProvider({ children }: { children: React.ReactNode }) {
  const [data, setData] = useState<YourType[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // 3. 初始化加载（useEffect）
  useEffect(() => {
    loadData();
  }, []);

  // 4. 数据操作（useCallback）
  const addItem = useCallback(async (item: YourType) => {
    const newItems = [...data, item];
    await storage.set('YOUR_KEY', newItems);
    setData(newItems);
  }, [data]);

  // 5. 使用 useMemo 优化
  return useMemo(() => ({
    data,
    isLoading,
    addItem,
    // ...其他方法
  }), [data, isLoading, addItem]);
}

// 6. Hook 导出
export const useYour = () => {
  const context = useContext(YourContext);
  if (!context) throw new Error('useYour must be used within YourProvider');
  return context;
};
```

### 3️⃣ 类型定义规范

```tsx
// types/[业务名].ts
import { BaseModel } from './index';

export interface YourModel extends BaseModel {
  id: string;          // 必须：唯一标识
  title: string;       // 必须：主要字段
  description?: string; // 可选：次要字段
  createdAt?: string;  // 可选：时间戳
}

export type YourCategory = 'type1' | 'type2' | 'type3';  // 枚举类型
```

### 4️⃣ 页面组件结构

```tsx
// app/(tabs)/index.tsx
import { useYour } from '@/contexts/YourContext';
import { StyleSheet, View, Text, ScrollView } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

export default function HomeScreen() {
  const { data, isLoading } = useYour();
  const insets = useSafeAreaInsets();

  return (
    <View style={styles.container}>
      {/* 固定头部 */}
      <View style={[styles.header, { paddingTop: insets.top + 20 }]}>
        <Text style={styles.title}>标题</Text>
      </View>

      {/* 滚动内容 */}
      <ScrollView style={styles.scrollView}>
        {isLoading ? (
          <Text>加载中...</Text>
        ) : data.length === 0 ? (
          <View style={styles.empty}>
            <Text>暂无数据</Text>
          </View>
        ) : (
          data.map(item => (
            <View key={item.id}>{/* 渲染内容 */}</View>
          ))
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F8F9FA' },
  header: { paddingHorizontal: 20, paddingBottom: 20, backgroundColor: '#FFFFFF' },
  title: { fontSize: 34, fontWeight: '700', color: '#1A1A1A' },
  // ... 更多样式
});
```

### 5️⃣ Provider 注入（必须在 _layout.tsx 中添加）

```tsx
// app/_layout.tsx
import { YourProvider } from '@/contexts/YourContext';

export default function RootLayout() {
  return (
    <QueryClientProvider client={queryClient}>
      <YourProvider>  {/* ← 在这里添加 */}
        <GestureHandlerRootView style={{ flex: 1 }}>
          <RootLayoutNav />
        </GestureHandlerRootView>
      </YourProvider>
    </QueryClientProvider>
  );
}
```

---

## 🎨 UI 设计原则

### 📸 图片资源

**⚠️ 必须使用真实图片，推荐来源：**
- **Unsplash** (https://unsplash.com) - 高质量免费照片
- **Pexels** (https://pexels.com) - 免费图库
- **Pixabay** (https://pixabay.com) - 免费图片

### 视觉设计规范

1. **使用 iOS 风格设计**：圆角 12-20px、阴影、卡片式布局
2. **颜色规范**：
   - 主色：`#007AFF`（或根据需求自定义）
   - 文字：`#1A1A1A`（主文字）、`#8E8E93`（次要文字）
   - 背景：`#F8F9FA`（页面）、`#FFFFFF`（卡片）
3. **间距规范**：参考 `constants/layout.ts`（8, 16, 24 的倍数）
4. **字体规范**：
   - 标题：34px / 700
   - 副标题：15px / 400
   - 正文：16px / 400
   - 小字：12-14px / 400

---

## 🔧 常用模式

### 模态弹窗

```tsx
import { Modal, View, TouchableOpacity } from 'react-native';

function MyModal({ visible, onClose }) {
  return (
    <Modal visible={visible} animationType="slide" transparent>
      <View style={styles.modalOverlay}>
        <View style={styles.modalContent}>
          {/* 内容 */}
        </View>
      </View>
    </Modal>
  );
}
```

### 列表渲染（大量数据用 FlatList）

```tsx
import { FlatList } from 'react-native';

<FlatList
  data={items}
  renderItem={({ item }) => <ItemCard item={item} />}
  keyExtractor={(item) => item.id}
/>
```

### 本地存储

```tsx
import { storage } from '@/utils/storage';

// 保存
await storage.set('MY_DATA', dataArray);

// 读取
const data = await storage.get<DataType[]>('MY_DATA');
```

---

## ✅ 生成检查清单

在生成代码后，确保：

- [ ] 所有 Context 已添加到 `app/_layout.tsx`
- [ ] 类型定义完整且导出正确
- [ ] 页面使用 `useSafeAreaInsets` 适配刘海屏
- [ ] 所有异步操作有错误处理
- [ ] 列表有空状态提示
- [ ] 使用 `useCallback` 优化函数
- [ ] 使用 `useMemo` 优化计算
- [ ] StyleSheet 在组件底部定义
- [ ] 导入路径使用 `@/` 别名

---

## 🚫 禁止操作

❌ 不要修改 `package.json`、`tsconfig.json`、`app.json`
❌ 不要删除 `utils/storage.ts`、`constants/colors.ts`
❌ 不要改变 `app/_layout.tsx` 的基础结构（只能添加 Provider）
❌ 不要使用外部 API（除非用户明确要求）

---

## 📌 示例：待办事项 App

**需求**："开发一个待办事项管理 App"

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
// 参考上面的 Context 模式实现
```

3. `app/(tabs)/index.tsx`
```tsx
// 待办列表页面，包含添加、完成、删除功能
```

4. `app/_layout.tsx`（添加 Provider）
```tsx
<TodoProvider>
  {/* 现有内容 */}
</TodoProvider>
```

---

## 🎓 总结

**核心三步骤**：
1. 定义类型（`types/`）
2. 实现状态管理（`contexts/`）
3. 构建页面（`app/(tabs)/`）

遵循以上规范，确保生成的代码：
- ✅ 可立即运行
- ✅ 结构清晰
- ✅ 易于维护
- ✅ 符合 React Native 最佳实践

