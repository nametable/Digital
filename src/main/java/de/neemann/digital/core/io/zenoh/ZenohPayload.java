package de.neemann.digital.core.io.zenoh;

import io.zenoh.bytes.Encoding;
import io.zenoh.bytes.ZBytes;
import io.zenoh.pubsub.PutOptions;
import io.zenoh.query.ReplyOptions;

final class ZenohPayload {

    private ZenohPayload() {
    }

    static ZBytes bytes(byte[] payload) {
        return ZBytes.from(payload);
    }

    static PutOptions putOptions() {
        PutOptions options = new PutOptions();
        options.setEncoding(Encoding.APPLICATION_OCTET_STREAM);
        return options;
    }

    static ReplyOptions replyOptions() {
        ReplyOptions options = new ReplyOptions();
        options.setEncoding(Encoding.APPLICATION_OCTET_STREAM);
        return options;
    }
}
