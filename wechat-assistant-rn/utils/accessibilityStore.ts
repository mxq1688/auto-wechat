import { create } from 'zustand';
import * as Speech from 'expo-speech';

interface AccessibilityState {
  isEnabled: boolean;
  autoAnswer: boolean;
  answerDelay: number; // seconds
  setEnabled: (enabled: boolean) => void;
  setAutoAnswer: (autoAnswer: boolean) => void;
  setAnswerDelay: (delay: number) => void;
  announceIncomingCall: (callerName: string) => Promise<void>;
  speak: (text: string) => Promise<void>;
}

export const useAccessibilityStore = create<AccessibilityState>((set, get) => ({
  isEnabled: false,
  autoAnswer: true,
  answerDelay: 3,

  setEnabled: (enabled: boolean) => {
    set({ isEnabled: enabled });
    const { speak } = get();
    speak(enabled ? '无障碍服务已启动' : '无障碍服务已关闭');
  },

  setAutoAnswer: (autoAnswer: boolean) => {
    set({ autoAnswer });
  },

  setAnswerDelay: (delay: number) => {
    set({ answerDelay: delay });
  },

  announceIncomingCall: async (callerName: string) => {
    const { speak, autoAnswer, answerDelay } = get();
    await speak(`收到来自 ${callerName} 的视频通话`);

    if (autoAnswer) {
      // Wait for delay
      await new Promise(resolve => setTimeout(resolve, answerDelay * 1000));
      await speak('自动接听');
      // Auto-answer logic would be triggered here
    }
  },

  speak: async (text: string) => {
    try {
      await Speech.speak(text, {
        language: 'zh-CN',
        pitch: 1.0,
        rate: 0.75,
      });
    } catch (error) {
      console.error('Speech error:', error);
    }
  },
}));
