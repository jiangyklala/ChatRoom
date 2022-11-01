package server.handle;

import libClink.core.Connector;
import libClink.utils.CloseUtils;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler {
    private final Connector connector;
    public final SocketChannel socketChannel;
    private final ClientWriteHandler writeHandler;  // 客户端写
    private final ClientHandlerCallback clientHandlerCallback;  // 客户端调用后的回调
    private final  String clientInfo;

    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallback clientHandlerCallback) throws IOException {
        this.socketChannel = socketChannel;

//        socketChannel.configureBlocking(false); // 设置非阻塞模式

//        Selector readSelector = Selector.open();
//        socketChannel.register(readSelector, SelectionKey.OP_READ);
//        this.readHandler = new ClientReadHandler(readSelector);

        connector = new Connector() {

            @Override
            public void onChannelClosed(SocketChannel channel) {
                super.onChannelClosed(channel);
                exitBySelf();
            }

            @Override
            protected void onReceiveNewMessage(String str) {
                super.onReceiveNewMessage(str);
                clientHandlerCallback.onNewMessageArrived(ClientHandler.this, str);
            }
        };
        connector.setup(socketChannel);

        Selector writeSelector = Selector.open();
        socketChannel.register(writeSelector, SelectionKey.OP_WRITE);
        this.writeHandler = new ClientWriteHandler(writeSelector);

        this.clientHandlerCallback = clientHandlerCallback;
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        System.out.println("新客户端连接: " + clientInfo);
    }

    public void send(String str) {
        writeHandler.send(str);
    }


    /**
     * 获取客户端信息
     */
    public String getClientInfo() {
        return clientInfo;
    }

    public void exitBySelf() {
        exit();
        clientHandlerCallback.onSelfClosed(this);
    }

    public void exit() {
        CloseUtils.close(connector);
        writeHandler.exit();
        CloseUtils.close(socketChannel);
        System.out.println("客户端已退出：" + clientInfo);
    }


    class ClientWriteHandler {
        private boolean done = false;
        private final Selector selector;
        private final ByteBuffer byteBuffer;
        private final ExecutorService executorService;

        ClientWriteHandler( Selector selector) {
            this.selector = selector;
            this.byteBuffer = ByteBuffer.allocate(256);
            this.executorService = Executors.newSingleThreadExecutor();  // 线程池
        }

        void send(String str) {
            // 如果需要发送时客户端下线了, 则直接返回
            if (done) {
                return;
            }
            executorService.execute(new WriteRunnable(str));
        }

        void exit() {
            done = true;
            CloseUtils.close(selector);
            executorService.shutdownNow();
        }

        private class WriteRunnable implements Runnable {
            private final String msg;

            WriteRunnable(String msg) {
                this.msg = msg + '\n';
            }

            @Override
            public void run() {
                if (ClientWriteHandler.this.done) {
                    return;
                }

                byteBuffer.clear();
                byteBuffer.put(msg.getBytes());
                byteBuffer.flip();  // 将指针回到起始位置, 把结束位置等于之前的指针位置

                while (!done && byteBuffer.hasRemaining()) {
                    try {
                        int len = socketChannel.write(byteBuffer);
                        if (len < 0) { // len == 0 合法
                            System.out.println("客户端已无法发送数据!");
                            ClientHandler.this.exitBySelf();
                            break;
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }

}
