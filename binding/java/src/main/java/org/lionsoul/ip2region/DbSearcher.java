package org.lionsoul.ip2region;

import org.lionsoul.ip2region.constant.DbConstant;
import org.lionsoul.ip2region.entity.DataBlock;
import org.lionsoul.ip2region.entity.IndexBlock;
import org.lionsoul.ip2region.exception.IpFormatException;
import org.lionsoul.ip2region.utils.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.net.util.IPAddressUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;


/**
 * @author wuqi3@corp.netease.com
 */
public class DbSearcher {
    private static final Logger logger = LoggerFactory.getLogger(DbSearcher.class);

    private DbType dbType;

    private int ipBytesLength;

    private QueryType queryType;

    private long totalHeaderBlockSize;

    /**
     * db file access handler
     */
    private RandomAccessFile raf = null;

    /**
     * for btree search only
     * header blocks buffer
     */
    private byte[][] HeaderSip = null;
    private int[] HeaderPtr = null;
    private int headerLength;

    /**
     * for memory and binary search
     * super blocks info
     */
    private long firstIndexPtr = 0;
    private long lastIndexPtr = 0;
    private int totalIndexBlocks = 0;

    /**
     * for memory search only
     * the original db binary string
     */
    private byte[] dbBinStr = null;

    /**
     * construct class
     *
     * @param dbFile
     * @throws FileNotFoundException
     */
    public DbSearcher(String dbFile, QueryType queryType) throws IOException {
        this.queryType = queryType;
        raf = new RandomAccessFile(dbFile, "r");
        switch (queryType) {
            case MEMORY:
                dbBinStr = new byte[(int) raf.length()];
                raf.seek(0L);
                raf.readFully(dbBinStr, 0, dbBinStr.length);
                raf.close();
                //initialize the global vars
                initMemoryOrBinaryModeParam(dbBinStr, dbBinStr.length);
                break;
            case BTREE:
                initBtreeModeParam(raf);
                break;
            case BINARY:
                raf.seek(0L);
                byte[] superBytes = new byte[DbConstant.SUPER_PART_LENGTH];
                raf.readFully(superBytes, 0, superBytes.length);
                //initialize the global vars
                initMemoryOrBinaryModeParam(superBytes, raf.length());
                break;
            default:
                break;
        }
    }

    /**
     * you can only use memory query when using this constructor
     * Thanks to the issue from Wendal at https://gitee.com/lionsoul/ip2region/issues/IILFL
     *
     * @param bytes
     */
    public DbSearcher(byte[] bytes) {
        this.dbBinStr = bytes;
        this.dbBinStr = bytes;
        initMemoryOrBinaryModeParam(bytes, bytes.length);
    }

    private void initMemoryOrBinaryModeParam(byte[] bytes, long fileSize) {
        dbType = (bytes[0] & 1) == 0 ? DbType.IPV4 : DbType.IPV6;
        totalHeaderBlockSize = ByteUtil.getIntLong(bytes, DbConstant.HEADER_BLOCK_PTR);
        ipBytesLength = dbType == DbType.IPV4 ? 4 : 16;
        long fileSizeInFile = ByteUtil.getIntLong(bytes, DbConstant.FILE_SIZE_PTR);
        if (fileSizeInFile != fileSize) {
            throw new RuntimeException(String.format("db file size error, excepted [%s], real [%s]", fileSizeInFile, fileSize));
        }
        firstIndexPtr = ByteUtil.getIntLong(bytes, DbConstant.FIRST_INDEX_PTR);
        lastIndexPtr = ByteUtil.getIntLong(bytes, DbConstant.END_INDEX_PTR);
        totalIndexBlocks = (int) ((lastIndexPtr - firstIndexPtr) / IndexBlock.getIndexBlockLength()) + 1;
    }

