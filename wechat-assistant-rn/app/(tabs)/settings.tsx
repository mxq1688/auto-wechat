import React from 'react';
import { View, Text, ScrollView, Pressable, Alert } from 'react-native';
import { Info, HelpCircle, ChevronRight } from 'lucide-react-native';

export default function SettingsScreen() {
  const showVoiceCommands = () => {
    Alert.alert(
      '语音命令列表',
      '接听电话\n  - 自动接听当前来电\n\n' +
      '打电话给[姓名]\n  - 拨打指定联系人的视频电话\n\n' +
      '挂断电话\n  - 结束当前通话\n\n' +
      '打开微信\n  - 快速启动微信应用',
      [{ text: '关闭' }]
    );
  };

  const showAbout = () => {
    Alert.alert(
      '关于微信助手',
      '版本：1.0.0\n\n' +
      '功能：\n' +
      '• 自动接听微信视频来电\n' +
      '• 语音播报来电人信息\n' +
      '• 语音控制拨打电话\n' +
      '• 独立视频通话功能',
      [{ text: '关闭' }]
    );
  };

  const showHelp = () => {
    Alert.alert(
      '使用帮助',
      '1. 启用无障碍服务\n' +
      '进入系统设置 > 无障碍 > 已安装的服务 > 微信助手\n\n' +
      '2. 授予必要权限\n' +
      '• 悬浮窗权限\n' +
      '• 麦克风权限\n' +
      '• 摄像头权限\n\n' +
      '3. 使用语音控制\n' +
      '点击麦克风按钮，说出命令即可',
      [{ text: '我知道了' }]
    );
  };

  return (
    <ScrollView className="flex-1 bg-gray-50">
      <View className="p-4">
        {/* Voice Control Settings */}
        <View className="bg-white rounded-2xl mb-4 shadow-sm overflow-hidden">
          <View className="p-4 border-b border-gray-100">
            <Text className="text-xl font-bold">语音控制设置</Text>
          </View>
          
          <SettingItem
            icon={<Info size={24} color="#3b82f6" />}
            title="支持的语音命令"
            subtitle="查看所有可用的语音命令"
            onPress={showVoiceCommands}
          />
        </View>

        {/* About */}
        <View className="bg-white rounded-2xl mb-4 shadow-sm overflow-hidden">
          <SettingItem
            icon={<Info size={24} color="#3b82f6" />}
            title="关于应用"
            subtitle="版本 1.0.0"
            onPress={showAbout}
          />
          
          <SettingItem
            icon={<HelpCircle size={24} color="#3b82f6" />}
            title="使用帮助"
            onPress={showHelp}
          />
        </View>

        {/* App Info */}
        <View className="bg-blue-50 rounded-2xl p-4">
          <View className="flex-row items-start">
            <Info size={20} color="#3b82f6" />
            <View className="flex-1 ml-3">
              <Text className="text-blue-900 font-semibold mb-2">设置说明</Text>
              <Text className="text-blue-700 text-sm leading-5">
                1. 需要在系统设置中启用本应用的无障碍服务{('\n')}
                2. 需要授予悬浮窗权限{('\n')}
                3. 首次使用需要手动配置权限
              </Text>
            </View>
          </View>
        </View>
      </View>
    </ScrollView>
  );
}

function SettingItem({
  icon,
  title,
  subtitle,
  onPress,
}: {
  icon: React.ReactNode;
  title: string;
  subtitle?: string;
  onPress: () => void;
}) {
  return (
    <Pressable
      className="flex-row items-center p-4 border-b border-gray-100 active:bg-gray-50"
      onPress={onPress}
    >
      {icon}
      <View className="flex-1 ml-4">
        <Text className="font-semibold text-base">{title}</Text>
        {subtitle && (
          <Text className="text-gray-500 text-sm mt-1">{subtitle}</Text>
        )}
      </View>
      <ChevronRight size={20} color="#9ca3af" />
    </Pressable>
  );
}
