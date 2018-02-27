package com.animaconnected.bluetooth.utils;

public class ByteUtils {

    /**
     * Decode a little endian unsigned 16-bit integer
     *
     * @param data   byte array to decode the data from
     * @param offset the offset where the data starts
     * @return decoded integer value
     */
    public static int decodeUInt16LE(final byte[] data, final int offset) {
        return (data[offset] & 0xff) + ((data[offset + 1] & 0xff) << 8);
    }

    /**
     * Decode a little endian unsigned 16-bit integer
     *
     * @param data   byte array to decode the data from (starts at first byte)
     * @return decoded integer value
     */
    public static int decodeUInt16LE(final byte[] data) {
        return decodeUInt16LE(data, 0);
    }

    /**
     * Encode a byte array with a little endian unsigned 16-bit integer
     *
     * @param value the numerical value of the integer to encode
     * @return byte array of the result
     */
    public static byte[] encodeUInt16LE(final int value) {
        byte[] data = new byte[2];
        data[0] = (byte) (value & 0xff);
        data[1] = (byte) ((value >> 8) & 0xff);
        return data;
    }

    /**
     * Encode a byte array with a little endian unsigned 16-bit integer into byte array
     *
     * @param data   byte array to place data into
     * @param offset the offset where the first byte should be placed
     * @param value  the numerical value of the integer to encode
     */
    public static void encodeUInt16LE(final byte[] data, final int offset, final int value) {
        data[offset + 0] = (byte) (value & 0xff);
        data[offset + 1] = (byte) ((value >> 8) & 0xff);
    }

    public static String bytesToHex(final byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (Byte b : data) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    /**
     * Decode arbitrary bits in a byte array as an unsigned integer
     * @param bytes the data to decode from
     * @param offset the zero based offset in bits of where to start
     * @param length the number of bits long the int is
     * @return the decoded integer
     */
    public static long decodeBitsAsUInt(final byte[] bytes, final int offset, final int length) {
        long result = 0;
        for (int i = 0; i < length; i++) {
            long bitValue = getBitFromBytes(bytes, offset + i);
            result |= bitValue << i;
        }
        return result;
    }

    /**
     * Returns the value (0 or 1) of an individual bit in a byte array
     */
    public static int getBitFromBytes(final byte[] bytes, final int position) {
        final int byteIndex = position / 8;
        final int bitOffset = position % 8;
        return (bytes[byteIndex] & (1 << bitOffset)) >> bitOffset;
    }

    /**
     * Returns the number of bits in an unsigned integer
     */
    public static int countBits(long number) {
        int count = 0;
        while (number > 0) {
            if ((number & 1) == 1) count++;
            number >>= 1;
        }
        return count;
    }

    public static int encodeDaysOfWeek(int daysOfWeek, final boolean isOneShot) {
        int result = daysOfWeek;
        result <<= 1;
        if (isOneShot) {
            result |= 1;
        }

        return result;
    }
}
