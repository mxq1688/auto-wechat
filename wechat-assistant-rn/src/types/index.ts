// Message types
export interface Message {
  id: string;
  content: string;
  sender: string;
  receiver: string;
  isGroup: boolean;
  groupName?: string;
  timestamp: Date;
  type: 'text' | 'image' | 'voice' | 'video' | 'file';
  isRead: boolean;
  isAutoReply?: boolean;
}

// Auto reply rule types
export interface AutoReplyRule {
  id: string;
  name: string;
  enabled: boolean;
  keyword: string;
  reply: string;
  matchType: 'exact' | 'contains' | 'regex' | 'startsWith' | 'endsWith';
  scope: 'all' | 'private' | 'group' | 'specific';
  specificContacts?: string[];
  priority: number;
  delay: number;
  maxReplyPerDay?: number;
  whitelist?: string[];
  blacklist?: string[];
  timeRange?: {
    start: string;
    end: string;
  };
  createdAt: Date;
  updatedAt: Date;
}

// Contact types
export interface Contact {
  id: string;
  name: string;
  avatar?: string;
  isGroup: boolean;
  memberCount?: number;
  lastMessage?: string;
  lastMessageTime?: Date;
  isPinned: boolean;
  isMuted: boolean;
}

// Settings types
export interface AppSettings {
  autoReplyEnabled: boolean;
  replyInGroups: boolean;
  replyInPrivate: boolean;
  defaultReplyDelay: number;
  messageMonitorEnabled: boolean;
  saveMessageHistory: boolean;
  maxHistoryDays: number;
  floatingBallEnabled: boolean;
  floatingBallOpacity: number;
  floatingBallSize: number;
  ttsEnabled: boolean;
  voiceCommandEnabled: boolean;
  ttsLanguage: string;
  autoAnswerVideo: boolean;
  autoAnswerDelay: number;
  signalingServerUrl: string;
  theme: 'light' | 'dark' | 'auto';
  language: 'zh' | 'en';
}

// WebRTC types
export interface CallState {
  isInCall: boolean;
  callType: 'voice' | 'video' | null;
  remoteUserId: string | null;
  isMuted: boolean;
  isSpeakerOn: boolean;
  isVideoEnabled: boolean;
  callStartTime: Date | null;
}

// Voice command types
export enum VoiceAction {
  ENABLE_AUTO_REPLY = 'enable_auto_reply',
  DISABLE_AUTO_REPLY = 'disable_auto_reply',
  SHOW_FLOATING_BALL = 'show_floating_ball',
  HIDE_FLOATING_BALL = 'hide_floating_ball',
  OPEN_MESSAGE_HISTORY = 'open_message_history',
  ADD_REPLY_RULE = 'add_reply_rule',
  ANSWER_VIDEO = 'answer_video',
  END_VIDEO = 'end_video',
  MUTE = 'mute',
  UNMUTE = 'unmute',
}

export interface VoiceCommand {
  action: VoiceAction;
  parameters?: any;
  timestamp: Date;
}