import React from 'react';
import { View, Text, TouchableOpacity } from 'react-native';

export default function HomeScreen() {
  const [isEnabled, setIsEnabled] = React.useState(false);

  return (
    <View style={{ flex: 1, backgroundColor: '#f3f4f6', padding: 20 }}>
      <View style={{ backgroundColor: 'white', borderRadius: 12, padding: 20, alignItems: 'center' }}>
        <Text style={{ fontSize: 24, fontWeight: 'bold', color: '#111827' }}>
          微信视频助手
        </Text>
        <Text style={{ fontSize: 16, color: '#6b7280', marginTop: 10 }}>
          {isEnabled ? '服务运行中' : '服务已停止'}
        </Text>
        <TouchableOpacity
          style={{
            backgroundColor: isEnabled ? '#ef4444' : '#10b981',
            paddingHorizontal: 30,
            paddingVertical: 12,
            borderRadius: 20,
            marginTop: 20
          }}
          onPress={() => setIsEnabled(!isEnabled)}
        >
          <Text style={{ color: 'white', fontSize: 16, fontWeight: 'bold' }}>
            {isEnabled ? '停止服务' : '启动服务'}
          </Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}