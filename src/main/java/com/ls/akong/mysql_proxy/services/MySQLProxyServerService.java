package com.ls.akong.mysql_proxy.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.ls.akong.mysql_proxy.model.SqlLogModel;
import com.ls.akong.mysql_proxy.util.MySQLMessage;
import com.ls.akong.mysql_proxy.util.SqlBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service(Service.Level.PROJECT)
public final class MySQLProxyServerService implements Disposable {

    private static final Logger logger = Logger.getInstance(MySQLProxyServerService.class);

    private final Project project;
    private final List<Socket> clientSockets;
    private final List<MysqlProxyServiceStateListener> listeners = new ArrayList<>();
    private boolean isServiceRunning = false;
    private ServerSocket serverSocket;


    public MySQLProxyServerService(Project project) {
        this.project = project;

        clientSockets = new ArrayList<>();
    }

    /**
     * 服务器是否运行中
     */
    public boolean isServiceRunning() {
        return isServiceRunning;
    }

    /**
     * 启动服务
     */
    public void startService() {
        if (!isServiceRunning) {
            MysqlProxySettings.State state = MysqlProxySettings.getInstance(project).getState();
            assert state != null;

            // Mysql Server Ip 校验
            if (Objects.equals(state.originalMysqlIp, "")) {
                Messages.showErrorDialog("Run parameter exception. Please go to the Configuration Management page, fill in the 'Remote MySQL Server IP Address' field, and try again.", "Invalid Run Configuration");
                return;
            }
            if (Objects.equals(state.originalMysqlPort, "")) {
                Messages.showErrorDialog("Run parameter exception. Please go to the Configuration Management page, fill in the 'Remote MySQL Server Port' field, and try again.", "Invalid Run Configuration");
                return;
            }

            // 监听的端口校验
            int port = Integer.parseInt(state.listeningPort);
            if (port == 0) {
                Messages.showErrorDialog("Run parameter exception. Please go to the Configuration Management page, fill in the 'Listening port' field, and try again.", "Invalid Run Configuration");
                return;
            }

            //  Java 7 的 try-with-resources 语法，它确保在代码块结束时自动关闭资源，无需手动关闭ServerSocket。这样可以避免资源泄漏。
            try (ServerSocket ignored = new ServerSocket(port)) {
                // If binding succeeds, the port is available
            } catch (BindException bindException) {     // 端口占用
                String errorMessage = "Run Failed, Error Message:\n" + bindException.getMessage() + "\n\n" + "Possible Reasons for the Failure:\n" + "1. Port " + port + " already in use. Please modify to a different port and retry.\n" + "2. Insufficient port binding permissions. If you're using a Linux-based system, consider using a port between 1024 and 65535.";
                String errorTitle = "Proxy Listener Port Conflict Error";
                logger.error(bindException.getMessage(), bindException);
                Messages.showErrorDialog(errorMessage, errorTitle);
                return;
            } catch (IOException ioException) {
                String errorMessage = "An error occurred while trying to bind to port " + port + ". Please check your network settings and try again." + ioException.getMessage();
                String errorTitle = "Connection Error";
                logger.error(ioException.getMessage(), ioException);
                Messages.showErrorDialog(errorMessage, errorTitle);
                return;
            }

            // 启动服务的逻辑
            new Thread(() -> runService(port)).start();
            isServiceRunning = true;
        }
    }

