import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import {CallState} from '../../types';

const initialState: CallState = {
  isInCall: false,
  callType: null,
  remoteUserId: null,
  isMuted: false,
  isSpeakerOn: false,
  isVideoEnabled: false,
  callStartTime: null,
};

const callSlice = createSlice({
  name: 'call',
  initialState,
  reducers: {
    startCall: (state, action: PayloadAction<{type: 'voice' | 'video'; userId: string}>) => {
      state.isInCall = true;
      state.callType = action.payload.type;
      state.remoteUserId = action.payload.userId;
      state.callStartTime = new Date();
      state.isVideoEnabled = action.payload.type === 'video';
    },
    endCall: (state) => {
      return initialState;
    },
    toggleMute: (state) => {
      state.isMuted = !state.isMuted;
    },
    toggleSpeaker: (state) => {
      state.isSpeakerOn = !state.isSpeakerOn;
    },
    toggleVideo: (state) => {
      state.isVideoEnabled = !state.isVideoEnabled;
    },
    setCallState: (state, action: PayloadAction<Partial<CallState>>) => {
      return {...state, ...action.payload};
    },
  },
});

export const {
  startCall,
  endCall,
  toggleMute,
  toggleSpeaker,
  toggleVideo,
  setCallState,
} = callSlice.actions;

export default callSlice.reducer;