    private void initBtreeModeParam(RandomAccessFile raf) throws IOException {
        // set db type
        raf.seek(0);
        byte[] superBytes = new byte[DbConstant.SUPER_PART_LENGTH];
        raf.readFully(superBytes, 0, superBytes.length);
        dbType = (superBytes[0] & 1) == 0 ? DbType.IPV4 : DbType.IPV6;
        totalHeaderBlockSize = ByteUtil.getIntLong(superBytes, DbConstant.HEADER_BLOCK_PTR);
        ipBytesLength = dbType == DbType.IPV4 ? 4 : 16;
        long fileSizeInFile = ByteUtil.getIntLong(superBytes, DbConstant.FILE_SIZE_PTR);
        long realFileSize = raf.length();
        if (fileSizeInFile != realFileSize) {
            throw new RuntimeException(String.format("db file size error, excepted [%s], real [%s]", fileSizeInFile, realFileSize));
        }
        byte[] b = new byte[(int) totalHeaderBlockSize];
        // byte[] b = new byte[4096];
        raf.readFully(b, 0, b.length);

        int indexLength = 20;

        //fill the header, b.lenght / 20
        int len = b.length / indexLength, idx = 0;
        HeaderSip = new byte[len][16];
        HeaderPtr = new int[len];
        long dataPtr;
        for (int i = 0; i < b.length; i += indexLength) {
            dataPtr = ByteUtil.getIntLong(b, i + 16);
            if (dataPtr == 0) {
                break;
            }
            System.arraycopy(b, i, HeaderSip[idx], 0, 16);
            HeaderPtr[idx] = (int) dataPtr;
            idx++;
        }
        headerLength = idx;
    }


    /**
     * get the region through the ip address with memory or binary or btree search algorithm
     *
     * @param ip
     * @return DataBlock
     * @throws IOException       using binary search or btree search may occurs IOException
     * @throws IpFormatException throw IpFormatException when ip format is not right
     */
    public String search(String ip) throws IpFormatException, IOException {
        byte[] ipBytes = getIpBytes(ip);
        DataBlock dataBlock = null;
        switch (queryType) {
            case MEMORY:
                dataBlock = memorySearch(ipBytes);
                break;
            case BINARY:
                dataBlock = binarySearch(ipBytes);
                break;
            case BTREE:
                dataBlock = bTreeSearch(ipBytes);
                break;
            default:
                break;
        }
        if (dataBlock == null) {
            return null;
        } else {
            return dataBlock.getRegion();
        }

    }

    /**
     * get the region with a int ip address with memory binary search algorithm
     *
     * @param ip
     * @throws IOException
     */
    private DataBlock memorySearch(byte[] ip) throws UnsupportedEncodingException {
        int blockLen = IndexBlock.getIndexBlockLength();
        //search the index blocks to define the data
        int l = 0, h = totalIndexBlocks;
        byte[] sip = new byte[16], eip = new byte[16];
        long dataWrapperPtr = 0;
        while (l <= h) {
            int m = (l + h) >> 1;
            int p = (int) (firstIndexPtr + m * blockLen);
            System.arraycopy(dbBinStr, p, sip, 0, 16);
            if (compareBytes(ip, sip, ipBytesLength) < 0) {
                h = m - 1;
            } else {
                System.arraycopy(dbBinStr, p + 16, eip, 0, 16);
                if (compareBytes(ip, eip, ipBytesLength) > 0) {
                    l = m + 1;
                } else {
                    dataWrapperPtr = ByteUtil.getIntLong(dbBinStr, p + 32);
                    break;
                }
            }
        }

        //not matched
        if (dataWrapperPtr == 0) {
            return null;
        }

        //get the data
        int dataLen = (int) ((dataWrapperPtr >> 24) & 0xFF);
        int dataPtr = (int) ((dataWrapperPtr & 0x00FFFFFF));
        String region = null;
        region = new String(dbBinStr, dataPtr, dataLen, "UTF-8");
        return new DataBlock(region, dataPtr);
    }

