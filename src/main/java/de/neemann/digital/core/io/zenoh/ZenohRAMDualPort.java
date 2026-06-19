/*
 * Copyright (c) 2024 Logan Bateman.
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */

package de.neemann.digital.core.io.zenoh;

import de.neemann.digital.core.element.ElementAttributes;
import de.neemann.digital.core.element.ElementTypeDescription;
import de.neemann.digital.core.element.ImmutableList;
import de.neemann.digital.core.element.Keys;
import de.neemann.digital.core.io.zenoh.ram_messages.GetQueryMessage;
import de.neemann.digital.core.io.zenoh.ram_messages.MemoryRangeMessage;
import de.neemann.digital.core.memory.RAMDualPort;
import io.zenoh.Session;
import io.zenoh.exceptions.ZError;
import io.zenoh.keyexpr.KeyExpr;
import io.zenoh.pubsub.Publisher;
import io.zenoh.query.Query;
import io.zenoh.query.Queryable;
import io.zenoh.sample.Sample;
import io.zenoh.pubsub.Subscriber;

import static de.neemann.digital.core.element.PinInfo.input;

import java.nio.ByteBuffer;

import de.neemann.digital.core.Model;
import de.neemann.digital.core.NodeException;

public class ZenohRAMDualPort extends RAMDualPort {

    public static final ElementTypeDescription DESCRIPTION = new ElementTypeDescription(ZenohRAMDualPort.class,
            input("A"),
            input("Din"),
            input("str"),
            input("C").setClock(),
            input("ld"))
            .addAttribute(Keys.ROTATE)
            .addAttribute(Keys.BITS)
            .addAttribute(Keys.ADDR_BITS)
            .addAttribute(Keys.INT_FORMAT)
            .addAttribute(Keys.IS_PROGRAM_MEMORY)
            .addAttribute(Keys.LABEL)
            .addAttribute(Keys.ZENOH_KEYEXPR);

    private final String baseZenohKeyExprStr;
    
    private Publisher changePublisher;
    private Subscriber setSubscriber;
    private Queryable getQueryable;
    private Queryable infoQueryable;
    private int bytesPerWord;
    private Model model;
    
    public ZenohRAMDualPort(ElementAttributes attributes) {
        super(attributes);
        baseZenohKeyExprStr = attributes.get(Keys.ZENOH_KEYEXPR);
        bytesPerWord = bytesPerWord(this.bits);
    }

    public void onSetMemoryMessage(Sample sample) {
        byte[] payload = sample.getPayload().toBytes();
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        MemoryRangeMessage message = MemoryRangeMessage.fromByteBuffer(buffer, bytesPerWord);
        System.out.println("/set called with address: " + message.address + " and length: " + message.data.length);
        for (int i = 0; i < message.data.length; i++) {
            this.memory.setData(message.address + i, message.data[i]);
        }

        // if output is enabled and the address is in the range of the message, update the output
        if (ldIn.getValue() != 0 && addrIn.getValue() >= message.address && addrIn.getValue() < message.address + message.data.length) {
            model.modify(() -> output.setValue(this.memory.getDataWord((int)addrIn.getValue())));
        }

        try {
            changePublisher.put(sample.getPayload(), ZenohPayload.putOptions());
        } catch (ZError e) {
            e.printStackTrace();
        }

    }

    public void onGetQuery(Query query) {
        // - 4 bytes of address
        // - 4 bytes of length
        byte[] payload = query.getPayload().toBytes();
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        GetQueryMessage getMessage = GetQueryMessage.fromByteBuffer(buffer);
        System.out.println("/get called with address: " + getMessage.address + " and length: " + getMessage.length);

        // ByteBuffer replyBuffer = ByteBuffer.allocate(getMessage.length * bytesPerWord);
        MemoryRangeMessage replyMessage = new MemoryRangeMessage(bytesPerWord, getMessage.address, new long[getMessage.length]);
        for (int i = 0; i < getMessage.length; i++) {
            replyMessage.data[i] = this.memory.getDataWord(getMessage.address + i);
        }

        try {
            query.reply(query.getKeyExpr(), ZenohPayload.bytes(replyMessage.toByteBuffer().array()), ZenohPayload.replyOptions());
        } catch (ZError e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // number of bytes needed to store a word
    private int bytesPerWord(int bits) {
        int bytes = (bits - 1) / 8 + 1;
        int roundedBytes = 1;
        while (roundedBytes < bytes) {
            roundedBytes *= 2;
        }
        return roundedBytes;
    }

    @Override
    protected void writeToMemory(int addr, long data) {
        long prevData = memory.getData()[addr];
        if (prevData == data) {
            return;
        }
        super.writeToMemory(addr, data);

        MemoryRangeMessage message = new MemoryRangeMessage(bytesPerWord, addr, new long[] { data });

        System.out.println("writeToMemory called!");
        try {
            changePublisher.put(ZenohPayload.bytes(message.toByteBuffer().array()), ZenohPayload.putOptions());
        } catch (ZError e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init(Model model) throws NodeException {

        this.model = model;
        Session session = SessionHolder.INSTANCE.getSession();
        try {
            changePublisher = session.declarePublisher(KeyExpr.tryFrom(this.baseZenohKeyExprStr + "/changes"));
            setSubscriber = session.declareSubscriber(KeyExpr.tryFrom(this.baseZenohKeyExprStr + "/set"),
                    sample -> this.onSetMemoryMessage(sample));
            getQueryable = session.declareQueryable(KeyExpr.tryFrom(this.baseZenohKeyExprStr + "/get"), query -> onGetQuery(query));
            
            infoQueryable = session.declareQueryable(KeyExpr.tryFrom(this.baseZenohKeyExprStr + "/info"), query -> {
                System.out.println("Received query: " + query);
                try {
                    ByteBuffer buffer = ByteBuffer.allocate(8);
                    buffer.putInt(this.size);
                    buffer.putInt(this.bits);
                    query.reply(query.getKeyExpr(), ZenohPayload.bytes(buffer.array()), ZenohPayload.replyOptions());
                } catch (ZError e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });

            // send initial info
            try {
                ByteBuffer buffer = ByteBuffer.allocate(8);
                buffer.putInt(this.size);
                buffer.putInt(this.bits);
                session.put(KeyExpr.tryFrom(this.baseZenohKeyExprStr + "/info"), ZenohPayload.bytes(buffer.array()), ZenohPayload.putOptions());
            } catch (ZError e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (ZError e) {
            throw new NodeException(e.getMessage(), this, -1, new ImmutableList<>());
        }
    }

    @Override
    public void cleanup(Model model) {
        changePublisher.close();
        setSubscriber.close();
        getQueryable.close();
        infoQueryable.close();
    }

}
