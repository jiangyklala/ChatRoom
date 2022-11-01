package client;

import client.bean.ServerInfo;

import java.io.*;

public class Client {
    public static void main(String[] args) {
        // 使用UDP监听服务器信息
        ServerInfo info = UDPSearcher.searchServer(10000);
        System.out.println("Server:" + info);

        // 监听信息成功
        if (info != null) {

            TCPClient tcpClient = null;

            try {
                // 通过得到的服务器信息与其建立TCP连接
                tcpClient = TCPClient.startWith(info);

                if (tcpClient == null) return;

                write(tcpClient);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (tcpClient != null) {
                    tcpClient.exit();
                }
            }
        }
    }

    public static void write(TCPClient tcpClient) throws IOException {
        // 构建键盘输入流
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        do {
            // 键盘读取一行
            String str = input.readLine();
            // 发送到服务器
            tcpClient.send(str);

            if ("00bye00".equalsIgnoreCase(str)) {
                break;
            }
        } while (true);

    }
}
