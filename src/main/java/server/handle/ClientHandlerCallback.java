package server.handle;

public interface ClientHandlerCallback {
    // 自身关闭通知
    void onSelfClosed(ClientHandler handler);

    // 收到消息时通知
    void onNewMessageArrived(ClientHandler handler, String msg);
}