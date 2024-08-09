/*
 * Copyright (c) 2024 Logan Bateman.
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digital.core.io.zenoh;

import de.neemann.digital.core.*;
import de.neemann.digital.core.element.Element;
import de.neemann.digital.core.element.ElementAttributes;
import de.neemann.digital.core.element.ElementTypeDescription;
import de.neemann.digital.core.element.Keys;
import de.neemann.digital.core.io.telnet.ServerHolder;
import de.neemann.digital.draw.elements.PinException;
import de.neemann.digital.lang.Lang;
import io.zenoh.Session;
import io.zenoh.exceptions.KeyExprException;
import io.zenoh.exceptions.ZenohException;
import io.zenoh.keyexpr.KeyExpr;
import io.zenoh.prelude.Encoding;
import io.zenoh.sample.Sample;
import io.zenoh.subscriber.Subscriber;

import java.io.IOException;
import java.nio.ByteBuffer;

import static de.neemann.digital.core.element.PinInfo.input;
import static de.neemann.digital.core.element.PinInfo.output;

/**
 * The ZenohSubscriberSync node
 */
public class ZenohSyncSubscriber extends Node implements Element {

    /**
     * The Zenoh Synchronous Subscriber description
     */
    public static final ElementTypeDescription DESCRIPTION = new ElementTypeDescription(ZenohSyncSubscriber.class,
            input("C").setClock())
            .addAttribute(Keys.BITS)
            .addAttribute(Keys.ROTATE)
            .addAttribute(Keys.LABEL)
            .addAttribute(Keys.ZENOH_KEYEXPR);

    private ObservableValue dataOut;
    private ObservableValue clockValue;
    private boolean lastClock;
    private final int[] bits;
    private KeyExpr zenohKeyExpr;
    private final String zenohKeyExprStr;
    private Model model;
    private RingBuffer ringBuffer;

    private Subscriber subscriber;

    /**
     * Creates a new instance
     *
     * @param attributes The components attributes
     */
    public ZenohSyncSubscriber(ElementAttributes attributes) {
        bits = new int[] { attributes.getBits() };
        dataOut = new ObservableValue("out", bits[0]).setPinDescription(DESCRIPTION);
        zenohKeyExprStr = attributes.get(Keys.ZENOH_KEYEXPR);
    }

    @Override
    public void setInputs(ObservableValues inputs) throws NodeException {
        clockValue = inputs.get(0).checkBits(1, this, 0).addObserverToValue(this);
    }

    @Override
    public void readInputs() throws NodeException {
        boolean clock = clockValue.getBool();
        // readEnable = readEnableValue.getBool();
        if (clock & !lastClock) {
            // if (writeEnable.getBool())
            // server.send((int) dataIn.getValue());
            // ringBuffer.delete();
        }
        lastClock = clock;
    }

    @Override
    public void writeOutputs() throws NodeException {
        boolean clock = clockValue.getBool();
        if (clock) return;
        if (ringBuffer.hasData()) {
            dataOut.setValue(ringBuffer.peek());
            ringBuffer.delete();
        } else {
            dataOut.setToHighZ();
        }
    }

    @Override
    public ObservableValues getOutputs() {
        return dataOut.asList();
    }

    public void onSample(Sample sample) {
        byte[] payload = sample.getValue().getPayload();

        ByteBuffer buffer = ByteBuffer.wrap(payload);

        final long value;
        if (payload.length == 8) {
            value = buffer.getLong();
        } else if (payload.length == 4) {
            value = buffer.getInt();
        } else {
            throw new RuntimeException("Payload length is not 4 or 8");
        }
        System.out.println(
                "Subscriber: " + sample.getKeyExpr().toString() + ": hex " + Long.toHexString(value) + " dec " + value);

        // model.modify(() -> dataOut.setValue(value));
        ringBuffer.put(value);

    }

    @Override
    public void init(Model model) throws NodeException {
        ringBuffer = new RingBuffer(64);
        Session session = SessionHolder.INSTANCE.getSession();
        this.model = model;

        try {
            zenohKeyExpr = KeyExpr.tryFrom(this.zenohKeyExprStr);
            subscriber = session.declareSubscriber(this.zenohKeyExpr)
                    .with(sample -> this.onSample(sample)).res();
            subscriber.getReceiver();

            session.get(this.zenohKeyExpr).res();
        } catch (ZenohException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void cleanup(Model model) {
        System.out.println("Cleaning up ZenohSubscriberSync");
        subscriber.close();
        ringBuffer = null;
    }

}