    /**
     * 关闭服务
     */
    public void stopService() {
        logger.info("调用 关闭 Mysql Proxy service 的 stopService");
        if (isServiceRunning) {
            // 关闭服务的逻辑
            isServiceRunning = false;

            try {
                // 关闭已建立的连接
                for (Socket clientSocket : clientSockets) {
                    if (clientSocket != null && !clientSocket.isClosed()) {
                        clientSocket.close();
                    }
                }
                clientSockets.clear();

                // 关闭服务监听的 Socket
                if (serverSocket != null && !serverSocket.isClosed()) {
                    logger.info("关闭 Mysql Proxy service");
                    serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        notifyListeners();
    }

    private void runService(int proxyPort) {
        try {
            // 监听在本地的端口（代理服务器的端口）
            serverSocket = new ServerSocket(proxyPort);
            logger.info("MySQL Proxy Server is listening on port " + proxyPort);

            notifyListeners();

            while (true) {
                // 接受客户端的连接请求
                Socket clientSocket = serverSocket.accept();
                logger.info("Accepted connection from client: " + clientSocket.getInetAddress());

                // 记录客户端的连接请求，停止服务的时候，断开它们
                clientSockets.add(clientSocket);

                // 启动新线程处理客户端请求
                Thread thread = new Thread(new ClientHandler(clientSocket, project));
                thread.start();
            }
        } catch (SocketException e) {
            logger.info("MySQL Proxy Server is stopped:" + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 监听代理服务的状态
     */
    public void addListener(MysqlProxyServiceStateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    private void notifyListeners() {
        for (MysqlProxyServiceStateListener listener : listeners) {
            listener.onServiceStateChanged(isServiceRunning);
        }
    }

    /**
     * shutdown hook 关闭服务
     */
    @Override
    public void dispose() {
        stopService();
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        private final Project project;

        public ClientHandler(Socket clientSocket, Project project) {
            this.clientSocket = clientSocket;
            this.project = project;
        }

        @Override
        public void run() {
            try {
                // 连接到 MySQL 服务器
                MysqlProxySettings.State state = MysqlProxySettings.getInstance(project).getState();
                assert state != null;
                String mysqlHost = state.originalMysqlIp; // MySQL 服务器地址
                int mysqlPort = Integer.parseInt(state.originalMysqlPort); // MySQL 服务器端口
                logger.info("client MySQL service " + mysqlHost + ":" + mysqlPort);
                Socket mysqlSocket = new Socket(mysqlHost, mysqlPort);

                // 创建线程用于转发 MySQL 服务器的响应给客户端
                Thread responseThread = new Thread(new ResponseHandler(clientSocket, mysqlSocket));
                responseThread.start();

                // 转发客户端请求给 MySQL 服务器
                InputStream clientIn = clientSocket.getInputStream();
                OutputStream mysqlOut = mysqlSocket.getOutputStream();

                byte[] buffer = new byte[4096];
                int bytesRead;
                SqlBuilder sqlBuilder = new SqlBuilder();

                while ((bytesRead = clientIn.read(buffer)) != -1) {
                    MysqlProxySettings recordingSwitch = MysqlProxySettings.getInstance(project);
                    if (recordingSwitch.isMonitorEnabled()) {
                        byte[] rawBuffer = Arrays.copyOfRange(buffer, 0, bytesRead);
                        MySQLMessage mm = new MySQLMessage(rawBuffer, sqlBuilder);

                        // 获取包头中的信息
                        int sequenceNumber = mm.getSequenceNumber();  // 序号
                        // 只有第一个包的长度是准确的，如果分包会不准确，因此这样处理
                        if (sequenceNumber == 0) {
                            sqlBuilder.setTargetLength(mm.getPackageLength() - 4);  // 数据包的长度
                            sqlBuilder.setCommandByte(mm.getCommandByte());     // 数据包中的命令字节
                        }

                        String sql = mm.getSql();
                        if (!sql.equals("")) {  /* !sqlQuery.equals("") 是客户端会发送 0 长度的包保存连接； */
                            SqlLogModel.insertLog(project, sql);
                        }
                    }

                    // 准发给 mysql service
                    mysqlOut.write(buffer, 0, bytesRead);
                    mysqlOut.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class ResponseHandler implements Runnable {
        private final Socket clientSocket;
        private final Socket mysqlSocket;

        public ResponseHandler(Socket clientSocket, Socket mysqlSocket) {
            this.clientSocket = clientSocket;
            this.mysqlSocket = mysqlSocket;
        }

        @Override
        public void run() {
            try {
                InputStream mysqlIn = mysqlSocket.getInputStream();
                OutputStream clientOut = clientSocket.getOutputStream();

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = mysqlIn.read(buffer)) != -1) {
                    int packetLength = (buffer[0] & 0xFF) | ((buffer[1] & 0xFF) << 8) | ((buffer[2] & 0xFF) << 16); // 数据包的长度
                    int sequenceNumber = buffer[3] & 0xFF;  // 序号
                    String responseData = new String(Arrays.copyOfRange(buffer, 5, bytesRead));

                    // 数据包中数据的偏移位置
//                    int dataIndex = 4;
//                    while (dataIndex < bytesRead) {
//                        // 解析数据类型
//                        byte dataType = buffer[dataIndex];
//                        dataIndex++; // 数据类型字节后移一位
//
//                        // 根据数据类型解析值
//                        switch (dataType) {
//                            case 0x00: // 数据类型为NULL
//                                System.out.println("NULL");
//                                break;
//
//                            case 0x01: // 数据类型为整数
//                                int intValue = (buffer[dataIndex] & 0xFF) |
//                                        ((buffer[dataIndex + 1] & 0xFF) << 8) |
//                                        ((buffer[dataIndex + 2] & 0xFF) << 16) |
//                                        ((buffer[dataIndex + 3] & 0xFF) << 24);
//                                dataIndex += 4; // 整数占用4个字节
//                                System.out.println("整数值: " + intValue);
//                                break;
//
//                            case 0x02: // 数据类型为浮点数
//                                // 解析浮点数的方式根据MySQL协议规范进行
//                                // ...
//                                break;
//
//                            case 0x08: // 数据类型为字符串
//                                int stringLength = (buffer[dataIndex] & 0xFF) | ((buffer[dataIndex + 1] & 0xFF) << 8);
//                                dataIndex += 2; // 字符串长度占用2个字节
//                                String stringValue = new String(Arrays.copyOfRange(buffer, dataIndex, dataIndex + stringLength));
//                                dataIndex += stringLength; // 字符串数据长度
//                                System.out.println("字符串值: " + stringValue);
//                                break;
//
//                            // 其他数据类型的处理
//                            // ...
//
//                            default:
//                                System.err.println("未知数据类型: " + dataType);
//                                break;
//                        }
//                    }

                    clientOut.write(buffer, 0, bytesRead);
                    clientOut.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                    mysqlSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
