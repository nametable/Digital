package de.neemann.digital.core.io.zenoh.ram_messages;

import java.nio.ByteBuffer;

public class MemoryRangeMessage {
    private int bytesPerWord;
    public int address;
    // public int length;
    public long[] data;

    public MemoryRangeMessage(int bytesPerWord, int address, long[] data) {
        this.bytesPerWord = bytesPerWord;
        this.address = address;
        // this.length = length;
        this.data = data;
    }

    public static MemoryRangeMessage fromByteBuffer(ByteBuffer buffer, int bytesPerWord) {
        int address = buffer.getInt();
        int length = buffer.getInt();
        long[] data = new long[length];
        switch (bytesPerWord) {
            case 1:
                for (int i = 0; i < length; i++) {
                    data[i] = buffer.get();
                }
                break;
            case 2:
                for (int i = 0; i < length; i++) {
                    data[i] = buffer.getShort();
                }
                break;
            case 4:
                for (int i = 0; i < length; i++) {
                    data[i] = buffer.getInt();
                }
                break;
            case 8:
                for (int i = 0; i < length; i++) {
                    data[i] = buffer.getLong();
                }
                break;
        }
        return new MemoryRangeMessage(length, address, data);
    }

    public ByteBuffer toByteBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(8 + data.length * bytesPerWord);
        buffer.putInt(address);
        buffer.putInt(data.length);
        switch (bytesPerWord) {
            case 1:
                for (long l : data) {
                    buffer.put((byte) l);
                }
                break;
            case 2:
                for (long l : data) {
                    buffer.putShort((short) l);
                }
                break;
            case 4:
                for (long l : data) {
                    buffer.putInt((int) l);
                }
                break;
            case 8:
                for (long l : data) {
                    buffer.putLong(l);
                }
                break;
        }

        return buffer;
    }
}
