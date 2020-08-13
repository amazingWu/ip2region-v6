package org.lionsoul.ip2region;

import org.lionsoul.ip2region.exception.DbMakerConfigException;

import java.io.IOException;

/**
 * project test script
 *
 * @author henxin619315@gmail.com
 */
public class TestMaker {
    public static void main(String[] argv) {
        try {
            DbConfig config = new DbConfig();
            DbMaker dbMaker = new DbMaker(
                    config,
                    DbType.IPV6,
                    "data/ipv6_merge.txt"
            );

            dbMaker.make("data/ipv6.db");
        } catch (DbMakerConfigException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            DbConfig config = new DbConfig();
            DbMaker dbMaker = new DbMaker(
                    config,
                    DbType.IPV4,
                    "data/ipv4.merge.txt"
            );

            dbMaker.make("data/ipv4.db");
        } catch (DbMakerConfigException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
