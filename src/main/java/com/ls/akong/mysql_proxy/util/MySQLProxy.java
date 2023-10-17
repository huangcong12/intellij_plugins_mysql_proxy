package com.ls.akong.mysql_proxy.util;

import com.intellij.openapi.project.Project;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;

import java.nio.charset.StandardCharsets;

public class MySQLProxy extends AbstractVerticle {
    private final StringBuilder sqlBuilder = new StringBuilder();
    private final CompositeByteBuf composite = Unpooled.compositeBuffer();

    private final String remoteMysqlServerIp;
    private final int remoteMysqlServerPort;
    private final int proxyPort;

    private Project project;

    public MySQLProxy(Project project, String remoteMysqlServerIp, int remoteMysqlServerPort, int proxyPort) {
        this.project = project;
        this.remoteMysqlServerIp = remoteMysqlServerIp;
        this.remoteMysqlServerPort = remoteMysqlServerPort;
        this.proxyPort = proxyPort;
    }

    @Override
    public void start() {
        NetServerOptions options = new NetServerOptions().setPort(this.proxyPort);
        NetServer server = vertx.createNetServer(options);

        server.connectHandler(client -> {
            SqlExecutionStatisticsCollector sqlExecutionStatisticsCollector = new SqlExecutionStatisticsCollector(project);

            vertx.createNetClient().connect(this.remoteMysqlServerPort, this.remoteMysqlServerIp, res -> {
                if (res.succeeded()) {
                    NetSocket mysqlSocket = res.result();
                    client.handler(buffer -> {
                        composite.addComponent(true, Unpooled.wrappedBuffer(buffer.getBytes()));
                        while (composite.readableBytes() > 4) {
                            int length = composite.getUnsignedMediumLE(composite.readerIndex());
                            if (composite.readableBytes() >= length + 4) {
                                byte[] bytes = new byte[length + 4];
                                composite.readBytes(bytes);
                                Buffer packet = Buffer.buffer(bytes);
                                handleClientPacket(packet, mysqlSocket, sqlExecutionStatisticsCollector);
                            } else {
                                break;
                            }
                        }
                    });
                    // 添加处理器来处理 MySQL 服务器返回的数据，并将其发送回 MySQL 客户端
                    mysqlSocket.handler(buffer -> handleResponsePacket(buffer, client, sqlExecutionStatisticsCollector));
                    mysqlSocket.closeHandler(v -> client.close());
                    mysqlSocket.exceptionHandler(err -> client.close());
                }
            });
        });

        server.listen();
    }

    /**
     * 服务器返回包
     *
     * @param buffer
     * @param dst
     * @param sqlExecutionStatisticsCollector
     */
    private void handleClientPacket(Buffer buffer, NetSocket dst, SqlExecutionStatisticsCollector sqlExecutionStatisticsCollector) {
        if (buffer.length() > 4) {
            int packetLength = buffer.getMediumLE(0);
            int packetType = buffer.getByte(4);
            if (packetType == 0x03) { // COM_QUERY
                sqlExecutionStatisticsCollector.startTiming(); // 开始计时
                handleComQuery(buffer, sqlExecutionStatisticsCollector);
            } else if (packetType == 0x16) { // COM_STMT_PREPARE
                sqlExecutionStatisticsCollector.startTiming(); // 开始计时
                handleComStmtPrepare(buffer, sqlExecutionStatisticsCollector);
            } else if (packetType == 0x17 && packetLength > 1) { // COM_STMT_EXECUTE
                sqlExecutionStatisticsCollector.startTiming(); // 开始计时
                handleComStmtExecute(buffer, sqlExecutionStatisticsCollector);
            } else if (packetType == 0x02) {    // COM_INIT_DB 选择数据库
                sqlExecutionStatisticsCollector.setDatabaseName(buffer.getString(5, buffer.length()));
            }
        }

        dst.write(buffer);
    }

    /**
     * 客户端请求包
     *
     * @param buffer
     * @param dst
     * @param sqlExecutionStatisticsCollector
     */
    private void handleResponsePacket(Buffer buffer, NetSocket dst, SqlExecutionStatisticsCollector sqlExecutionStatisticsCollector) {
        if (buffer.length() > 4) {
            int packetType = buffer.getByte(4);
            if (buffer.getUnsignedMediumLE(0) == 12 && packetType == 0x00) { // COM_STMT_PREPARE_OK
                handleComStmtPrepareOk(buffer, sqlExecutionStatisticsCollector);
            }
        }

        dst.write(buffer);

        // 结束计时
        sqlExecutionStatisticsCollector.stopTiming();
    }

    /**
     * 普通查询
     *
     * @param buffer
     */
    private void handleComQuery(Buffer buffer, SqlExecutionStatisticsCollector sqlExecutionStatisticsCollector) {
        String sqlPart = buffer.slice(5, buffer.length()).toString(StandardCharsets.UTF_8);
        sqlBuilder.append(sqlPart);
        if (buffer.length() < 65536) { // 65536 是 MySQL 协议包的最大长度，如果小于这个值，说明 SQL 语句已经完整
            // 设置 sql
            sqlExecutionStatisticsCollector.setSql(sqlBuilder.toString());
//            System.out.println("sql: " + sqlBuilder.toString());
            sqlBuilder.setLength(0); // 清空 StringBuilder
        }
    }

    /**
     * 暂存预处理 sql
     *
     * @param buffer
     */
    private void handleComStmtPrepare(Buffer buffer, SqlExecutionStatisticsCollector sqlExecutionStatisticsCollector) {
        // 暂存预处理 sql
        sqlExecutionStatisticsCollector.setPreparingStatement(buffer.slice(5, buffer.length()).toString(StandardCharsets.UTF_8));
    }

    /**
     * 根据执行包的 statementId，取出对应的 SQL
     *
     * @param buffer
     * @param sqlExecutionStatisticsCollector
     */
    private void handleComStmtExecute(Buffer buffer, SqlExecutionStatisticsCollector sqlExecutionStatisticsCollector) {
        if (buffer.length() < 15) {
            return;
        }

        int statementId = buffer.getIntLE(5); // 提取语句 ID
        String preSql = sqlExecutionStatisticsCollector.removePreparedStatement(statementId);

        if (preSql == null) {
            System.err.println("Error: Cannot find the prepared statement for statementId: " + statementId);
            return;
        }

        PreparedSqlComposer stmtExecute = new PreparedSqlComposer(buffer.getBytes(), preSql);
        stmtExecute.fillingParameter();
        String sql = stmtExecute.getSql();

        // 设置 sql
        sqlExecutionStatisticsCollector.setSql(sql);
//        System.out.println("SQL: " + sql);
    }

    /**
     * 取出预处理 sql 返回的 ok 包里的 statementId，并和预处理 sql 关联起来
     *
     * @param buffer
     * @param sqlExecutionStatisticsCollector
     */
    private void handleComStmtPrepareOk(Buffer buffer, SqlExecutionStatisticsCollector sqlExecutionStatisticsCollector) {
        int statementId = buffer.getIntLE(5); // 提取语句 ID
        if (statementId > 100) {
            System.out.println("捕获到异常的 " + statementId);
        }

        sqlExecutionStatisticsCollector.putToPreparedStatements(statementId, sqlExecutionStatisticsCollector.getPreparingStatement());
    }
}


