package com.zmm.chatroom.util;

import java.util.Random;

/**
 * @author zmm
 * @date 2021/7/6 20:00
 */
public class IDUtils {

    /** 机器id所占的位数 */
    private static final long workerIdBits = 5L;

    /** 数据标识id所占的位数 */
    private static final long datacenterIdBits = 5L;

    /** 毫秒内序列(0~4095) */
    private static long sequence = 0L;

    /** 上次生成ID的时间截 */
    private static long lastTimestamp = -1L;

    public static int machineId = new Random().nextInt(100);

    /**
     * 获得下一个ID (该方法是线程安全的)
     * @return SnowflakeId
     */
    public static synchronized long nextId() {
        long timestamp = timeGen();

        // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(
                    String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }

        // 如果是同一时间生成的，则进行毫秒内序列
        // 序列在id中占的位数
        final long sequenceBits = 12L;
        if (lastTimestamp == timestamp) {
            // 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)
            long sequenceMask = ~(-1L << sequenceBits);
            sequence = (sequence + 1) & sequenceMask;
            //毫秒内序列溢出
            if (sequence == 0) {
                //阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp);
            }
        }
        //时间戳改变，毫秒内序列重置
        else {
            sequence = 0L;
        }

        //上次生成ID的时间截
        lastTimestamp = timestamp;

        //移位并通过或运算拼到一起组成64位的ID
        final long currentStamp = 1565020800000L;
        long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

        // ((当前时间) << 12 + 5 + 5) | 0 | 0
        return ((timestamp - currentStamp) << timestampLeftShift) // 时间戳部分
                | machineId << sequenceBits                       // 机器标识部分
                | sequence;                                       // 序列号部分
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     * @param lastTimestamp 上次生成ID的时间截
     * @return 当前时间戳
     */
    protected static long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 返回以毫秒为单位的当前时间
     * @return 当前时间(毫秒)
     */
    protected static long timeGen() {
        return System.currentTimeMillis();
    }

}
