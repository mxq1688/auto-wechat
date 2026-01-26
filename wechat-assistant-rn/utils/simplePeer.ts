// 简化版PeerJS配置 - 使用免费的PeerJS Cloud
// 无需注册，立即可用

export const PEER_CONFIG = {
  // PeerJS免费云服务配置
  host: '0.peerjs.com',
  port: 443,
  secure: true,
  path: '/',
  
  // ICE服务器配置
  config: {
    iceServers: [
      // Google免费STUN服务器
      { urls: 'stun:stun.l.google.com:19302' },
      { urls: 'stun:stun1.l.google.com:19302' },
      
      // PeerJS提供的免费TURN服务器
      {
        urls: 'turn:0.peerjs.com:3478',
        username: 'peerjs',
        credential: 'peerjsp'
      }
    ]
  },
  
  debug: 2 // 开启调试日志
};

// 生成随机的Peer ID
export function generatePeerId() {
  return 'wechat-' + Math.random().toString(36).substr(2, 9);
}

// 使用示例
// import Peer from 'peerjs';
// import { PEER_CONFIG, generatePeerId } from './simplePeer';
// 
// const peer = new Peer(generatePeerId(), PEER_CONFIG);
// 
// peer.on('open', (id) => {
//   console.log('连接成功！你的ID是：' + id);
//   console.log('分享这个ID给对方来建立连接');
// });
