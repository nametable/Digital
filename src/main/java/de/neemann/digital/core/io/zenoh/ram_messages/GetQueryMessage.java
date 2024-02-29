package de.neemann.digital.core.io.zenoh.ram_messages;

import java.nio.ByteBuffer;

public class GetQueryMessage {
    public int address;
    public int length;

    public GetQueryMessage(int address, int length) {
        this.address = address;
        this.length = length;
    }

    public static GetQueryMessage fromByteBuffer(ByteBuffer buffer) {
        return new GetQueryMessage(buffer.getInt(), buffer.getInt());
    }
}
