import React, { useState } from 'react';
import { View, Text, ScrollView, Pressable, TextInput } from 'react-native';
import { useRouter } from 'expo-router';
import { Search, Video, User } from 'lucide-react-native';
import type { Contact } from '../../types/contact';

const MOCK_CONTACTS: Contact[] = [
  { id: '1', name: '张三', phoneNumber: '13800138000' },
  { id: '2', name: '李四', phoneNumber: '13800138001' },
  { id: '3', name: '王五', phoneNumber: '13800138002' },
  { id: '4', name: '赵六', phoneNumber: '13800138003' },
  { id: '5', name: '钱七', phoneNumber: '13800138004' },
  { id: '6', name: '孙八', phoneNumber: '13800138005' },
];

export default function ContactsScreen() {
  const router = useRouter();
  const [searchQuery, setSearchQuery] = useState('');

  const filteredContacts = MOCK_CONTACTS.filter(
    (contact) =>
      contact.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      contact.phoneNumber?.includes(searchQuery)
  );

  const getAvatarColor = (name: string) => {
    const colors = [
      '#ef4444', '#f59e0b', '#10b981', '#3b82f6',
      '#8b5cf6', '#ec4899', '#14b8a6', '#f97316',
    ];
    const index = name.charCodeAt(0) % colors.length;
    return colors[index];
  };

  return (
    <View className="flex-1 bg-gray-50">
      {/* Search Bar */}
      <View className="bg-white p-4">
        <View className="flex-row items-center bg-gray-100 rounded-xl px-4 py-3">
          <Search size={20} color="#9ca3af" />
          <TextInput
            className="flex-1 ml-2 text-base"
            placeholder="搜索联系人"
            value={searchQuery}
            onChangeText={setSearchQuery}
          />
        </View>
      </View>

      {/* Contacts List */}
      <ScrollView className="flex-1">
        {filteredContacts.length === 0 ? (
          <View className="items-center justify-center py-20">
            <User size={64} color="#d1d5db" />
            <Text className="text-gray-400 mt-4 text-lg">没有找到联系人</Text>
          </View>
        ) : (
          <View className="p-4">
            {filteredContacts.map((contact) => (
              <ContactListItem
                key={contact.id}
                contact={contact}
                avatarColor={getAvatarColor(contact.name)}
                onCall={() => {
                  router.push({
                    pathname: '/call/[id]',
                    params: { id: contact.id, name: contact.name },
                  });
                }}
              />
            ))}
          </View>
        )}
      </ScrollView>
    </View>
  );
}

function ContactListItem({
  contact,
  avatarColor,
  onCall,
}: {
  contact: Contact;
  avatarColor: string;
  onCall: () => void;
}) {
  return (
    <Pressable
      className="bg-white rounded-xl p-4 mb-3 shadow-sm flex-row items-center active:bg-gray-50"
      onPress={onCall}
    >
      {/* Avatar */}
      <View
        className="w-14 h-14 rounded-full items-center justify-center"
        style={{ backgroundColor: avatarColor }}
      >
        <Text className="text-white text-xl font-bold">
          {contact.name[0]}
        </Text>
      </View>

      {/* Contact Info */}
      <View className="flex-1 ml-4">
        <Text className="text-lg font-semibold">{contact.name}</Text>
        {contact.phoneNumber && (
          <Text className="text-gray-500 mt-1">{contact.phoneNumber}</Text>
        )}
      </View>

      {/* Call Button */}
      <Pressable
        className="w-12 h-12 bg-green-500 rounded-full items-center justify-center"
        onPress={(e) => {
          e.stopPropagation();
          onCall();
        }}
      >
        <Video size={24} color="white" />
      </Pressable>
    </Pressable>
  );
}
