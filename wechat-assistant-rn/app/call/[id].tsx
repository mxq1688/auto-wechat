import React, { useEffect, useState } from 'react';
import { View, Text, Pressable, Alert } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { RTCView } from 'react-native-webrtc';
import { useCallStore } from '../../utils/callStore';
import { PEER_CONFIG, generatePeerId } from '../../utils/simplePeer';
import { Phone, Mic, MicOff, Video as VideoIcon, VideoOff, SwitchCamera } from 'lucide-react-native';
import type { Contact } from '../../types/contact';

export default function VideoCallScreen() {
  const router = useRouter();
  const params = useLocalSearchParams();
  const [isMuted, setIsMuted] = useState(false);
  const [isVideoOff, setIsVideoOff] = useState(false);

  const {
    callState,
    currentContact,
    localStream,
    remoteStream,
    startCall,
    endCall,
    toggleMute,
    toggleCamera,
  } = useCallStore();

  const contact: Contact = {
    id: params.id as string,
    name: params.name as string,
  };

  useEffect(() => {
    // Start call when component mounts
    startCall(contact);
    
    // 显示PeerJS信息
    Alert.alert(
      'WebRTC已配置',
      '使用免费的PeerJS Cloud服务\n' +
      '• 无需注册，立即可用\n' +
      '• STUN/TURN服务器已配置\n' +
      '• 支持P2P视频通话',
      [{ text: '知道了' }]
    );

    // Cleanup on unmount
    return () => {
      endCall();
    };
  }, []);

  const handleEndCall = async () => {
    await endCall();
    router.back();
  };

  const handleToggleMute = () => {
    setIsMuted(!isMuted);
    toggleMute();
  };

  const handleToggleVideo = () => {
    setIsVideoOff(!isVideoOff);
    toggleCamera();
  };

  const getCallStateText = () => {
    switch (callState) {
      case 'idle':
        return '准备中...';
      case 'connecting':
        return '正在连接...';
      case 'ringing':
        return '等待接听...';
      case 'connected':
        return '通话中';
      case 'ended':
        return '通话已结束';
      default:
        return '';
    }
  };

  const getAvatarColor = (name: string) => {
    const colors = [
      '#ef4444', '#f59e0b', '#10b981', '#3b82f6',
      '#8b5cf6', '#ec4899', '#14b8a6', '#f97316',
    ];
    const index = name.charCodeAt(0) % colors.length;
    return colors[index];
  };

  return (
    <View className="flex-1 bg-black">
      {/* Remote Video */}
      {remoteStream ? (
        <RTCView
          streamURL={remoteStream.toURL()}
          style={{ flex: 1 }}
          objectFit="cover"
        />
      ) : (
        <View className="flex-1 items-center justify-center">
          <View
            className="w-32 h-32 rounded-full items-center justify-center mb-6"
            style={{ backgroundColor: getAvatarColor(contact.name) }}
          >
            <Text className="text-white text-5xl font-bold">
              {contact.name[0]}
            </Text>
          </View>
          <Text className="text-white text-3xl font-semibold mb-4">
            {contact.name}
          </Text>
          <Text className="text-gray-400 text-lg">{getCallStateText()}</Text>
          {callState === 'connecting' && (
            <View className="mt-6">
              <View className="w-8 h-8 border-4 border-white border-t-transparent rounded-full animate-spin" />
            </View>
          )}
        </View>
      )}

      {/* Local Video (PIP) */}
      {localStream && !isVideoOff && (
        <View className="absolute top-4 right-4 w-32 h-40 rounded-xl overflow-hidden border-2 border-white">
          <RTCView
            streamURL={localStream.toURL()}
            style={{ flex: 1 }}
            objectFit="cover"
            mirror
          />
        </View>
      )}

      {/* Top Bar */}
      <View className="absolute top-0 left-0 right-0 p-4">
        <View className="items-center">
          <Text className="text-white text-xl font-semibold">
            {contact.name}
          </Text>
          {callState === 'connected' && (
            <Text className="text-gray-300 text-sm mt-1">通话中</Text>
          )}
        </View>
      </View>

      {/* Bottom Controls */}
      <View className="absolute bottom-0 left-0 right-0 p-6 pb-12">
        <View className="flex-row justify-around items-center">
          {/* Mute Button */}
          <ControlButton
            icon={
              isMuted ? (
                <MicOff size={32} color="white" />
              ) : (
                <Mic size={32} color="white" />
              )
            }
            label={isMuted ? '取消静音' : '静音'}
            onPress={handleToggleMute}
          />

          {/* End Call Button */}
          <ControlButton
            icon={<Phone size={32} color="white" />}
            label="挂断"
            backgroundColor="#ef4444"
            onPress={handleEndCall}
          />

          {/* Camera Switch Button */}
          <ControlButton
            icon={<SwitchCamera size={32} color="white" />}
            label="切换"
            onPress={handleToggleVideo}
          />
        </View>
      </View>
    </View>
  );
}

function ControlButton({
  icon,
  label,
  backgroundColor = 'rgba(255, 255, 255, 0.2)',
  onPress,
}: {
  icon: React.ReactNode;
  label: string;
  backgroundColor?: string;
  onPress: () => void;
}) {
  return (
    <View className="items-center">
      <Pressable
        className="w-16 h-16 rounded-full items-center justify-center"
        style={{ backgroundColor }}
        onPress={onPress}
      >
        {icon}
      </Pressable>
      <Text className="text-white text-xs mt-2">{label}</Text>
    </View>
  );
}
