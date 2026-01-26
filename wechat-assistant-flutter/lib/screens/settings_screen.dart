import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/accessibility_provider.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final accessibilityProvider = context.watch<AccessibilityProvider>();

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        // Accessibility Settings
        Card(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.all(16),
                child: Text(
                  '无障碍服务设置',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
              ),
              SwitchListTile(
                title: const Text('自动接听'),
                subtitle: const Text('检测到微信来电时自动接听'),
                value: accessibilityProvider.autoAnswer,
                onChanged: (value) {
                  accessibilityProvider.setAutoAnswer(value);
                },
              ),
              ListTile(
                title: const Text('接听延迟'),
                subtitle: Text('${accessibilityProvider.answerDelay} 秒后接听'),
                trailing: SizedBox(
                  width: 200,
                  child: Slider(
                    value: accessibilityProvider.answerDelay.toDouble(),
                    min: 0,
                    max: 10,
                    divisions: 10,
                    label: '${accessibilityProvider.answerDelay}秒',
                    onChanged: (value) {
                      accessibilityProvider.setAnswerDelay(value.toInt());
                    },
                  ),
                ),
              ),
              const Divider(),
              Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Icon(Icons.info_outline, color: Colors.blue[700]),
                        const SizedBox(width: 8),
                        Text(
                          '设置说明',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w500,
                            color: Colors.blue[700],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    Text(
                      '1. 需要在系统设置中启用本应用的无障碍服务\n'
                      '2. 需要授予悬浮窗权限\n'
                      '3. 首次使用需要手动配置权限',
                      style: TextStyle(
                        fontSize: 14,
                        color: Colors.grey[700],
                        height: 1.5,
                      ),
                    ),
                    const SizedBox(height: 12),
                    OutlinedButton.icon(
                      onPressed: () {
                        // TODO: Open system accessibility settings
                      },
                      icon: const Icon(Icons.settings),
                      label: const Text('打开系统设置'),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),

        // Voice Control Settings
        Card(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.all(16),
                child: Text(
                  '语音控制设置',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
              ),
              ListTile(
                leading: const Icon(Icons.mic),
                title: const Text('支持的语音命令'),
                subtitle: const Text('查看所有可用的语音命令'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () {
                  _showVoiceCommandsDialog(context);
                },
              ),
              ListTile(
                leading: const Icon(Icons.language),
                title: const Text('语音识别语言'),
                subtitle: const Text('中文（简体）'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () {
                  // TODO: Show language selection
                },
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),

        // Call Settings
        Card(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.all(16),
                child: Text(
                  '通话设置',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
              ),
              ListTile(
                leading: const Icon(Icons.videocam),
                title: const Text('默认摄像头'),
                subtitle: const Text('前置摄像头'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () {
                  // TODO: Show camera selection
                },
              ),
              ListTile(
                leading: const Icon(Icons.volume_up),
                title: const Text('通话音量'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () {
                  // TODO: Show volume settings
                },
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),

        // About
        Card(
          child: Column(
            children: [
              ListTile(
                leading: const Icon(Icons.info),
                title: const Text('关于应用'),
                subtitle: const Text('版本 1.0.0'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () {
                  _showAboutDialog(context);
                },
              ),
              ListTile(
                leading: const Icon(Icons.help),
                title: const Text('使用帮助'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () {
                  _showHelpDialog(context);
                },
              ),
            ],
          ),
        ),
      ],
    );
  }

  void _showVoiceCommandsDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('语音命令列表'),
        content: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              _buildCommandItem('接听电话', '自动接听当前来电'),
              _buildCommandItem('打电话给[姓名]', '拨打指定联系人的视频电话'),
              _buildCommandItem('挂断电话', '结束当前通话'),
              _buildCommandItem('打开微信', '快速启动微信应用'),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('关闭'),
          ),
        ],
      ),
    );
  }

  Widget _buildCommandItem(String command, String description) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            command,
            style: const TextStyle(
              fontWeight: FontWeight.bold,
              fontSize: 16,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            description,
            style: TextStyle(
              color: Colors.grey[600],
              fontSize: 14,
            ),
          ),
        ],
      ),
    );
  }

  void _showAboutDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('关于微信助手'),
        content: const Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('版本：1.0.0'),
            SizedBox(height: 8),
            Text('功能：'),
            Text('• 自动接听微信视频来电'),
            Text('• 语音播报来电人信息'),
            Text('• 语音控制拨打电话'),
            Text('• 独立视频通话功能'),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('关闭'),
          ),
        ],
      ),
    );
  }

  void _showHelpDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('使用帮助'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '1. 启用无障碍服务',
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: 8),
              const Text('进入系统设置 > 无障碍 > 已安装的服务 > 微信助手'),
              const SizedBox(height: 16),
              Text(
                '2. 授予必要权限',
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: 8),
              const Text('• 悬浮窗权限\n• 麦克风权限\n• 摄像头权限'),
              const SizedBox(height: 16),
              Text(
                '3. 使用语音控制',
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: 8),
              const Text('点击麦克风按钮，说出命令即可'),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('我知道了'),
          ),
        ],
      ),
    );
  }
}
