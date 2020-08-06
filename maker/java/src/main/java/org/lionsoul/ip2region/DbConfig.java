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
     * total header data block size.
     * 太小的话会导致该区域不够建立二级索引，从而会导致覆盖数据区域，因此需要根据 indexNum来确定该值，indexNum为文件行数。
     * 该值需要 > 20 * indexPartitionNum.
     * 其中indexPartitionNum =  indexNum / 2048
     */
    private int totalHeaderSize;

    /**
     * max index data block size
     * u should always choice the fastest read block size
     */
    private int indexPartitionSize;

    /**
     * construct method
     *
     * @param totalHeaderSize
     * @throws DbMakerConfigException
     */
    public DbConfig(int totalHeaderSize) throws DbMakerConfigException {
        if ((totalHeaderSize % 20) != 0) {
            throw new DbMakerConfigException("totalHeaderSize must be times of 20");
        }
        this.totalHeaderSize = totalHeaderSize;
        // each index partition save 2048 index
        this.indexPartitionSize = IndexBlock.getIndexBlockLength() * 2048;
    }

    public DbConfig() throws DbMakerConfigException {
        /**
         * default value can support 2048 * 2048 records
         */
        this(20 * 2048);
    }

    public int getTotalHeaderSize() {
        return totalHeaderSize;
    }

    public DbConfig setTotalHeaderSize(int totalHeaderSize) {
        this.totalHeaderSize = totalHeaderSize;
        return this;
    }

    public int getIndexPartitionSize() {
        return indexPartitionSize;
    }

    public DbConfig setIndexPartitionSize(int dataBlockSize) {
        this.indexPartitionSize = dataBlockSize;
        return this;
    }
}
