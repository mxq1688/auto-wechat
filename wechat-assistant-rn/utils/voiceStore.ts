import { create } from 'zustand';
import Voice from '@react-native-voice/voice';
import * as Speech from 'expo-speech';

interface VoiceState {
  isListening: boolean;
  lastWords: string;
  confidence: number;
  isInitialized: boolean;
  startListening: () => Promise<void>;
  stopListening: () => Promise<void>;
  speak: (text: string) => Promise<void>;
}

export const useVoiceStore = create<VoiceState>((set, get) => {
  // Initialize Voice
  Voice.onSpeechStart = () => {
    set({ isListening: true, lastWords: '' });
  };

  Voice.onSpeechEnd = () => {
    set({ isListening: false });
  };

  Voice.onSpeechResults = (event) => {
    const results = event.value || [];
    if (results.length > 0) {
      const text = results[0];
      set({ lastWords: text });
      processVoiceCommand(text);
    }
  };

  Voice.onSpeechError = (error) => {
    console.error('Speech error:', error);
    set({ isListening: false });
  };

  const processVoiceCommand = (command: string) => {
    const normalized = command.replace(/\s/g, '').toLowerCase();
    const { speak } = get();

    if (normalized.includes('接听') || normalized.includes('接电话')) {
      speak('正在接听');
      // Trigger answer call action
    } else if (normalized.includes('打电话') || normalized.includes('视频通话')) {
      const name = extractContactName(command);
      if (name) {
        speak(`正在呼叫${name}`);
        // Trigger make call action
      } else {
        speak('请说出联系人姓名');
      }
    } else if (normalized.includes('挂断') || normalized.includes('结束通话')) {
      speak('正在挂断');
      // Trigger end call action
    } else if (normalized.includes('打开微信')) {
      speak('正在打开微信');
      // Trigger open WeChat action
    } else {
      speak('未识别的命令');
    }
  };

  const extractContactName = (command: string): string | null => {
    const patterns = [
      /给(.+?)打/,
      /打电话给(.+)/,
      /视频通话(.+)/,
    ];

    for (const pattern of patterns) {
      const match = command.match(pattern);
      if (match && match[1]) {
        return match[1].trim();
      }
    }

    return null;
  };

  return {
    isListening: false,
    lastWords: '',
    confidence: 0,
    isInitialized: false,

    startListening: async () => {
      try {
        await Voice.start('zh-CN');
        set({ isInitialized: true });
      } catch (error) {
        console.error('Error starting voice:', error);
        const { speak } = get();
        await speak('语音识别启动失败');
      }
    },

    stopListening: async () => {
      try {
        await Voice.stop();
      } catch (error) {
        console.error('Error stopping voice:', error);
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
  };
});
