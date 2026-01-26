import AsyncStorage from '@react-native-async-storage/async-storage';

/**
 * 通用本地存储工具
 */
export const storage = {
  /**
   * 保存数据
   */
  async set<T>(key: string, value: T): Promise<void> {
    try {
      const jsonValue = JSON.stringify(value);
      await AsyncStorage.setItem(key, jsonValue);
    } catch (error) {
      console.error(`Error saving data for key "${key}":`, error);
      throw error;
    }
  },

  /**
   * 读取数据
   */
  async get<T>(key: string): Promise<T | null> {
    try {
      const jsonValue = await AsyncStorage.getItem(key);
      return jsonValue != null ? JSON.parse(jsonValue) : null;
    } catch (error) {
      console.error(`Error reading data for key "${key}":`, error);
      return null;
    }
  },

  /**
   * 删除数据
   */
  async remove(key: string): Promise<void> {
    try {
      await AsyncStorage.removeItem(key);
    } catch (error) {
      console.error(`Error removing data for key "${key}":`, error);
      throw error;
    }
  },

  /**
   * 清空所有数据
   */
  async clear(): Promise<void> {
    try {
      await AsyncStorage.clear();
    } catch (error) {
      console.error('Error clearing storage:', error);
      throw error;
    }
  },
};

