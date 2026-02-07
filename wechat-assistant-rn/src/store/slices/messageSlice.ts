import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import {Message} from '../../types';

interface MessageState {
  messages: Message[];
  unreadCount: number;
  isMonitoring: boolean;
  lastSyncTime: Date | null;
}

const initialState: MessageState = {
  messages: [],
  unreadCount: 0,
  isMonitoring: false,
  lastSyncTime: null,
};

const messageSlice = createSlice({
  name: 'messages',
  initialState,
  reducers: {
    addMessage: (state, action: PayloadAction<Message>) => {
      state.messages.unshift(action.payload);
      if (!action.payload.isRead) {
        state.unreadCount++;
      }
      // Keep only last 500 messages in memory
      if (state.messages.length > 500) {
        state.messages = state.messages.slice(0, 500);
      }
    },
    addMessages: (state, action: PayloadAction<Message[]>) => {
      state.messages = [...action.payload, ...state.messages];
      state.messages.sort((a, b) => 
        new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
      );
      // Recalculate unread count
      state.unreadCount = state.messages.filter(m => !m.isRead).length;
    },
    markAsRead: (state, action: PayloadAction<string>) => {
      const message = state.messages.find(m => m.id === action.payload);
      if (message && !message.isRead) {
        message.isRead = true;
        state.unreadCount = Math.max(0, state.unreadCount - 1);
      }
    },
    markAllAsRead: (state) => {
      state.messages.forEach(m => m.isRead = true);
      state.unreadCount = 0;
    },
    deleteMessage: (state, action: PayloadAction<string>) => {
      const message = state.messages.find(m => m.id === action.payload);
      if (message && !message.isRead) {
        state.unreadCount = Math.max(0, state.unreadCount - 1);
      }
      state.messages = state.messages.filter(m => m.id !== action.payload);
    },
    clearMessages: (state) => {
      state.messages = [];
      state.unreadCount = 0;
    },
    setMonitoring: (state, action: PayloadAction<boolean>) => {
      state.isMonitoring = action.payload;
    },
    setSyncTime: (state, action: PayloadAction<Date>) => {
      state.lastSyncTime = action.payload;
    },
  },
});

export const {
  addMessage,
  addMessages,
  markAsRead,
  markAllAsRead,
  deleteMessage,
  clearMessages,
  setMonitoring,
  setSyncTime,
} = messageSlice.actions;

export default messageSlice.reducer;