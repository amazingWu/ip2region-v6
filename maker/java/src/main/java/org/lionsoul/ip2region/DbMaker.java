package org.lionsoul.ip2region;

import org.apache.commons.cli.*;
import org.lionsoul.ip2region.constant.DbConstant;
import org.lionsoul.ip2region.entity.DataBlock;
import org.lionsoul.ip2region.entity.HeaderBlock;
import org.lionsoul.ip2region.entity.IndexBlock;
import org.lionsoul.ip2region.exception.DbMakerConfigException;
import org.lionsoul.ip2region.exception.DbMakerDataException;
import org.lionsoul.ip2region.utils.ByteUtil;
import sun.net.util.IPAddressUtil;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * fast ip db maker
 * <p>
 * db struct:
 * 1. header part
 * 1): super part:
 * +--------+---------+---------+---------+---------+
 * 1 bytes  |  4bytes | 4 bytes | 4 bytes | 4 bytes
 * +--------+---------+---------+---------+---------+
 * db type, db size, header block size, start index ptr, end index ptr
 * <p>
 * 2): b-tree index part
 * +------------------------+-----------+-----------+-----------+
 * | 20bytes (16 + 4)		| 20bytes	| 20bytes	| 20bytes	| ...
 * +------------------------+-----------+-----------+-----------+
 * start ip ptr  index ptr
 * <p>
 * 2. data part:
 * +------------+-----------------------+
 * | 2bytes		| dynamic length		|
 * +------------+-----------------------+
 * data length   xxxxx
 * <p>
 * 3. index part: (ip range)
 * +------------+-----------+---------------+
 * | 16bytes	| 16bytes	| 4bytes		|
 * +------------+-----------+---------------+
 * start ip 	  end ip	  3 byte data ptr & 1 byte data length
 *
 * @author chenxin619315@gmail.com
 */
public class DbMaker {
    /**
     * db config
     */
    private DbConfig dbConfig;

    /**
     * db type
     */
    private DbType dbType;

    /**
     * ip source file path
     */
    private File ipSrcFile;

    /**
     * buffer
     */
    private LinkedList<IndexBlock> indexPool;
    private LinkedList<HeaderBlock> headerPool;

    /**
     * region and data ptr mapping data
     */
    private HashMap<String, DataBlock> regionPtrPool = null;

    /**
     * construct method
     *
     * @param config
     * @param ipSrcFile tb source ip file
     * @throws DbMakerConfigException
     * @throws IOException
     */
    public DbMaker(
            DbConfig config,
            DbType dbType,
            String ipSrcFile) throws IOException {
        this.dbConfig = config;
        this.dbType = dbType;
        this.ipSrcFile = new File(ipSrcFile);
        this.regionPtrPool = new HashMap<String, DataBlock>();
        if (this.ipSrcFile.exists() == false) {
            throw new IOException("Error: Invalid file path " + ipSrcFile);
        }
    }

    /**
     * initialize the db file
     *
     * @param raf
     * @throws IOException
     */
    private void initDbFile(RandomAccessFile raf) throws IOException {
        //1. zero fill the header part
        raf.seek(0L);
        raf.write(new byte[DbConstant.SUPER_PART_LENGTH]);        //super block
        raf.write(new byte[dbConfig.getTotalHeaderSize()]);        //header block

        headerPool = new LinkedList<HeaderBlock>();
        indexPool = new LinkedList<IndexBlock>();
    }

    /**
     * make the Db file
     *
     * @param dbFile target output file path
     * @throws IOException
     */
    public void make(String dbFile) throws IOException {
        File file = new File(dbFile);
        if (file.exists()) {
            file.delete();
        }

        //alloc the header size
        BufferedReader reader = new BufferedReader(new FileReader(this.ipSrcFile));
        RandomAccessFile raf = new RandomAccessFile(dbFile, "rw");

        //init the db file
        initDbFile(raf);
        System.out.println("+-Db file initialized.");

        //analysis main loop
        System.out.println("+-Try to write the data blocks ... ");
        String line = null;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }
            if (line.charAt(0) == '#') {
                continue;
            }
            //1. get the start ip
            int sIdx = 0, eIdx = 0;
            if ((eIdx = line.indexOf('|', sIdx + 1)) == -1) {
                continue;
            }
            String startIp = line.substring(sIdx, eIdx);

