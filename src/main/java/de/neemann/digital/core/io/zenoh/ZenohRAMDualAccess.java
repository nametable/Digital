/*
 * Copyright (c) 2024 Logan Bateman.
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */

package de.neemann.digital.core.io.zenoh;

import static de.neemann.digital.core.element.PinInfo.input;
import static de.neemann.digital.core.element.PinInfo.output;

import de.neemann.digital.core.Model;
import de.neemann.digital.core.element.ElementAttributes;
import de.neemann.digital.core.element.ElementTypeDescription;
import de.neemann.digital.core.element.Keys;
import de.neemann.digital.core.io.zenoh.ram_messages.GetQueryMessage;
import de.neemann.digital.core.io.zenoh.ram_messages.MemoryRangeMessage;
import de.neemann.digital.core.memory.RAMDualAccess;
import io.zenoh.Session;
import io.zenoh.exceptions.ZenohException;
import io.zenoh.keyexpr.KeyExpr;
import io.zenoh.prelude.Encoding;
import io.zenoh.publication.Publisher;
import io.zenoh.queryable.Queryable;
import io.zenoh.subscriber.Subscriber;

import java.nio.ByteBuffer;

public class ZenohRAMDualAccess extends RAMDualAccess {

    private final String baseZenohKeyExpr;
    private int bytesPerWord;

    private Publisher changePublisher;
    private Subscriber setSubscriber;
    private Queryable getQueryable;
    private Queryable infoQueryable;

    public ZenohRAMDualAccess(ElementAttributes attributes) {
        super(attributes);
        baseZenohKeyExpr = attributes.get(Keys.ZENOH_KEYEXPR);
        bytesPerWord = bytesPerWord(this.bits);
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

    /**
     * The RAMs {@link ElementTypeDescription}
     */
    public static final ElementTypeDescription DESCRIPTION = new ElementTypeDescription(ZenohRAMDualAccess.class,
            input("str"),
            input("C").setClock(),
            input("ld"),
            input("1A"),
            input("1Din"),
            input("2A"))
            .addAttribute(Keys.ROTATE)
            .addAttribute(Keys.BITS)
            .addAttribute(Keys.ADDR_BITS)
            .addAttribute(Keys.IS_PROGRAM_MEMORY)
            .addAttribute(Keys.LABEL)
            .addAttribute(Keys.ZENOH_KEYEXPR);

    @Override
    protected void writeToMemory(int addr, long data) {
        long prevData = memory.getData()[addr];
        if (prevData == data) {
            return;
        }
        super.writeToMemory(addr, data);

        MemoryRangeMessage message = new MemoryRangeMessage(bytesPerWord, addr, new long[] { data });

        // System.out.println("writeToMemory called!");
        changePublisher.put(
                new io.zenoh.value.Value(message.toByteBuffer().array(), new Encoding(Encoding.ID.APPLICATION_OCTET_STREAM, null)))
                .res();
    }

    @Override
    public void init(Model model) {

        Session session = SessionHolder.INSTANCE.getSession();
        try {
            changePublisher = session.declarePublisher(KeyExpr.tryFrom(this.baseZenohKeyExpr + "/changes")).res();
            setSubscriber = session.declareSubscriber(KeyExpr.tryFrom(this.baseZenohKeyExpr + "/set"))
                    .with(sample -> {
                        byte[] payload = sample.getValue().getPayload();
                        ByteBuffer buffer = ByteBuffer.wrap(payload);
                        MemoryRangeMessage message = MemoryRangeMessage.fromByteBuffer(buffer, bytesPerWord);
                        System.out.println(
                                "DualAccessRAM - /set called with address: " + message.address + " and length: " + message.data.length);
                        for (int i = 0; i < message.data.length; i++) {
                            this.memory.setData(message.address + i, message.data[i]);
                        }

                        // if output1 is enabled and the address is in the range of the message, update
                        // out1
                        if (ld1In.getValue() != 0 && addr1In.getValue() >= message.address
                                && addr1In.getValue() < message.address + message.data.length) {
                            model.modify(() -> out1.setValue(this.memory.getDataWord((int) addr1In.getValue())));
                        }
                        // if output2 address is in the range of the message, update out2
                        if (addr2In.getValue() >= message.address
                                && addr2In.getValue() < message.address + message.data.length) {
                            model.modify(() -> out2.setValue(this.memory.getDataWord((int) addr2In.getValue())));
                        }

                        changePublisher.put(sample.getValue()).res();
                    }).res();
            getQueryable = session.declareQueryable(KeyExpr.tryFrom(this.baseZenohKeyExpr + "/get")).with(query -> {
                System.out.println("Zenoh Dual Access RAM - received /get query: " + query);
                // - 4 bytes of address
                // - 4 bytes of length
                byte[] payload = query.getValue().getPayload();
                ByteBuffer buffer = ByteBuffer.wrap(payload);
                GetQueryMessage getMessage = GetQueryMessage.fromByteBuffer(buffer);
                System.out.println(
                        "/get called with address: " + getMessage.address + " and length: " + getMessage.length);

                // ByteBuffer replyBuffer = ByteBuffer.allocate(getMessage.length *
                // bytesPerWord);
                MemoryRangeMessage replyMessage = new MemoryRangeMessage(bytesPerWord, getMessage.address,
                        new long[getMessage.length]);
                for (int i = 0; i < getMessage.length; i++) {
                    replyMessage.data[i] = this.memory.getDataWord(getMessage.address + i);
                }

                try {
                    query.reply(query.getKeyExpr())
                            .success(new io.zenoh.value.Value(replyMessage.toByteBuffer().array(),
                            new Encoding(Encoding.ID.APPLICATION_OCTET_STREAM, null)))
                            .res();
                } catch (ZenohException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }).res();

            infoQueryable = session.declareQueryable(KeyExpr.tryFrom(this.baseZenohKeyExpr + "/info")).with(query -> {
                System.out.println("Zenoh Dual Access RAM - received /info query: " + query);
                try {
                    ByteBuffer buffer = ByteBuffer.allocate(8);
                    buffer.putInt(this.size);
                    buffer.putInt(this.bits);
                    query.reply(query.getKeyExpr())
                            .success(new io.zenoh.value.Value(buffer.array(), new Encoding(Encoding.ID.APPLICATION_OCTET_STREAM, null)))
                            .res();
                } catch (ZenohException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }).res();
        } catch (ZenohException e) {
            e.printStackTrace();
            // throw new NodeException(e);
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
