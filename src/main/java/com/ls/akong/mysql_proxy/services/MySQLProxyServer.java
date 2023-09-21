package com.ls.akong.mysql_proxy.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.ls.akong.mysql_proxy.model.SqlLogModel;
import com.ls.akong.mysql_proxy.util.MySQLMessage;
import com.ls.akong.mysql_proxy.util.SqlBuilder;
import com.ls.akong.mysql_proxy.util.SqlRunTimer;

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
public final class MySQLProxyServer implements Disposable {

    private static final Logger logger = Logger.getInstance(MySQLProxyServer.class);

    private final Project project;
    private final List<Socket> clientSockets;
    private final List<MysqlProxyServiceStateListener> listeners = new ArrayList<>();
    private boolean isServiceRunning = false;
    private ServerSocket serverSocket;


    public MySQLProxyServer(Project project) {
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

            // 马上通知一遍，因为跟随编辑器启动的逻辑运行比较早，那时候还没有订阅，因此需要马上通知一遍，更改图标
            notifyListeners();
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
                SqlRunTimer sqlRunTimer = new SqlRunTimer(project);
                Thread responseThread = new Thread(new ResponseHandler(clientSocket, mysqlSocket, sqlRunTimer));
                responseThread.start();

                // 转发客户端请求给 MySQL 服务器
                InputStream clientIn = clientSocket.getInputStream();
                OutputStream mysqlOut = mysqlSocket.getOutputStream();

                // 原来这里是 4096 的，但是 MySQL 的包最大是 16 M，如果我们自己切，难以知道哪个包是最后的，不能确认一条超级长的 SQL 是否完毕
                // 因此调整成 16 M:16 * 1024 * 1024=16777216
                byte[] buffer = new byte[16777216];
                int bytesRead;
                SqlBuilder sqlBuilder = new SqlBuilder();

                while ((bytesRead = clientIn.read(buffer)) != -1) {
                    MysqlProxySettings recordingSwitch = MysqlProxySettings.getInstance(project);
                    if (recordingSwitch.isMonitorEnabled()) {
                        // 20230909 发现预处理的包 23、25 合并成一个包发送的情况，因此需要判断是否是合并包发送的，如果是还需分开处理
                        for (int i = 0; i < bytesRead; ) {
                            int length = (buffer[i] & 0xFF) | ((buffer[i + 1] & 0xFF) << 8) | ((buffer[i + 2] & 0xFF) << 16);

                            byte[] itemRawBuffer = Arrays.copyOfRange(buffer, i, i + length + 4);
                            MySQLMessage mm = new MySQLMessage(itemRawBuffer, sqlBuilder);

                            int sequenceNumber = mm.getSequenceNumber();
                            if (sequenceNumber == 0) {
                                sqlBuilder.setTargetLength(mm.getPackageLength() - 4);
                                sqlBuilder.setCommandByte(mm.getCommandByte());
                            }

                            String sql = mm.getSql();
                            if (!sql.equals("")) {
                                long id = SqlLogModel.insertLog(project, sql);
                                if (id != -1) {
                                    sqlRunTimer.setId(id);
                                    sqlRunTimer.startTimer();
                                }

                                // Notify the UI to update for the specified project
                                MyTableView myTableView = MyTableView.getInstance(project);
                                myTableView.updateData();
                            }

                            i += length + 4;
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

        private SqlRunTimer sqlRunTimer;

        public ResponseHandler(Socket clientSocket, Socket mysqlSocket, SqlRunTimer sqlRunTimer) {
            this.clientSocket = clientSocket;
            this.mysqlSocket = mysqlSocket;
            this.sqlRunTimer = sqlRunTimer;
        }

        @Override
        public void run() {
            try {
                InputStream mysqlIn = mysqlSocket.getInputStream();
                OutputStream clientOut = clientSocket.getOutputStream();

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = mysqlIn.read(buffer)) != -1) {
                    // 参考：https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_basic_response_packets.html
                    int header = buffer[4] & 0xFF; // header
                    if (header == 0x00 || header == 0xFE) {     // Update response successfully
                        sqlRunTimer.stopTimer();
                    } else if (header == 0xFF) {                // Update response fail
                        sqlRunTimer.stopTimer();
                    } else if ((buffer[bytesRead - 9] & 0xFF) == 5 && (buffer[bytesRead - 8] & 0xFF) == 0
                            && (buffer[bytesRead - 7] & 0xFF) == 0 && (buffer[bytesRead - 5] & 0xFF) == 254) {  // Query response success
                        sqlRunTimer.stopTimer();
                    }

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
