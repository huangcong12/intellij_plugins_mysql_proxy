package com.ls.akong.mysql_proxy.util;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;

public class MySQLMessage {
    public static final long NULL_LENGTH = -1;
    private static final byte[] EMPTY_BYTES = new byte[0];
    private static final ThreadLocal<Calendar> localCalendar = new ThreadLocal<Calendar>();
    private final byte[] data;
    private final int length;
    private int position;

    // 用于组装 sql，当很长的一条 sql 分多个包发送的时候，只用 MySQLMessage 不能满足，因此抽离一个 sqlBuilder
    private SqlBuilder sqlBuilder;

    public MySQLMessage(byte[] data, SqlBuilder sqlBuilder) {
        this.data = data;
        this.length = data.length;
        this.position = 0;

        this.sqlBuilder = sqlBuilder;
    }

    /**
     * 获取 SQL
     *
     * @return
     */
    public String getSql() {
        // 发送预处理参数的包，特殊处理
        if (getCommandByte() == MySQLCommandByte.COM_STMT_EXECUTE) {
            StmtExecute statement = new StmtExecute(data, sqlBuilder.getStmtExecuteParamsLength());
            // 填充参数
            sqlBuilder.fillingParameter(statement.params());
        } else {
            // 普通包
            byte[] newBuffer = Arrays.copyOfRange(data, 5, length);
            sqlBuilder.append(new String(newBuffer, StandardCharsets.UTF_8));

            // 满包才返回 sql，因为需要兼容长 sql，一条 sql 分多个包发送的情况
//            if (!sqlBuilder.isCollectionComplete()) {
//                return "";
//            }

            // 预处理包不返回 sql，等待 COM_STMT_EXECUTE 才返回
            if (getCommandByte() == MySQLCommandByte.COM_STMT_PREPARE || getCommandByte() == MySQLCommandByte.COM_STMT_CLOSE) {
                return "";
            }
        }

        String sql = sqlBuilder.toString();
        sqlBuilder.reset();

        if (sql.contains("mysql_native_password")) {    // mysql_native_password 是发送账号密码
            return "";
        }

        return sql;
    }

    /**
     * 获取序号，默认是 0，如果是长 SQL 分多个包，后面包的序号会递增，表示这个包是前一个包的一部分
     *
     * @return int
     */
    public int getSequenceNumber() {
        return data[3] & 0xFF;  // 序号
    }

    /**
     * 获取数据包的长度
     *
     * @return int
     */
    public int getPackageLength() {
        return (data[0] & 0xFF) | ((data[1] & 0xFF) << 8) | ((data[2] & 0xFF) << 16); // 数据包的长度
    }

    /**
     * 获取数据包中的命令字节
     *
     * @return int
     */
    public int getCommandByte() {
        return data[4] & 0xFF;     // 数据包中的命令字节。这个字段不能信任，比如 wordpress 这个字段都是 3（query）
    }

    public int length() {
        return length;
    }

    public int position() {
        return position;
    }

    public byte[] bytes() {
        return data;
    }

    public void move(int i) {
        position += i;
    }

    public void position(int i) {
        this.position = i;
    }

    public boolean hasRemaining() {
        return length > position;
    }

    public byte read(int i) {
        return data[i];
    }

    public byte read() {
        return data[position++];
    }

    public int readUB2() {
        final byte[] b = this.data;
        int i = b[position++] & 0xff;
        i |= (b[position++] & 0xff) << 8;
        return i;
    }

    public int readUB3() {
        final byte[] b = this.data;
        int i = b[position++] & 0xff;
        i |= (b[position++] & 0xff) << 8;
        i |= (b[position++] & 0xff) << 16;
        return i;
    }

    public long readUB4() {
        final byte[] b = this.data;
        long l = (long) (b[position++] & 0xff);
        l |= (long) (b[position++] & 0xff) << 8;
        l |= (long) (b[position++] & 0xff) << 16;
        l |= (long) (b[position++] & 0xff) << 24;
        return l;
    }

