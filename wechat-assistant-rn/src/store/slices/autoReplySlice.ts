import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import {AutoReplyRule} from '../../types';

interface AutoReplyState {
  rules: AutoReplyRule[];
  isEnabled: boolean;
  replyHistory: {
    contactId: string;
    timestamp: Date;
    ruleId: string;
  }[];
}

const initialState: AutoReplyState = {
  rules: [],
  isEnabled: false,
  replyHistory: [],
};

const autoReplySlice = createSlice({
  name: 'autoReply',
  initialState,
  reducers: {
    setEnabled: (state, action: PayloadAction<boolean>) => {
      state.isEnabled = action.payload;
    },
    addRule: (state, action: PayloadAction<AutoReplyRule>) => {
      state.rules.push(action.payload);
      state.rules.sort((a, b) => b.priority - a.priority);
    },
    updateRule: (state, action: PayloadAction<AutoReplyRule>) => {
      const index = state.rules.findIndex(r => r.id === action.payload.id);
      if (index !== -1) {
        state.rules[index] = action.payload;
        state.rules.sort((a, b) => b.priority - a.priority);
      }
    },
    deleteRule: (state, action: PayloadAction<string>) => {
      state.rules = state.rules.filter(r => r.id !== action.payload);
    },
    toggleRule: (state, action: PayloadAction<string>) => {
      const rule = state.rules.find(r => r.id === action.payload);
      if (rule) {
        rule.enabled = !rule.enabled;
      }
    },
    addReplyHistory: (state, action: PayloadAction<{contactId: string; ruleId: string}>) => {
      state.replyHistory.push({
        ...action.payload,
        timestamp: new Date(),
      });
      // Keep only last 1000 records
      if (state.replyHistory.length > 1000) {
        state.replyHistory = state.replyHistory.slice(-1000);
      }
    },
    clearReplyHistory: (state) => {
      state.replyHistory = [];
    },
  },
});

export const {
  setEnabled,
  addRule,
  updateRule,
  deleteRule,
  toggleRule,
  addReplyHistory,
  clearReplyHistory,
} = autoReplySlice.actions;

export default autoReplySlice.reducer;