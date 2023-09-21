package com.ls.akong.mysql_proxy.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 专门用于 COM_STMT_EXECUTE 包处理的类
 */
public class StmtExecute {
    public static final long NULL_LENGTH = -1;

    /**
     * 参数起始位
     *
     * @param int
     */
    private final int paramsStartPosition = 16;
    private byte[] packetData;
    private int length;

    private int position;

    public StmtExecute(byte[] packetData, int length) {
        this.packetData = packetData;
        this.length = length;
    }


    /**
     * 获取数据包里的所有参数
     *
     * @return
     */
    public Object[] params() {
        Object[] paramsList = new Object[length];
        if (length == 0 || packetData.length <= paramsStartPosition) {
            return paramsList;
        }

        // 20230912 根据松哥提供的 yylAdmin 项目研究出来的，也不懂为啥这样计算
        position = (15 + new byte[(paramsList.length + 7) / 8].length);

        List<Integer> paramsType = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            paramsType.add(packetData[position++] & 0xff | (packetData[position++] & 0xff) << 8);
        }

        for (int i = 0; i < paramsType.size(); i++) {
            int paramType = paramsType.get(i);
            switch (paramType) {
                case MySQLFields.FIELD_TYPE_LONGLONG:       // 数据库字段是 int、tinyint 会走这里
                    paramsList[i] = packetData[position++] & 0xff | (packetData[position++] & 0xff) << 8 | (packetData[position++] & 0xff) << 16 | (packetData[position++] & 0xff) << 24 | (packetData[position++] & 0xff) << 32 | (packetData[position++] & 0xff) << 40 | (packetData[position++] & 0xff) << 48 | (packetData[position] & 0xff) << 56;
                    break;
                case MySQLFields.FIELD_TYPE_VAR_STRING:     // 数据库字段是 varchar 会走这里
                case MySQLFields.FIELD_TYPE_STRING:
                case MySQLFields.FIELD_TYPE_VARCHAR:
                    // 参数长度
                    int paramLength = (int) readLength();
                    if (paramLength == NULL_LENGTH) {
                        paramsList[i] = null;
                    } else {
                        paramsList[i] = "'" + new String(Arrays.copyOfRange(packetData, position, (position + paramLength)), StandardCharsets.UTF_8) + "'";
                        // 调整指针位置
                        position += paramLength - 1;
                    }

                    break;
                default:
                    paramsList[i] = "`Unknown type`:" + paramType;
            }

            // 如果还有参数，则帮忙加到下一个的开始，因为需要考虑最后一个，不能再进行 position++ 的情况
            if (packetData.length > position) {
                position += 1;
            }
        }

        return paramsList;
    }

    public long readLength() {
        int length = packetData[position++] & 0xff;
        switch (length) {
            case 251:
                return NULL_LENGTH;
            case 252:
                return packetData[position++] & 0xff | (packetData[position++] & 0xff) << 8;
            case 253:
                return packetData[position++] & 0xff | (packetData[position++] & 0xff) << 8 | (packetData[position++] & 0xff) << 16;
            case 254:
                return packetData[position++] & 0xff | (packetData[position++] & 0xff) << 8 | (packetData[position++] & 0xff) << 16
                        | (long) (packetData[position++] & 0xff) << 24
                        | (long) (packetData[position++] & 0xff) << 32
                        | (long) (packetData[position++] & 0xff) << 40
                        | (long) (packetData[position++] & 0xff) << 48
                        | (long) (packetData[position++] & 0xff) << 56;
            default:
                return length;
        }
    }
}
