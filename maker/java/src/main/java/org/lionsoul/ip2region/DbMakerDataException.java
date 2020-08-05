package org.lionsoul.ip2region;

/**
 * @author wuqi3@corp.netease.com 2020-08-04 19:09
 * @since
 */
public class DbMakerDataException extends RuntimeException {
    public DbMakerDataException() {
    }

    public DbMakerDataException(String message) {
        super(message);
    }

    public DbMakerDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public DbMakerDataException(Throwable cause) {
        super(cause);
    }

    public DbMakerDataException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