    /**
     * get the region with a int ip address with b-tree algorithm
     *
     * @param ip
     * @throws IOException
     */
    private DataBlock bTreeSearch(byte[] ip) throws IOException {
        //1. define the index block with the binary search
        if (compareBytes(ip, HeaderSip[0], ipBytesLength) == 0) {
            return getByIndexPtr(HeaderPtr[0]);
        } else if (compareBytes(ip, HeaderSip[headerLength - 1], ipBytesLength) == 0) {
            return getByIndexPtr(HeaderPtr[headerLength - 1]);
        }

        int l = 0, h = headerLength, sptr = 0, eptr = 0;
        while (l <= h) {
            int m = (l + h) >> 1;
            // perfect matched, just return it
            if (compareBytes(ip, HeaderSip[m], ipBytesLength) == 0) {
                if (m > 0) {
                    sptr = HeaderPtr[m - 1];
                    eptr = HeaderPtr[m];
                } else {
                    sptr = HeaderPtr[m];
                    eptr = HeaderPtr[m + 1];
                }

                break;
            }

            //less then the middle value
            if (compareBytes(ip, HeaderSip[m], ipBytesLength) < 0) {
                if (m == 0) {
                    sptr = HeaderPtr[m];
                    eptr = HeaderPtr[m + 1];
                    break;
                } else if (compareBytes(ip, HeaderSip[m - 1], ipBytesLength) > 0) {
                    sptr = HeaderPtr[m - 1];
                    eptr = HeaderPtr[m];
                    break;
                }
                h = m - 1;
            } else {
                if (m == headerLength - 1) {
                    sptr = HeaderPtr[m - 1];
                    eptr = HeaderPtr[m];
                    break;
                } else if (compareBytes(ip, HeaderSip[m + 1], ipBytesLength) <= 0) {
                    sptr = HeaderPtr[m];
                    eptr = HeaderPtr[m + 1];
                    break;
                }
                l = m + 1;
            }
        }

        //match nothing just stop it
        if (sptr == 0) {
            return null;
        }
        //2. search the index blocks to define the data
        int blockLen = eptr - sptr, blen = IndexBlock.getIndexBlockLength();
        //include the right border block
        byte[] iBuffer = new byte[blockLen + blen];
        raf.seek(sptr);
        raf.readFully(iBuffer, 0, iBuffer.length);

        l = 0;
        h = blockLen / blen;
        byte[] sip = new byte[16], eip = new byte[16];
        long dataWrapperPtr = 0;
        while (l <= h) {
            int m = (l + h) >> 1;
            int p = m * blen;
            System.arraycopy(iBuffer, p, sip, 0, 16);
            if (compareBytes(ip, sip, ipBytesLength) < 0) {
                h = m - 1;
            } else {
                System.arraycopy(iBuffer, p + 16, eip, 0, 16);
                if (compareBytes(ip, eip, ipBytesLength) > 0) {
                    l = m + 1;
                } else {
                    dataWrapperPtr = ByteUtil.getIntLong(iBuffer, p + 32);
                    break;
                }
            }
        }

        //not matched
        if (dataWrapperPtr == 0) {
            return null;
        }

        //3. get the data
        int dataLen = (int) ((dataWrapperPtr >> 24) & 0xFF);
        int dataPtr = (int) ((dataWrapperPtr & 0x00FFFFFF));

        raf.seek(dataPtr);
        byte[] data = new byte[dataLen];
        raf.readFully(data, 0, data.length);
        String region = new String(data, "UTF-8");
        return new DataBlock(region, dataPtr);
    }

