package org.lionsoul.ip2region;

/**
 * @author wuqi3@corp.netease.com 2020-08-04 20:25
 * @since
 */
public enum QueryType {
    /**
     * 内存模式，线程安全
     */
    MEMORY,
    /**
     * 二分查找模式。
     * 不是线程安全的实现，不同线程可以通过创建不同的查询对象来使用，并发量很大的情况下，
     * 可能会产生打开文件数过多的错误，请修改内核的最大允许打开文件数(fs.file-max=一个更高的值)，或者使用持久化的memory算法。
     */
    BINARY,
    /**
     * btree模式。
     * 不是线程安全的实现，不同线程可以通过创建不同的查询对象来使用，并发量很大的情况下，
     * 可能会产生打开文件数过多的错误，请修改内核的最大允许打开文件数(fs.file-max=一个更高的值)，或者使用持久化的memory算法。
     */
    BTREE
}
