package org.lionsoul.ip2region.entity;

/**
 * data block class
 *
 * @author chenxin619315@gmail.com
 */
public class DataBlock {
    /**
     * region address
     */
    private String region;

    /**
     * region ptr in the db file
     */
    private int dataPtr;

    /**
     * construct method
     *
     * @param region  region string
     * @param dataPtr data ptr
     */
    public DataBlock(String region, int dataPtr) {
        this.region = region;
        this.dataPtr = dataPtr;
    }

    public DataBlock(String region) {
        this(region, 0);
    }

    public String getRegion() {
        return region;
    }

    public DataBlock setRegion(String region) {
        this.region = region;
        return this;
    }

    public int getDataPtr() {
        return dataPtr;
    }

    public DataBlock setDataPtr(int dataPtr) {
        this.dataPtr = dataPtr;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(region).append('|').append(dataPtr);
        return sb.toString();
    }

}
