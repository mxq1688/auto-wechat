import AutoReplyManager from './AutoReplyManager';
import MessageMonitor from './MessageMonitor';
import FloatingBallService from './FloatingBallService';
import WebRTCService from './WebRTCService';
import VoiceCommandService from './VoiceCommandService';

export async function initializeServices(): Promise<void> {
  try {
    await AutoReplyManager.getInstance().initialize();
    await MessageMonitor.getInstance().initialize();
    await FloatingBallService.getInstance().initialize();
    await WebRTCService.getInstance().initialize();
    await VoiceCommandService.getInstance().initialize();
    console.log('All services initialized successfully');
  } catch (error) {
    console.error('Failed to initialize services:', error);
  }
}

export {
  AutoReplyManager,
  MessageMonitor,
  FloatingBallService,
  WebRTCService,
  VoiceCommandService,
};