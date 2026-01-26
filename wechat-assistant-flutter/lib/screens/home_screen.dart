import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/accessibility_provider.dart';
import '../providers/voice_provider.dart';
import 'contacts_screen.dart';
import 'settings_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _selectedIndex = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('微信助手'),
        elevation: 2,
      ),
      body: IndexedStack(
        index: _selectedIndex,
        children: const [
          _MainTab(),
          ContactsScreen(),
          SettingsScreen(),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedIndex,
        onDestinationSelected: (index) {
          setState(() {
            _selectedIndex = index;
          });
        },
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.home_outlined),
            selectedIcon: Icon(Icons.home),
            label: '首页',
          ),
          NavigationDestination(
            icon: Icon(Icons.contacts_outlined),
            selectedIcon: Icon(Icons.contacts),
            label: '联系人',
          ),
          NavigationDestination(
            icon: Icon(Icons.settings_outlined),
            selectedIcon: Icon(Icons.settings),
            label: '设置',
          ),
        ],
      ),
    );
  }
}

class _MainTab extends StatelessWidget {
  const _MainTab();

  @override
  Widget build(BuildContext context) {
    final accessibilityProvider = context.watch<AccessibilityProvider>();
    final voiceProvider = context.watch<VoiceProvider>();

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Status Card
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                children: [
                  Icon(
                    accessibilityProvider.isEnabled
                        ? Icons.check_circle
                        : Icons.cancel,
                    size: 64,
                    color: accessibilityProvider.isEnabled
                        ? Colors.green
                        : Colors.grey,
                  ),
                  const SizedBox(height: 16),
                  Text(
                    accessibilityProvider.isEnabled
                        ? '服务运行中'
                        : '服务已停止',
                    style: Theme.of(context).textTheme.headlineSmall,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    accessibilityProvider.isEnabled
                        ? '正在监听微信视频来电'
                        : '启动服务以开始使用',
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          color: Colors.grey[600],
                        ),
                  ),
                  const SizedBox(height: 20),
                  FilledButton.icon(
                    onPressed: () {
                      accessibilityProvider.toggleAccessibilityService();
                    },
                    icon: Icon(
                      accessibilityProvider.isEnabled
                          ? Icons.stop
                          : Icons.play_arrow,
                    ),
                    label: Text(
                      accessibilityProvider.isEnabled ? '停止服务' : '启动服务',
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Voice Control Card
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const Icon(Icons.mic, size: 28),
                      const SizedBox(width: 12),
                      Text(
                        '语音控制',
                        style: Theme.of(context).textTheme.titleLarge,
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  if (voiceProvider.isListening)
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Colors.blue.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Row(
                        children: [
                          const Icon(Icons.mic, color: Colors.blue),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              voiceProvider.lastWords.isEmpty
                                  ? '正在聆听...'
                                  : voiceProvider.lastWords,
                              style: const TextStyle(color: Colors.blue),
                            ),
                          ),
                        ],
                      ),
                    ),
                  if (!voiceProvider.isListening)
                    Text(
                      '支持的命令：\n• "接听电话"\n• "打电话给[姓名]"\n• "挂断电话"\n• "打开微信"',
                      style: TextStyle(color: Colors.grey[600]),
                    ),
                  const SizedBox(height: 16),
                  Center(
                    child: FloatingActionButton.large(
                      onPressed: () {
                        if (voiceProvider.isListening) {
                          voiceProvider.stopListening();
                        } else {
                          voiceProvider.startListening();
                        }
                      },
                      backgroundColor: voiceProvider.isListening
                          ? Colors.red
                          : Colors.blue,
                      child: Icon(
                        voiceProvider.isListening ? Icons.stop : Icons.mic,
                        size: 32,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Quick Actions
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '快捷功能',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                  const SizedBox(height: 16),
                  _QuickActionButton(
                    icon: Icons.wechat,
                    label: '打开微信',
                    onTap: () {
                      // TODO: Open WeChat
                    },
                  ),
                  const SizedBox(height: 12),
                  _QuickActionButton(
                    icon: Icons.video_call,
                    label: '发起视频通话',
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => const ContactsScreen(),
                        ),
                      );
                    },
                  ),
                  const SizedBox(height: 12),
                  _QuickActionButton(
                    icon: Icons.settings_accessibility,
                    label: '无障碍设置',
                    onTap: () {
                      // TODO: Open accessibility settings
                    },
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _QuickActionButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;

  const _QuickActionButton({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          border: Border.all(color: Colors.grey[300]!),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Row(
          children: [
            Icon(icon, size: 28),
            const SizedBox(width: 16),
            Expanded(
              child: Text(
                label,
                style: Theme.of(context).textTheme.titleMedium,
              ),
            ),
            const Icon(Icons.chevron_right),
          ],
        ),
      ),
    );
  }
}
