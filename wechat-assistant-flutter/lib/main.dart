import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'providers/auto_reply_provider.dart';
import 'providers/message_provider.dart';
import 'providers/settings_provider.dart';
import 'providers/call_provider.dart';
import 'screens/home_screen.dart';
import 'services/auto_reply_manager.dart';
import 'services/message_monitor.dart';
import 'services/floating_ball_service.dart';
import 'services/voice_command_service.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Initialize services
  await AutoReplyManager().initialize();
  await MessageMonitor().initialize();
  await FloatingBallService().initialize();
  await VoiceCommandService().initialize();
  
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => AutoReplyProvider()),
        ChangeNotifierProvider(create: (_) => MessageProvider()),
        ChangeNotifierProvider(create: (_) => SettingsProvider()),
        ChangeNotifierProvider(create: (_) => CallProvider()),
      ],
      child: Consumer<SettingsProvider>(
        builder: (context, settings, child) {
          return MaterialApp(
            title: 'WeChat Assistant',
            theme: ThemeData(
              primarySwatch: Colors.green,
              useMaterial3: true,
              brightness: settings.theme == 'dark' ? Brightness.dark : Brightness.light,
            ),
            home: const HomeScreen(),
            debugShowCheckedModeBanner: false,
          );
        },
      ),
    );
  }
}