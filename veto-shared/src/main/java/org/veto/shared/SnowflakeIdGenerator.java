package org.veto.shared;

import java.util.Objects;

public class SnowflakeIdGenerator {

    // 1. 核心参数
    
    // 开始时间戳（2025-01-01 00:00:00.000）
    private final static long START_TIMESTAMP = 1735689600000L;
    
    // 机器ID所占位数
    private final static long WORKER_ID_BITS = 5L;
    // 数据中心ID所占位数
    private final static long DATA_CENTER_ID_BITS = 5L;
    // 序列号所占位数
    private final static long SEQUENCE_BITS = 12L;

    // 2. 核心位移
    
    // 机器ID最大值（31）
    private final static long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    // 数据中心ID最大值（31）
    private final static long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);
    
    // 机器ID向左位移12位
    private final static long WORKER_ID_SHIFT = SEQUENCE_BITS;
    // 数据中心ID向左位移17位
    private final static long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    // 时间戳向左位移22位
    private final static long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;

    // 3. 实例属性
    
    private final long workerId; // 机器ID (0~31)
    private final long dataCenterId; // 数据中心ID (0~31)
    private long sequence = 0L; // 序列号
    private long lastTimestamp = -1L; // 上一次生成ID的时间戳

    /**
     * 构造函数
     * @param workerId 机器ID，范围 [0, 31]
     * @param dataCenterId 数据中心ID，范围 [0, 31]
     */
    public SnowflakeIdGenerator(long workerId, long dataCenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(String.format("Worker ID can't be greater than %d or less than 0", MAX_WORKER_ID));
        }
        if (dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0) {
            throw new IllegalArgumentException(String.format("Data Center ID can't be greater than %d or less than 0", MAX_DATA_CENTER_ID));
        }
        this.workerId = workerId;
        this.dataCenterId = dataCenterId;
    }

    /**
     * 生成下一个 ID
     * @return 唯一的 64 位长整型 ID
     */
    public synchronized long nextId() {
        long currentTimestamp = timeGen();

        // 如果当前时间小于上一次生成 ID 的时间戳，说明系统时钟回拨过
        if (currentTimestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id for " + (lastTimestamp - currentTimestamp) + " milliseconds");
        }

        // 如果在同一毫秒内，序列号自增
        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & ~(-1L << SEQUENCE_BITS);
            // 序列号溢出，等到下一毫秒
            if (sequence == 0) {
                currentTimestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 跨毫秒了，序列号重置为 0
            sequence = 0L;
        }

        // 更新上一次生成 ID 的时间戳
        lastTimestamp = currentTimestamp;

        // 拼接所有位，生成最终 ID
        return ((currentTimestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT) |
               (dataCenterId << DATA_CENTER_ID_SHIFT) |
               (workerId << WORKER_ID_SHIFT) |
               sequence;
    }

    /**
     * 等待下一毫秒，直到获得新的时间戳
     * @param lastTimestamp 上一次生成 ID 的时间戳
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳（毫秒）
     */
    private long timeGen() {
        return System.currentTimeMillis();
    }
    
    // --- 简单测试 ---
    public static void main(String[] args) {
        // 创建一个 ID 生成器，机器ID为1，数据中心ID为1
        SnowflakeIdGenerator idWorker = new SnowflakeIdGenerator(1, 1);
        
        // 连续生成 10 个 ID
        for (int i = 0; i < 10; i++) {
            System.out.println(idWorker.nextId());
        }
    }
}