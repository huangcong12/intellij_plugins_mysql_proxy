package com.ls.akong.mysql_proxy.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.ls.akong.mysql_proxy.model.SqlLogModel;

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

    private Project project;

    private boolean isServiceRunning = false;
    private ServerSocket serverSocket;

    private List<Socket> clientSockets;

    private List<MysqlProxyServiceStateListener> listeners = new ArrayList<>();


    public MySQLProxyServerService(Project project) {
        this.project = project;

        clientSockets = new ArrayList<>();
    }

    // 根据命令字节获取命令名称
    private static String getCommandName(int commandByte) {
        switch (commandByte) {
            case 0x00:
                return "Sleep"; // 该命令没有特定的用途，并且不应由客户端发送
            case 0x01:
                return "Quit";  // 客户端用此命令告知服务器该客户端将断开连接
            case 0x02:
                return "Init Db";   // 客户端使用此命令告知服务器它想要更改默认操作的数据库
            case 0x03:
                return "Query";     // 这可能是最常用的命令，客户端用它来向服务器发送SQL语句
            case 0x04:
                return "Field List";    // 此命令用于获取数据表的字段信息
            case 0x05:
                return "Create Db";    // 用于创建数据库，此命令已经不再使用
            case 0x06:
                return "Drop Db";    // 用于删除数据库，此命令已经不再使用
            case 0x07:
                return "Refresh";    // 用于清空缓存，重读my.cnf文件等操作
            case 0x08:
                return "Shutdown";    // 关闭MySQL服务器
            case 0x09:
                return "Statistic";    // 用于获取服务器统计信息
            case 0x10:
                return "Prepare Statement";
            case 0x11:
                return "Execute Statement";
            case 0x12:
                return "Prepared Execute";
            case 0x16:
                return "Stmt Prepare";      // Prepare一个SQL语句并返回给客户端，预处理的SQL语句带有占位符
            case 0x17:
                return "Stmt Execute";      // 执行已经Prepare的SQL语句
            case 0x18:
                return "Stmt Send Long Data";      // 发送BLOB类型的数据
            case 0x19:
                return "Stmt Close";      // 关闭已经Prepare的SQL语句
            case 0x1a:
                return "Stmt Reset";      // 重置已经Prepare的SQL语句，使其可以再被执行
            case 0x1b:
                return "Set Option";      // 设置或重置客户端的语句执行选项
            case 0x2D:
                return "Transaction Begin";
            case 0x2E:
                return "Transaction Commit";
            // 添加更多命令类型和名称映射
            default:
                return "Unknown" + commandByte;
        }
    }

    /**
     * 服务器是否运行中
     *
     * @return
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
                String errorMessage = "Run Failed, Error Message:\n" + bindException.getMessage() + "\n\n" +
                        "Possible Reasons for the Failure:\n" +
                        "1. Port " + port + " already in use. Please modify to a different port and retry.\n" +
                        "2. Insufficient port binding permissions. If you're using a Linux-based system, consider using a port between 1024 and 65535.";
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

    public void addListener(MysqlProxyServiceStateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(MysqlProxyServiceStateListener listener) {
        listeners.remove(listener);
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
                OutputStream clientOut = clientSocket.getOutputStream();
                InputStream mysqlIn = mysqlSocket.getInputStream();
                OutputStream mysqlOut = mysqlSocket.getOutputStream();

                byte[] buffer = new byte[4096];
                int bytesRead;
                int packetLength = 0;
                int commandByte = 0;
                StringBuilder sqlBuilder = new StringBuilder(); // 用于存储SQL语句

                while ((bytesRead = clientIn.read(buffer)) != -1) {
                    // 将接收到的数据转换为字符串
                    String requestData = new String(Arrays.copyOfRange(buffer, 4, bytesRead));
                    sqlBuilder.append(requestData);
                    String sqlQuery = sqlBuilder.toString().trim();
                    // 去掉换行符、多个空格只保留一个
                    sqlQuery = sqlQuery.replaceAll("\n", " ").replaceAll("\r", " ").replaceAll(" +", " ");

                    // 获取包头中的信息
                    int sequenceNumber = buffer[3] & 0xFF;  // 序号
                    // 只有第一个包的长度是准确的，如果分包会不准确，因此这样处理
                    if (sequenceNumber == 0) {
                        packetLength = (buffer[0] & 0xFF) | ((buffer[1] & 0xFF) << 8) | ((buffer[2] & 0xFF) << 16); // 数据包的长度
                        commandByte = buffer[4] & 0xFF;     // 数据包中的命令字节
                    }

                    // !sqlQuery.equals("") 是客户端会发送 0 长度的包保存连接；sqlBuilder.length() >= packetLength - 4：满包，兼容长 sql，数据包分多个的情况
                    if (!sqlQuery.equals("") && sqlBuilder.length() >= packetLength - 4) {
                        // 只记录 Query 的到表里
                        MysqlProxySettings recordingSwitch = MysqlProxySettings.getInstance(project);
                        if (recordingSwitch.isMonitorEnabled() && commandByte == 0x03) {
                            SqlLogModel.insertLog(project, sqlQuery);
                            // 通知页面展示
                            MyTableView myTableView = MyTableView.getInstance(project);
                            myTableView.updateData();
                        }

                        // 重置收集器
                        sqlBuilder.setLength(0);
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