    public int readInt() {
        final byte[] b = this.data;
        int i = b[position++] & 0xff;
        i |= (b[position++] & 0xff) << 8;
        i |= (b[position++] & 0xff) << 16;
        i |= (b[position++] & 0xff) << 24;
        return i;
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public long readLong() {
        final byte[] b = this.data;
        long l = (long) (b[position++] & 0xff);
        l |= (long) (b[position++] & 0xff) << 8;
        l |= (long) (b[position++] & 0xff) << 16;
        l |= (long) (b[position++] & 0xff) << 24;
        l |= (long) (b[position++] & 0xff) << 32;
        l |= (long) (b[position++] & 0xff) << 40;
        l |= (long) (b[position++] & 0xff) << 48;
        l |= (long) (b[position++] & 0xff) << 56;
        return l;
    }

    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    public long readLength() {
        int length = data[position++] & 0xff;
        switch (length) {
            case 251:
                return NULL_LENGTH;
            case 252:
                return readUB2();
            case 253:
                return readUB3();
            case 254:
                return readLong();
            default:
                return length;
        }
    }

    public byte[] readBytes() {
        if (position >= length) {
            return EMPTY_BYTES;
        }
        byte[] ab = new byte[length - position];
        System.arraycopy(data, position, ab, 0, ab.length);
        position = length;
        return ab;
    }

    public byte[] readBytes(int length) {
        byte[] ab = new byte[length];
        System.arraycopy(data, position, ab, 0, length);
        position += length;
        return ab;
    }

    public byte[] readBytesWithNull() {
        final byte[] b = this.data;
        if (position >= length) {
            return EMPTY_BYTES;
        }
        int offset = -1;
        for (int i = position; i < length; i++) {
            if (b[i] == 0) {
                offset = i;
                break;
            }
        }
        switch (offset) {
            case -1:
                byte[] ab1 = new byte[length - position];
                System.arraycopy(b, position, ab1, 0, ab1.length);
                position = length;
                return ab1;
            case 0:
                position++;
                return EMPTY_BYTES;
            default:
                byte[] ab2 = new byte[offset - position];
                System.arraycopy(b, position, ab2, 0, ab2.length);
                position = offset + 1;
                return ab2;
        }
    }

    public int getRowLength(int fileldCount) {
        int size = 0;
        int bak_position = position;
        position += 4;
        for (int i = 0; i < fileldCount; i++) {
            int length = (int) readLength();
            if (length == NULL_LENGTH || length <= 0) {
                continue;
            }

            position += length;
            size += length;
        }
        position = bak_position;
        return size;
    }

    public byte[] readBytesWithLength() {
        int length = (int) readLength();
        if (length == NULL_LENGTH) {
            return null;
        }
        if (length <= 0) {
            return EMPTY_BYTES;
        }

        byte[] ab = new byte[length];
        System.arraycopy(data, position, ab, 0, ab.length);
        position += length;
        return ab;
    }

    public String readString() {
        if (position >= length) {
            return null;
        }
        String s = new String(data, position, length - position);
        position = length;
        return s;
    }

    public String readString(String charset) throws UnsupportedEncodingException {
        if (position >= length) {
            return null;
        }

        String s = new String(data, position, length - position, charset);
        position = length;
        return s;
    }

    public String readStringWithNull() {
        final byte[] b = this.data;
        if (position >= length) {
            return null;
        }
        int offset = -1;
        for (int i = position; i < length; i++) {
            if (b[i] == 0) {
                offset = i;
                break;
            }
        }
        if (offset == -1) {
            String s = new String(b, position, length - position);
            position = length;
            return s;
        }
        if (offset > position) {
            String s = new String(b, position, offset - position);
            position = offset + 1;
            return s;
        } else {
            position++;
            return null;
        }
    }

    public String readStringWithNull(String charset) throws UnsupportedEncodingException {
        final byte[] b = this.data;
        if (position >= length) {
            return null;
        }
        int offset = -1;
        for (int i = position; i < length; i++) {
            if (b[i] == 0) {
                offset = i;
                break;
            }
        }
        switch (offset) {
            case -1:
                String s1 = new String(b, position, length - position, charset);
                position = length;
                return s1;
            case 0:
                position++;
                return null;
            default:
                String s2 = new String(b, position, offset - position, charset);
                position = offset + 1;
                return s2;
        }
    }

    public String readStringWithLength() {
        int length = (int) readLength();
        if (length <= 0) {
            return null;
        }
        String s = new String(data, position, length);
        position += length;
        return s;
    }

    public String readStringWithLength(String charset) throws UnsupportedEncodingException {
        int length = (int) readLength();
//        if (length <= 0) {
//            return null;
//        }
        String s = new String(data, position, length, charset);
        position += length;
        return s;
    }


    public BigDecimal readBigDecimal() {
        String src = readStringWithLength();
        return src == null ? null : new BigDecimal(src);
    }

    public String toString() {
        return new StringBuilder().append(Arrays.toString(data)).toString();
    }
}
