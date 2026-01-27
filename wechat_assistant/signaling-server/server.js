/**
 * 简易 WebSocket 信令服务器
 * 用于 WebRTC 视频通话的信令交换
 * 
 * 使用方法:
 *   npm install ws
 *   node server.js
 * 
 * 默认端口: 8080
 */

const WebSocket = require('ws');
const http = require('http');
const url = require('url');

const PORT = process.env.PORT || 8080;

// 存储在线用户
const users = new Map();

// 创建 HTTP 服务器
const server = http.createServer((req, res) => {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('WebRTC Signaling Server is running\n');
});

// 创建 WebSocket 服务器
const wss = new WebSocket.Server({ server });

wss.on('connection', (ws, req) => {
    const query = url.parse(req.url, true).query;
    const userId = query.userId;
    
    if (!userId) {
        ws.close(4001, 'userId is required');
        return;
    }
    
    console.log(`User connected: ${userId}`);
    
    // 存储用户连接
    users.set(userId, ws);
    ws.userId = userId;
    
    // 通知其他用户该用户上线
    broadcastUserStatus(userId, 'online');
    
    ws.on('message', (data) => {
        try {
            const message = JSON.parse(data);
            handleMessage(ws, message);
        } catch (e) {
            console.error('Error parsing message:', e);
        }
    });
    
    ws.on('close', () => {
        console.log(`User disconnected: ${userId}`);
        users.delete(userId);
        broadcastUserStatus(userId, 'offline');
    });
    
    ws.on('error', (error) => {
        console.error(`WebSocket error for ${userId}:`, error);
    });
});

function handleMessage(ws, message) {
    const { type, to, from } = message;
    
    console.log(`Message from ${from}: ${type} -> ${to || 'broadcast'}`);
    
    switch (type) {
        case 'register':
            // 用户注册，发送在线用户列表
            const onlineUsers = Array.from(users.keys()).filter(id => id !== from);
            ws.send(JSON.stringify({
                type: 'online_users',
                users: onlineUsers
            }));
            break;
            
        case 'call_offer':
        case 'call_answer':
        case 'call_reject':
        case 'call_end':
        case 'ice_candidate':
            // 转发给目标用户
            forwardMessage(to, message);
            break;
            
        case 'get_online_users':
            const users_list = Array.from(users.keys()).filter(id => id !== from);
            ws.send(JSON.stringify({
                type: 'online_users',
                users: users_list
            }));
            break;
            
        default:
            console.log('Unknown message type:', type);
    }
}

function forwardMessage(targetUserId, message) {
    const targetWs = users.get(targetUserId);
    
    if (targetWs && targetWs.readyState === WebSocket.OPEN) {
        targetWs.send(JSON.stringify(message));
        console.log(`Message forwarded to ${targetUserId}`);
    } else {
        // 通知发送者目标用户不在线
        const senderWs = users.get(message.from);
        if (senderWs && senderWs.readyState === WebSocket.OPEN) {
            senderWs.send(JSON.stringify({
                type: 'error',
                message: `User ${targetUserId} is not online`
            }));
        }
        console.log(`Target user ${targetUserId} not found or disconnected`);
    }
}

function broadcastUserStatus(userId, status) {
    const message = JSON.stringify({
        type: status === 'online' ? 'user_online' : 'user_offline',
        userId: userId
    });
    
    users.forEach((ws, id) => {
        if (id !== userId && ws.readyState === WebSocket.OPEN) {
            ws.send(message);
        }
    });
}

// 启动服务器
server.listen(PORT, '0.0.0.0', () => {
    console.log(`Signaling server running on port ${PORT}`);
    console.log(`WebSocket URL: ws://localhost:${PORT}?userId=YOUR_USER_ID`);
});

// 优雅关闭
process.on('SIGINT', () => {
    console.log('\nShutting down server...');
    wss.close(() => {
        server.close(() => {
            process.exit(0);
        });
    });
});