    /**
     * get the region with a int ip address with binary search algorithm
     *
     * @param ip
     * @throws IOException
     */
    private DataBlock binarySearch(byte[] ip) throws IOException {
        int blockLength = IndexBlock.getIndexBlockLength();
        //search the index blocks to define the data
        int l = 0, h = totalIndexBlocks;
        byte[] buffer = new byte[blockLength];
        byte[] sip = new byte[16], eip = new byte[16];
        long dataWrapperPtr = 0;
        while (l <= h) {
            int m = (l + h) >> 1;
            //set the file pointer
            raf.seek(firstIndexPtr + m * blockLength);
            raf.readFully(buffer, 0, buffer.length);
            System.arraycopy(buffer, 0, sip, 0, 16);
            if (compareBytes(ip, sip, ipBytesLength) < 0) {
                h = m - 1;
            } else {
                System.arraycopy(buffer, 16, eip, 0, 16);
                if (compareBytes(ip, eip, ipBytesLength) > 0) {
                    l = m + 1;
                } else {
                    dataWrapperPtr = ByteUtil.getIntLong(buffer, 32);
                    break;
                }
            }
        }

        //not matched
        if (dataWrapperPtr == 0) {
            return null;
        }

        //get the data
        int dataLen = (int) ((dataWrapperPtr >> 24) & 0xFF);
        int dataPtr = (int) ((dataWrapperPtr & 0x00FFFFFF));

        raf.seek(dataPtr);
        byte[] data = new byte[dataLen];
        raf.readFully(data, 0, data.length);
        String region = new String(data, "UTF-8");
        return new DataBlock(region, dataPtr);
    }


    /**
     * get by index ptr
     *
     * @param ptr
     * @throws IOException
     */
    private DataBlock getByIndexPtr(long ptr) throws IOException {
        raf.seek(ptr);
        byte[] buffer = new byte[36];
        raf.readFully(buffer, 0, buffer.length);
        long extra = ByteUtil.getIntLong(buffer, 32);

        int dataLen = (int) ((extra >> 24) & 0xFF);
        int dataPtr = (int) ((extra & 0x00FFFFFF));

        raf.seek(dataPtr);
        byte[] data = new byte[dataLen];
        raf.readFully(data, 0, data.length);
        String region = new String(data, "UTF-8");

        return new DataBlock(region, dataPtr);
    }

    /**
     * get db type
     *
     * @return
     */
    public DbType getDbType() {
        return dbType;
    }

    /**
     * get query type
     *
     * @return
     */
    public QueryType getQueryType() {
        return queryType;
    }

    /**
     * close the db
     *
     * @throws IOException
     */
    public void close() {
        try {
            //let gc do its work
            HeaderSip = null;
            HeaderPtr = null;
            dbBinStr = null;
            raf.close();
        } catch (Exception e) {
        }
    }

    private byte[] getIpBytes(String ip) throws IpFormatException {
        byte[] ipBytes;
        if (dbType == DbType.IPV4) {
            ipBytes = IPAddressUtil.textToNumericFormatV4(ip);
        } else {
            ipBytes = IPAddressUtil.textToNumericFormatV6(ip);
        }
        if (ipBytes == null) {
            throw new IpFormatException(String.format("ip [%s] format error for %s", ip, dbType));
        }
        return ipBytes;
    }

    /**
     * @param bytes1
     * @param bytes2
     * @param length 检查前多少位的byte
     * @return
     */
    private static int compareBytes(byte[] bytes1, byte[] bytes2, int length) {
        for (int i = 0; i < bytes1.length && i < bytes2.length && i < length; i++) {
            if (bytes1[i] * bytes2[i] > 0) {
                if (bytes1[i] < bytes2[i]) {
                    return -1;
                } else if (bytes1[i] > bytes2[i]) {
                    return 1;
                }
            } else if (bytes1[i] * bytes2[i] < 0) {
                // 异号时，负数的大
                if (bytes1[i] > 0) {
                    return -1;
                } else {
                    return 1;
                }
            } else if (bytes1[i] * bytes2[i] == 0 && bytes1[i] + bytes2[i] != 0) {
                // 0 最小
                if (bytes1[i] == 0) {
                    return -1;
                } else {
                    return 1;
                }
            }
        }
        if (bytes1.length >= length && bytes2.length >= length) {
            return 0;
        } else {
            if (bytes1.length > bytes2.length) {
                return 1;
            } else if (bytes1.length == bytes2.length) {
                return 0;
            } else {
                return -1;
            }
        }
    }
}

