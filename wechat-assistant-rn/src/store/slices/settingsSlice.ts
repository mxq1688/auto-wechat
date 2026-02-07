import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import {AppSettings} from '../../types';

const initialState: AppSettings = {
  autoReplyEnabled: false,
  replyInGroups: false,
  replyInPrivate: true,
  defaultReplyDelay: 2,
  messageMonitorEnabled: false,
  saveMessageHistory: true,
  maxHistoryDays: 30,
  floatingBallEnabled: false,
  floatingBallOpacity: 0.8,
  floatingBallSize: 56,
  ttsEnabled: false,
  voiceCommandEnabled: false,
  ttsLanguage: 'zh-CN',
  autoAnswerVideo: false,
  autoAnswerDelay: 3,
  signalingServerUrl: 'http://localhost:3000',
  theme: 'light',
  language: 'zh',
};

const settingsSlice = createSlice({
  name: 'settings',
  initialState,
  reducers: {
    updateSettings: (state, action: PayloadAction<Partial<AppSettings>>) => {
      return {...state, ...action.payload};
    },
    toggleAutoReply: (state) => {
      state.autoReplyEnabled = !state.autoReplyEnabled;
    },
    toggleMessageMonitor: (state) => {
      state.messageMonitorEnabled = !state.messageMonitorEnabled;
    },
    toggleFloatingBall: (state) => {
      state.floatingBallEnabled = !state.floatingBallEnabled;
    },
    toggleTTS: (state) => {
      state.ttsEnabled = !state.ttsEnabled;
    },
    toggleVoiceCommand: (state) => {
      state.voiceCommandEnabled = !state.voiceCommandEnabled;
    },
    setTheme: (state, action: PayloadAction<'light' | 'dark' | 'auto'>) => {
      state.theme = action.payload;
    },
    setLanguage: (state, action: PayloadAction<'zh' | 'en'>) => {
      state.language = action.payload;
    },
    resetSettings: () => initialState,
  },
});

export const {
  updateSettings,
  toggleAutoReply,
  toggleMessageMonitor,
  toggleFloatingBall,
  toggleTTS,
  toggleVoiceCommand,
  setTheme,
  setLanguage,
  resetSettings,
} = settingsSlice.actions;

export default settingsSlice.reducer;