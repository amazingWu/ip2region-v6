package org.lionsoul.ip2region.entity;

import org.lionsoul.ip2region.utils.ByteUtil;

/**
 * item index class
 *
 * @author chenxin<chenxin619315                                                               @                                                               gmail.com>
 */
public class IndexBlock {
    private static int LENGTH = 36;

    /**
     * start ip address
     */
    private byte[] startIp;

    /**
     * end ip address
     */
    private byte[] endIp;

    /**
     * data ptr and data length
     */
    private int dataPtr;

    /**
     * data length
     */
    private int dataLen;

    public IndexBlock(byte[] startIp, byte[] endIp, int dataPtr, int dataLen) {
        this.startIp = startIp;
        this.endIp = endIp;
        this.dataPtr = dataPtr;
        this.dataLen = dataLen;
    }

    public byte[] getStartIp() {
        return startIp;
    }

    public IndexBlock setStartIp(byte[] startIp) {
        this.startIp = startIp;
        return this;
    }

    public byte[] getEndIp() {
        return endIp;
    }

    public IndexBlock setEndIp(byte[] endIp) {
        this.endIp = endIp;
        return this;
    }

    public int getDataPtr() {
        return dataPtr;
    }

    public IndexBlock setDataPtr(int dataPtr) {
        this.dataPtr = dataPtr;
        return this;
    }

    public int getDataLen() {
        return dataLen;
    }

    public IndexBlock setDataLen(int dataLen) {
        this.dataLen = dataLen;
        return this;
    }

    public static int getIndexBlockLength() {
        return LENGTH;
    }

    /**
     * get the bytes for storage
     *
     * @return byte[]
     */
    public byte[] getBytes() {
        /*
         * +------------+-----------+-----------+
         * | 32bytes        | 32bytes    | 4bytes    |
         * +------------+-----------+-----------+
         *  start ip      end ip      data ptr + len
         */
        byte[] b = new byte[36];
        System.arraycopy(startIp, 0, b, 0, Math.min(startIp.length, 16));
        System.arraycopy(endIp, 0, b, 16, Math.min(startIp.length, 16));

        //write the data ptr and the length
        long mix = dataPtr | ((dataLen << 24) & 0xFF000000L);
        ByteUtil.writeIntLong(b, 32, mix);

        return b;
    }
}
