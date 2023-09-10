package com.ls.akong.mysql_proxy.util;

import java.util.List;

/**
 * 用于组装 sql
 */
public class SqlBuilder {
    private final StringBuilder builder;
    /**
     * 目标长度，如果一条 sql 分多个包发送，第一个包会有 sql 目标长度，后面的附加包没有，因此需要存起来，用于判断一条 sql 是否已收集完成
     */
    private int targetLength = 0;
    /**
     * 数据包的类型，放这里是因为需要兼容预处理包，进行跨包组装 sql
     */
    private int commandByte = 0;

    public SqlBuilder() {
        builder = new StringBuilder();
    }

    public void setCommandByte(int commandByte) {
        this.commandByte = commandByte;
    }

    /**
     * 目标 sql 长度。数据包为 0 的时候，会告诉 sql 的长度
     *
     * @param targetLength
     */
    public void setTargetLength(int targetLength) {
        this.targetLength = targetLength;
    }

    /**
     * 追加 sql
     *
     * @param sql
     */
    public void append(String sql) {
        builder.append(sql);
    }

    /**
     * 当前 sql 的长度
     *
     * @return int
     */
    public int length() {
        return builder.length();
    }

    /**
     * 重置
     */
    public void reset() {
        builder.setLength(0);
    }

    /**
     * sql 是否收集完成
     *
     * @return boolean
     */
    public boolean isCollectionComplete() {
        return length() >= targetLength;
    }

    /**
     * 转成一条 sql
     *
     * @return string
     */
    public String toString() {
        String sqlQuery = builder.toString().trim();
        // 去掉换行符、多个空格只保留一个
        return sqlQuery.replaceAll("\n", " ").replaceAll("\r", " ").replaceAll(" +", " ");
    }

    /**
     * 获取预处理 SQL 参数的个数：统计 ? 的数量
     *
     * @return int
     */
    public int getStmtExecuteParamsLength() {
        int count = 0;
        for (int i = 0; i < builder.length(); i++) {
            if (builder.charAt(i) == '?') {
                count++;
            }
        }
        return count;
    }

    public void fillingParameter(Object[] params) {
        int paramIndex = 0;
        int paramCount = params.length;

        while (builder.indexOf("?") != -1 && paramIndex < paramCount) {
            int index = builder.indexOf("?");
            builder.replace(index, index + 1, params[paramIndex].toString());
            paramIndex++;
        }
    }
}
