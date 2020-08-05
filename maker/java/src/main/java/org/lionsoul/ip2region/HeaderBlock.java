package org.lionsoul.ip2region;

/**
 * header block class
 *
 * @author chenxin<chenxin619315                                                                                                                                                                                                                                                               @                                                                                                                                                                                                                                                               gmail.com>
 */
public class HeaderBlock {
    /**
     * index block start ip address
     */
    private byte[] indexStartIp;

    /**
     * ip address
     */
    private int indexPtr;

    public HeaderBlock(byte[] indexStartIp, int indexPtr) {
        this.indexStartIp = indexStartIp;
        this.indexPtr = indexPtr;
    }

    public byte[] getIndexStartIp() {
        return indexStartIp;
    }

    public HeaderBlock setIndexStartIp(byte[] indexStartIp) {
        this.indexStartIp = indexStartIp;
        return this;
    }

    public int getIndexPtr() {
        return indexPtr;
    }

    public HeaderBlock setIndexPtr(int indexPtr) {
        this.indexPtr = indexPtr;
        return this;
    }

    /**
     * get the bytes for db storage
     *
     * @return byte[]
     */
    public byte[] getBytes() {
        /*
         * +------------+-----------+
         * | 16bytes        | 4bytes    |
         * +------------+-----------+
         *  start ip      index ptr
         */
        byte[] b = new byte[20];

        System.arraycopy(indexStartIp, 0, b, 0, Math.min(indexStartIp.length, 16));
        Util.writeIntLong(b, 16, indexPtr);
        return b;
    }
}
