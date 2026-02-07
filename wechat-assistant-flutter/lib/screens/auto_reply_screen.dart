import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/auto_reply_rule.dart';
import '../providers/auto_reply_provider.dart';

class AutoReplyScreen extends StatefulWidget {
  const AutoReplyScreen({Key? key}) : super(key: key);

  @override
  State<AutoReplyScreen> createState() => _AutoReplyScreenState();
}

class _AutoReplyScreenState extends State<AutoReplyScreen> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('自动回复规则'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: _addNewRule,
          ),
        ],
      ),
      body: Consumer<AutoReplyProvider>(
        builder: (context, provider, child) {
          if (provider.rules.isEmpty) {
            return _buildEmptyState();
          }
          return _buildRulesList(provider.rules);
        },
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.rule,
            size: 80,
            color: Colors.grey[400],
          ),
          const SizedBox(height: 16),
          Text(
            '暂无回复规则',
            style: TextStyle(
              fontSize: 18,
              color: Colors.grey[600],
            ),
          ),
          const SizedBox(height: 8),
          ElevatedButton.icon(
            onPressed: _addNewRule,
            icon: const Icon(Icons.add),
            label: const Text('添加规则'),
          ),
        ],
      ),
    );
  }

  Widget _buildRulesList(List<AutoReplyRule> rules) {
    return ListView.builder(
      itemCount: rules.length,
      itemBuilder: (context, index) {
        final rule = rules[index];
        return Card(
          margin: const EdgeInsets.all(8),
          child: ListTile(
            leading: CircleAvatar(
              backgroundColor: rule.enabled ? Colors.green : Colors.grey,
              child: Text(
                '${rule.priority}',
                style: const TextStyle(color: Colors.white),
              ),
            ),
            title: Text(rule.name),
            subtitle: Text('关键词: ${rule.keyword}\n回复: ${rule.reply}'),
            isThreeLine: true,
            trailing: Switch(
              value: rule.enabled,
              onChanged: (value) {
                // Toggle rule
              },
            ),
            onTap: () => _editRule(rule),
          ),
        );
      },
    );
  }

  void _addNewRule() {
    // Navigate to rule editor
  }

  void _editRule(AutoReplyRule rule) {
    // Navigate to rule editor
  }
}