package org.lionsoul.ip2region;

import org.junit.Test;
import org.lionsoul.ip2region.exception.IpFormatException;

import java.io.*;

/**
 * data check class
 *
 * @author
 **/

public class TestSearchAll {

    @Test
    public void testIpv4Db() {
        try {
            DbSearcher _searcher = new DbSearcher("../../data/ipv4.db", QueryType.MEMORY);
            BufferedReader bfr = new BufferedReader(new FileReader("../../data/ipv4.merge.txt"));
            BufferedWriter bwr = new BufferedWriter(new FileWriter("../../data/error_ipv4_log.txt", true));
            execute(_searcher, bwr, bfr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testIpv6Db() {
        try {
            DbSearcher _searcher = new DbSearcher("../../data/ipv6.db", QueryType.MEMORY);
            BufferedReader bfr = new BufferedReader(new FileReader("../../data/ipv6.merge.txt"));
            BufferedWriter bwr = new BufferedWriter(new FileWriter("../../data/error_ipv6_log.txt", true));
            execute(_searcher, bwr, bfr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void execute(DbSearcher searcher, BufferedWriter bwr, BufferedReader bfr) throws IOException {
        int errCount = 0;
        int lineCount = 0;
        String str = null;
        while ((str = bfr.readLine()) != null) {
            StringBuffer line = new StringBuffer(str);
            //get first ip
            int first_idx = line.indexOf("|");
            String first_ip = line.substring(0, first_idx);

            line = new StringBuffer(line.substring(first_idx + 1));

            //get second ip
            int second_idx = line.indexOf("|");
            String second_ip = line.substring(0, second_idx);

            //get addr
            String source_region = line.substring(second_idx + 1);

            //search from DbSearcher
            System.out.println("+---Start, start to search");
            System.out.println("+---[Info]: Source region = " + source_region);

            System.out.println("+---[Info]: Step1, search for first IP: " + first_ip);
            String fdata = null;
            try {
                fdata = searcher.search(first_ip);
            } catch (IpFormatException e) {
                e.printStackTrace();
            }
            if (!source_region.equalsIgnoreCase(fdata)) {
                System.out.println("[Error]: Search first IP failed, DB region = " + fdata);
                bwr.write("[Source]: Region: " + fdata);
                bwr.newLine();
                bwr.write("[Source]: First Ip: " + first_ip);
                bwr.newLine();
                bwr.write("[DB]: Region: " + fdata);
                bwr.newLine();
                bwr.flush();
                errCount++;
            }

            System.out.println("+---[Info]: Step2, search for second IP: " + second_ip);
            String sdata = null;
            try {
                sdata = searcher.search(second_ip);
            } catch (IpFormatException e) {
                e.printStackTrace();
            }
            if (!source_region.equalsIgnoreCase(sdata)) {
                System.out.println("[Error]: Search second IP failed, DB region = " + sdata);
                bwr.write("[Source]: Region: " + sdata);
                bwr.newLine();
                bwr.write("[Source]: First Ip: " + second_ip);
                bwr.newLine();
                bwr.write("[DB]: Region: " + sdata);
                bwr.newLine();
                bwr.flush();
                errCount++;
            }

            lineCount++;
        }

        bwr.close();
        bfr.close();
        System.out.println("+---Done, search complished");
        System.out.println("+---Statistics, Error count = " + errCount
                + ", Total line = " + lineCount
                + ", Fail ratio = " + ((float) (errCount / lineCount)) * 100 + "%");
    }
}
