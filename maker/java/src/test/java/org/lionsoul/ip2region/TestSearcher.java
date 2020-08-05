package org.lionsoul.ip2region;

import org.junit.Test;

import java.io.IOException;

/**
 * @author wuqi3@corp.netease.com 2020-08-04 16:25
 * @since
 */
public class TestSearcher {
    @Test
    public void ipv6MemoryQueryTest() {
        try {
            DbConfig config = new DbConfig();
            DbSearcher searcher = new DbSearcher(config, "../../data/ipv6.db", QueryType.BTREE);
            for (int i = 0; i < 10; i++) {
                String line = "2001:4:113:0:0:0:0:0";
                double sTime = System.nanoTime();
                String region = searcher.search(line);
                double cTime = (System.nanoTime() - sTime) / 1000000;
                System.out.printf("%s in %.5f millseconds\n", region, cTime);
            }
            searcher.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (DbMakerConfigException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void ipv4MemoryQueryTest() {
        try {
            DbConfig config = new DbConfig();
            DbSearcher searcher = new DbSearcher(config, "../../data/ipv4.db", QueryType.BTREE);
            String line = "1.0.0.0";
            double sTime = System.nanoTime();
            String dataBlock = searcher.search(line);
            double cTime = (System.nanoTime() - sTime) / 1000000;
            System.out.printf("%s in %.5f millseconds\n", dataBlock, cTime);
            searcher.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (DbMakerConfigException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