            //2. get the end ip
            sIdx = eIdx + 1;
            if ((eIdx = line.indexOf('|', sIdx + 1)) == -1) {
                continue;
            }
            String endIp = line.substring(sIdx, eIdx);

            //3. get the region
            sIdx = eIdx + 1;
            String region = line.substring(sIdx);

            System.out.println("+-Try to process item " + line);
            addDataBlock(raf, startIp, endIp, region);
            System.out.println("|--[Ok]");
        }
        System.out.println("|--Data block flushed!");
        System.out.println("|--Data file pointer: " + raf.getFilePointer() + "\n");

        //write the index bytes
        System.out.println("+-Try to write index blocks ... ");

        //record the start block
        IndexBlock indexBlock = null;
        HeaderBlock hb = null;
        indexBlock = indexPool.getFirst();
        byte[] indexStartIp = indexBlock.getStartIp();
        long indexStartPtr = raf.getFilePointer(), indexEndPtr;
        headerPool.add(new HeaderBlock(indexStartIp, (int) (indexStartPtr)));

        int blockLength = IndexBlock.getIndexBlockLength();
        int counter = 0;
        int shotCounter = (dbConfig.getIndexPartitionSize() / blockLength) - 1; // num of index each index partition having
        Iterator<IndexBlock> indexIt = indexPool.iterator();
        while (indexIt.hasNext()) {
            indexBlock = indexIt.next();
            if (++counter >= shotCounter) {
                hb = new HeaderBlock(
                        indexBlock.getStartIp(),
                        (int) raf.getFilePointer()
                );

                headerPool.add(hb);
                counter = 0;
            }
            //write the buffer
            raf.write(indexBlock.getBytes());
        }

        // record the end block
        if (counter > 0) {
            indexBlock = indexPool.getLast();
            hb = new HeaderBlock(
                    indexBlock.getStartIp(),
                    ((int) raf.getFilePointer()) - IndexBlock.getIndexBlockLength()
            );
            headerPool.add(hb);
        }

        indexEndPtr = raf.getFilePointer();
        System.out.println("|--[Ok]");

        //write the super blocks
        System.out.println("+-Try to write the super blocks ... ");
        raf.seek(0L);    //reset the file pointer
        byte[] superBuffer = new byte[DbConstant.SUPER_PART_LENGTH];
        // set db type
        superBuffer[0] = dbType == DbType.IPV4 ? (byte) 0 : (byte) 1;
        ByteUtil.writeIntLong(superBuffer, DbConstant.FIRST_INDEX_PTR, indexStartPtr);
        ByteUtil.writeIntLong(superBuffer, DbConstant.HEADER_BLOCK_PTR, dbConfig.getTotalHeaderSize());
        ByteUtil.writeIntLong(superBuffer, DbConstant.END_INDEX_PTR, indexEndPtr - blockLength);
        raf.write(superBuffer);
        System.out.println("|--[Ok]");

        //write the header blocks
        System.out.println("+-Try to write the header blocks ... ");
        Iterator<HeaderBlock> headerIt = headerPool.iterator();
        while (headerIt.hasNext()) {
            HeaderBlock headerBlock = headerIt.next();
            raf.write(headerBlock.getBytes());
        }
        //write the copyright and the release timestamp info
        System.out.println("+-Try to write the copyright and release date info ... ");
        raf.seek(raf.length());
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        String copyright = "Created by wuqi3 at " + dateFormat.format(cal.getTime());
        //the unix timestamp
        raf.write((int) (cal.getTime().getTime() / 1000));
        raf.write(copyright.getBytes());
        System.out.println("|--[Ok]");
        // write file length
        byte[] fileSize = new byte[4];
        ByteUtil.writeIntLong(fileSize, 0, raf.length());
        raf.seek(DbConstant.FILE_SIZE_PTR);
        raf.write(fileSize);
        reader.close();
        raf.close();
    }

    /**
     * internal method to add a new data block record
     *
     * @param raf
     * @param startIp
     * @param endIp
     * @param region  data
     */
    private void addDataBlock(
            RandomAccessFile raf,
            String startIp, String endIp, String region) {
        try {
            byte[] data = region.getBytes("UTF-8");
            int dataPtr = 0;

            //check region ptr pool first
            if (regionPtrPool.containsKey(region)) {
                DataBlock dataBlock = regionPtrPool.get(region);
                dataPtr = dataBlock.getDataPtr();
                System.out.println("dataPtr: " + dataPtr + ", region: " + region);
            } else {
                dataPtr = (int) raf.getFilePointer();
                raf.write(data);
                regionPtrPool.put(region, new DataBlock(region, dataPtr));
            }

            byte[] startIpBytes = dbType == DbType.IPV4 ? IPAddressUtil.textToNumericFormatV4(startIp) : IPAddressUtil.textToNumericFormatV6(startIp);
            byte[] endIpBytes = dbType == DbType.IPV4 ? IPAddressUtil.textToNumericFormatV4(endIp) : IPAddressUtil.textToNumericFormatV6(endIp);
            if (startIpBytes == null || endIpBytes == null) {
                throw new DbMakerDataException(String.format("can not get bytes of startIp[%s] or endIp[%s] with dbType[%s]", startIp, endIp, dbType));
            }
            //add the data index blocks
            IndexBlock ib = new IndexBlock(
                    startIpBytes,
                    endIpBytes,
                    dataPtr,
                    data.length
            );
            indexPool.add(ib);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public DbConfig getDbConfig() {
        return dbConfig;
    }

    public DbMaker setDbConfig(DbConfig dbConfig) {
        this.dbConfig = dbConfig;
        return this;
    }

    public File getIpSrcFile() {
        return ipSrcFile;
    }

    public DbMaker setIpSrcFile(File ipSrcFile) {
        this.ipSrcFile = ipSrcFile;
        return this;
    }

    /**
     * make this directly a runnable application
     * interface to make the database file
     */
    public static void main(String args[]) {
        String dstDir = "./data/";
        Options options = new Options();
        // help
        Option option = new Option("h", "help", false, "help");
        options.addOption(option);

        option = new Option("s", "source", true, "source text file path");
        options.addOption(option);

        option = new Option("f", "fileName", true, "result file name");
        options.addOption(option);

        option = new Option("t", "type", true, "ipv4 | ipv6");
        options.addOption(option);

        try {
            CommandLineParser parser = new GnuParser();
            CommandLine commandLine = parser.parse(options, args);
            if (commandLine.getOptions().length == 0 || commandLine.hasOption('h')) {
                new HelpFormatter().printHelp("ip2region-maker", options, true);
                return;
            }
            if (!commandLine.hasOption("s")) {
                throw new Exception("source must be set");
            }
            if (!commandLine.hasOption("f")) {
                throw new Exception("fileName must be set");
            }
            if (!commandLine.hasOption("t")) {
                throw new Exception("type must be set");
            }
            DbType dbType;
            if ("ipv4".equals(commandLine.getOptionValue("t"))) {
                dbType = DbType.IPV4;
            } else if ("ipv4".equals(commandLine.getOptionValue("t"))) {
                dbType = DbType.IPV6;
            } else {
                throw new Exception("type is invalid");
            }

            DbConfig config = new DbConfig();
            DbMaker dbMaker = new DbMaker(config, dbType, commandLine.getOptionValue("s"));
            dbMaker.make(dstDir + commandLine.getOptionValue("f"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}