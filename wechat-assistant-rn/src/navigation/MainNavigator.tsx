import React from 'react';
import {createBottomTabNavigator} from '@react-navigation/bottom-tabs';
import {createStackNavigator} from '@react-navigation/stack';
import Icon from 'react-native-vector-icons/MaterialIcons';
import HomeScreen from '../screens/HomeScreen';
import AutoReplyScreen from '../screens/AutoReplyScreen';
import MessageHistoryScreen from '../screens/MessageHistoryScreen';
import SettingsScreen from '../screens/SettingsScreen';

const Tab = createBottomTabNavigator();
const Stack = createStackNavigator();

const MainNavigator: React.FC = () => {
  return (
    <Tab.Navigator
      screenOptions={({route}) => ({
        tabBarIcon: ({focused, color, size}) => {
          let iconName = 'home';
          if (route.name === 'Home') iconName = 'home';
          else if (route.name === 'AutoReply') iconName = 'message';
          else if (route.name === 'Messages') iconName = 'history';
          else if (route.name === 'Settings') iconName = 'settings';
          return <Icon name={iconName} size={size} color={color} />;
        },
        tabBarActiveTintColor: '#07C160',
        tabBarInactiveTintColor: 'gray',
      })}
    >
      <Tab.Screen name="Home" component={HomeScreen} options={{title: '首页'}} />
      <Tab.Screen name="AutoReply" component={AutoReplyScreen} options={{title: '自动回复'}} />
      <Tab.Screen name="Messages" component={MessageHistoryScreen} options={{title: '消息'}} />
      <Tab.Screen name="Settings" component={SettingsScreen} options={{title: '设置'}} />
    </Tab.Navigator>
  );
};

export default MainNavigator;