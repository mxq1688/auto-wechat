import { create } from 'zustand';
import { RTCPeerConnection, RTCSessionDescription, mediaDevices } from 'react-native-webrtc';
import type { Contact, CallState } from '../types/contact';

interface CallStoreState {
  callState: CallState;
  currentContact: Contact | null;
  localStream: any | null;
  remoteStream: any | null;
  peerConnection: RTCPeerConnection | null;
  startCall: (contact: Contact) => Promise<void>;
  answerCall: (contact: Contact, offer: any) => Promise<void>;
  endCall: () => Promise<void>;
  toggleMute: () => void;
  toggleCamera: () => void;
}

const STUN_SERVERS = {
  iceServers: [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' },
  ],
};

export const useCallStore = create<CallStoreState>((set, get) => ({
  callState: 'idle' as CallState,
  currentContact: null,
  localStream: null,
  remoteStream: null,
  peerConnection: null,

  startCall: async (contact: Contact) => {
    set({ currentContact: contact, callState: 'connecting' as CallState });

    try {
      // Get user media
      const stream = await mediaDevices.getUserMedia({
        audio: true,
        video: {
          facingMode: 'user',
          width: { ideal: 1280 },
          height: { ideal: 720 },
        },
      });

      set({ localStream: stream });

      // Create peer connection
      const pc = new RTCPeerConnection(STUN_SERVERS);

      // Add local stream tracks
      stream.getTracks().forEach((track: any) => {
        pc.addTrack(track, stream);
      });

      // Handle remote stream
      pc.ontrack = (event: any) => {
        if (event.streams && event.streams[0]) {
          set({ remoteStream: event.streams[0], callState: 'connected' as CallState });
        }
      };

      // Handle ICE candidates
      pc.onicecandidate = (event: any) => {
        if (event.candidate) {
          // Send ICE candidate through signaling service
          console.log('ICE candidate:', event.candidate);
        }
      };

      // Handle connection state
      pc.onconnectionstatechange = () => {
        console.log('Connection state:', pc.connectionState);
        if (pc.connectionState === 'disconnected' || pc.connectionState === 'failed') {
          get().endCall();
        }
      };

      // Create and set local description
      const offer = await pc.createOffer();
      await pc.setLocalDescription(offer);

      set({ peerConnection: pc, callState: 'ringing' as CallState });

      // Send offer through signaling service
      console.log('Offer created:', offer);
    } catch (error) {
      console.error('Error starting call:', error);
      await get().endCall();
    }
  },

  answerCall: async (contact: Contact, offer: any) => {
    set({ currentContact: contact, callState: 'connecting' as CallState });

    try {
      // Get user media
      const stream = await mediaDevices.getUserMedia({
        audio: true,
        video: {
          facingMode: 'user',
          width: { ideal: 1280 },
          height: { ideal: 720 },
        },
      });

      set({ localStream: stream });

      // Create peer connection
      const pc = new RTCPeerConnection(STUN_SERVERS);

      // Add local stream tracks
      stream.getTracks().forEach((track: any) => {
        pc.addTrack(track, stream);
      });

      // Handle remote stream
      pc.ontrack = (event: any) => {
        if (event.streams && event.streams[0]) {
          set({ remoteStream: event.streams[0], callState: 'connected' as CallState });
        }
      };

      // Handle ICE candidates
      pc.onicecandidate = (event: any) => {
        if (event.candidate) {
          console.log('ICE candidate:', event.candidate);
        }
      };

      // Set remote description
      await pc.setRemoteDescription(new RTCSessionDescription(offer));

      // Create and set local description
      const answer = await pc.createAnswer();
      await pc.setLocalDescription(answer);

      set({ peerConnection: pc, callState: 'connected' as CallState });

      // Send answer through signaling service
      console.log('Answer created:', answer);
    } catch (error) {
      console.error('Error answering call:', error);
      await get().endCall();
    }
  },

  endCall: async () => {
    const { localStream, peerConnection } = get();

    // Stop local stream
    if (localStream) {
      localStream.getTracks().forEach((track: any) => track.stop());
    }

    // Close peer connection
    if (peerConnection) {
      peerConnection.close();
    }

    set({
      callState: 'ended' as CallState,
      localStream: null,
      remoteStream: null,
      peerConnection: null,
    });

    // Reset to idle after 1 second
    setTimeout(() => {
      set({ callState: 'idle' as CallState, currentContact: null });
    }, 1000);
  },

  toggleMute: () => {
    const { localStream } = get();
    if (localStream) {
      const audioTrack = localStream.getAudioTracks()[0];
      if (audioTrack) {
        audioTrack.enabled = !audioTrack.enabled;
      }
    }
  },

  toggleCamera: () => {
    const { localStream } = get();
    if (localStream) {
      const videoTrack = localStream.getVideoTracks()[0];
      if (videoTrack) {
        videoTrack.enabled = !videoTrack.enabled;
      }
    }
  },
}));
