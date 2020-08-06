package org.lionsoul.ip2region.exception;

/**
 * ip format exception
 *
 * @author wuqi3@corp.netease.com 2020-08-06 10:41
 * @since
 */
public class IpFormatException extends Exception {
    public IpFormatException() {
    }

    public IpFormatException(String message) {
        super(message);
    }

    public IpFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public IpFormatException(Throwable cause) {
        super(cause);
    }

    public IpFormatException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
