package org.lionsoul.ip2region.utils;

/**
 * util class
 *
 * @author chenxin
 */
public class ByteUtil {
    /**
     * write specfield bytes to a byte array start from offset
     *
     * @param b
     * @param offset
     * @param v
     * @param bytes
     */
    public static void write(byte[] b, int offset, long v, int bytes) {
        for (int i = 0; i < bytes; i++) {
            b[offset++] = (byte) ((v >>> (8 * i)) & 0xFF);
        }
    }


    /**
     * write a int to a byte array
     *
     * @param b
     * @param offset
     * @param v
     */
    public static void writeIntLong(byte[] b, int offset, long v) {
        b[offset++] = (byte) ((v >> 0) & 0xFF);
        b[offset++] = (byte) ((v >> 8) & 0xFF);
        b[offset++] = (byte) ((v >> 16) & 0xFF);
        b[offset] = (byte) ((v >> 24) & 0xFF);
    }

    /**
     * get a int from a byte array start from the specifiled offset
     *
     * @param b
     * @param offset
     */
    public static long getIntLong(byte[] b, int offset) {
        return (
                ((b[offset++] & 0x000000FFL)) |
                        ((b[offset++] << 8) & 0x0000FF00L) |
                        ((b[offset++] << 16) & 0x00FF0000L) |
                        ((b[offset] << 24) & 0xFF000000L)
        );
    }

    /**
     * get a int from a byte array start from the specifield offset
     *
     * @param b
     * @param offset
     */
    public static int getInt3(byte[] b, int offset) {
        return (
                (b[offset++] & 0x000000FF) |
                        (b[offset++] & 0x0000FF00) |
                        (b[offset] & 0x00FF0000)
        );
    }

    public static int getInt2(byte[] b, int offset) {
        return (
                (b[offset++] & 0x000000FF) |
                        (b[offset] & 0x0000FF00)
        );
    }

    public static int getInt1(byte[] b, int offset) {
        return (
                (b[offset] & 0x000000FF)
        );
    }
}
