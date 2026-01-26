/**
 * 通用类型定义
 * 根据具体业务需求添加类型
 */

// 示例：基础数据模型
export interface BaseModel {
  id: string;
  createdAt?: string;
  updatedAt?: string;
}

// 示例：API 响应格式
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
  error?: string;
}

