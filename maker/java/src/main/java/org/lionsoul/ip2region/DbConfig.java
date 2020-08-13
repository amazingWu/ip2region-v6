package org.lionsoul.ip2region;

import org.lionsoul.ip2region.entity.IndexBlock;
import org.lionsoul.ip2region.exception.DbMakerConfigException;

/**
 * database configuration class
 *
 * @author chenxin619315@gmail.com
 */
public class DbConfig {
    /**
     * max index data block size
     * u should always choice the fastest read block size
     */
    private int indexPartitionSize;

    /**
     * construct method
     *
     * @param indexPartitionNum index num of each index partition containing
     * @throws DbMakerConfigException
     */
    public DbConfig(int indexPartitionNum) throws DbMakerConfigException {
        // each index partition save 2048 index
        this.indexPartitionSize = IndexBlock.getIndexBlockLength() * indexPartitionNum;
    }

    public DbConfig() throws DbMakerConfigException {
        /**
         * default value can support 2048 * 2048 records
         */
        this(2048);
    }

    public int getIndexPartitionSize() {
        return indexPartitionSize;
    }

    public DbConfig setIndexPartitionSize(int dataBlockSize) {
        this.indexPartitionSize = dataBlockSize;
        return this;
    }
}
