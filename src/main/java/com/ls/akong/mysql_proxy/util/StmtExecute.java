package com.ls.akong.mysql_proxy.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 专门用于 COM_STMT_EXECUTE 包处理的类
 */
public class StmtExecute {
    /**
     * 参数起始位
     *
     * @param int
     */
    private final int paramsStartPosition = 16;
    private byte[] packetData;
    private int length;

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

        // 先获取参数的类型
        List<Integer> paramsType = new ArrayList<>();
        int position = paramsStartPosition;
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
                    int paramLength = packetData[position++];

                    paramsList[i] = "`" + new String(Arrays.copyOfRange(packetData, position, (position + paramLength))) + "`";
                    // 调整指针位置
                    position += paramLength - 1;
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
}
