# 🚀 Service 启动流程

## 完整启动步骤

```bash
# 1. 安装 Bun（首次使用）
curl -fsSL https://bun.sh/install | bash
export BUN_INSTALL="$HOME/.bun"
export PATH="$BUN_INSTALL/bin:$PATH"

# 2. 安装依赖
bun install

# 3. 获取可用端口
# 使用 get_all_domains_ports 工具查找可用端口
# 例如返回：8081

# 4. 启动服务
bun expo start --tunnel --web --port 8081

# 5. 获取 tunnel URL（在新终端执行）
curl -s http://localhost:4040/api/tunnels | jq -r '.tunnels[].public_url'
# 输出示例：https://abc-xyz.ngrok-free.app

# 6. 生成 Expo URL
# 将 https:// 替换为 exp://
# https://abc-xyz.ngrok-free.app → exp://abc-xyz.ngrok-free.app
```

## 访问方式

- **手机端**：在 Expo Go 中输入 `exp://abc-xyz.ngrok-free.app`
- **浏览器**：访问 `https://abc-xyz.ngrok-free.app`

## 常见问题

### 端口被占用
```bash
# 重新使用 get_all_domains_ports 工具获取另一个可用端口
# 然后使用新端口启动
bun expo start --tunnel --web --port 8082
```
