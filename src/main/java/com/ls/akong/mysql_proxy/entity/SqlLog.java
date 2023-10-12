package com.ls.akong.mysql_proxy.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Sql 日志记录表
 */
public class SqlLog {
    private static String tableName = "sql_log2";
    private final int id;
    private final String sql;
    private final Long createdAt;
    private final long executionTime;
    private final String signature;

    public SqlLog(int id, String sql, Long createdAt, long executionTime, String signature) {
        this.id = id;
        this.sql = sql;
        this.createdAt = createdAt;
        this.executionTime = executionTime;
        this.signature = signature;
    }

    public static String getTableName() {
        return tableName;
    }

    public int getId() {
        return id;
    }

    public String getSql() {
        return sql;
    }

    public String getCreatedAt() {
        Instant savedInstant = Instant.ofEpochMilli(createdAt);
        ZonedDateTime zonedDateTime = savedInstant.atZone(ZoneId.systemDefault());

        LocalDate currentDate = LocalDate.now();
        LocalDate inputDate = zonedDateTime.toLocalDate();

        String formattedDate;

        if (inputDate.equals(currentDate)) {
            formattedDate = "Today";
        } else if (inputDate.equals(currentDate.minusDays(1))) {
            formattedDate = "Yesterday";
        } else {
            DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy/M/d");
            formattedDate = inputDate.format(customFormatter);
        }

        DateTimeFormatter customTimeFormatter = DateTimeFormatter.ofPattern("a h:mm:ss");
        return formattedDate + " " + zonedDateTime.format(customTimeFormatter);
    }

    public long getExecutionTime() {
        return executionTime;
    }

    /**
     * 获取执行时间
     *
     * @return
     */
    public String getFormatExecutionTime() {
        if (executionTime >= 1000) {
            double seconds = executionTime / 1000.0;
            return String.format("%.2f s", seconds);
        }
        return executionTime + " ms";
    }
}
