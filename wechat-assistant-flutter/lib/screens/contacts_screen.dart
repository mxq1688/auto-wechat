import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/contact.dart';
import '../providers/call_provider.dart';
import 'video_call_screen.dart';

class ContactsScreen extends StatefulWidget {
  const ContactsScreen({super.key});

  @override
  State<ContactsScreen> createState() => _ContactsScreenState();
}

class _ContactsScreenState extends State<ContactsScreen> {
  final List<Contact> _contacts = [
    Contact(
      id: '1',
      name: '张三',
      avatarUrl: null,
      phoneNumber: '13800138000',
    ),
    Contact(
      id: '2',
      name: '李四',
      avatarUrl: null,
      phoneNumber: '13800138001',
    ),
    Contact(
      id: '3',
      name: '王五',
      avatarUrl: null,
      phoneNumber: '13800138002',
    ),
    Contact(
      id: '4',
      name: '赵六',
      avatarUrl: null,
      phoneNumber: '13800138003',
    ),
  ];

  String _searchQuery = '';

  List<Contact> get _filteredContacts {
    if (_searchQuery.isEmpty) {
      return _contacts;
    }
    return _contacts.where((contact) {
      return contact.name.toLowerCase().contains(_searchQuery.toLowerCase()) ||
          (contact.phoneNumber?.contains(_searchQuery) ?? false);
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // Search Bar
        Padding(
          padding: const EdgeInsets.all(16),
          child: TextField(
            decoration: InputDecoration(
              hintText: '搜索联系人',
              prefixIcon: const Icon(Icons.search),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
              ),
              filled: true,
            ),
            onChanged: (value) {
              setState(() {
                _searchQuery = value;
              });
            },
          ),
        ),

        // Contacts List
        Expanded(
          child: _filteredContacts.isEmpty
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(
                        Icons.person_off,
                        size: 64,
                        color: Colors.grey[400],
                      ),
                      const SizedBox(height: 16),
                      Text(
                        '没有找到联系人',
                        style: TextStyle(
                          fontSize: 16,
                          color: Colors.grey[600],
                        ),
                      ),
                    ],
                  ),
                )
              : ListView.builder(
                  itemCount: _filteredContacts.length,
                  itemBuilder: (context, index) {
                    final contact = _filteredContacts[index];
                    return _ContactListItem(
                      contact: contact,
                      onCall: () => _startCall(contact),
                    );
                  },
                ),
        ),
      ],
    );
  }

  void _startCall(Contact contact) async {
    final callProvider = context.read<CallProvider>();
    
    // Initialize renderers if not already done
    if (callProvider.localRenderer == null) {
      await callProvider.initializeRenderers();
    }

    if (!mounted) return;

    // Navigate to video call screen
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => VideoCallScreen(contact: contact),
      ),
    );
  }
}

class _ContactListItem extends StatelessWidget {
  final Contact contact;
  final VoidCallback onCall;

  const _ContactListItem({
    required this.contact,
    required this.onCall,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(
        horizontal: 16,
        vertical: 8,
      ),
      leading: CircleAvatar(
        radius: 28,
        backgroundColor: Colors.primaries[
            contact.name.hashCode % Colors.primaries.length
        ],
        child: Text(
          contact.name[0],
          style: const TextStyle(
            color: Colors.white,
            fontSize: 20,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
      title: Text(
        contact.name,
        style: const TextStyle(
          fontSize: 16,
          fontWeight: FontWeight.w500,
        ),
      ),
      subtitle: contact.phoneNumber != null
          ? Text(
              contact.phoneNumber!,
              style: TextStyle(color: Colors.grey[600]),
            )
          : null,
      trailing: IconButton(
        icon: const Icon(Icons.video_call),
        color: Colors.green,
        iconSize: 32,
        onPressed: onCall,
      ),
      onTap: onCall,
    );
  }
}
