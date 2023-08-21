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
    private final int id;
    private final String sql;
    private final Long createdAt;

    public SqlLog(int id, String sql, Long createdAt) {
        this.id = id;
        this.sql = sql;
        this.createdAt = createdAt;
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
}
