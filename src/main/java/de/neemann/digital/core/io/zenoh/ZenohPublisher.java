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
import de.neemann.digital.draw.elements.PinException;
import io.zenoh.Session;
import io.zenoh.exceptions.ZenohException;
import io.zenoh.keyexpr.KeyExpr;
import io.zenoh.prelude.Encoding;
import io.zenoh.publication.Publisher;
import io.zenoh.queryable.Queryable;

import java.nio.ByteBuffer;

import static de.neemann.digital.core.element.PinInfo.input;

/**
 * The telnet node
 */
public class ZenohPublisher extends Node implements Element, ZenohDataSender {

    /**
     * The ZenohPublisher description
     */
    public static final ElementTypeDescription DESCRIPTION = new ElementTypeDescription(ZenohPublisher.class,
            input("in"))
            .addAttribute(Keys.BITS)
            .addAttribute(Keys.ROTATE)
            .addAttribute(Keys.LABEL)
            .addAttribute(Keys.ZENOH_KEYEXPR)
            .addAttribute(Keys.ZENOH_ENABLE_PUBLISHING)
            .addAttribute(Keys.ZENOH_ENABLE_QUERYING)
            .addAttribute(Keys.ZENOH_ENABLE_RATE_LIMIT);

    private ObservableValue dataIn;
    private final int[] bits;
    private final String zenohKeyExpr;
    private final boolean enableQuerying; // whether or not to create a queryable
    private final boolean enablePublishing; // whether or not to enable publishing
    private final boolean enableRateLimit;

    private long lastDataSent;

    private Publisher publisher;
    private Queryable queryable;

    /**
     * Creates a new instance
     *
     * @param attributes The components attributes
     */
    public ZenohPublisher(ElementAttributes attributes) {
        bits = new int[]{attributes.getBits()};
        zenohKeyExpr = attributes.get(Keys.ZENOH_KEYEXPR);
        enableQuerying = attributes.get(Keys.ZENOH_ENABLE_QUERYING);
        enablePublishing = attributes.get(Keys.ZENOH_ENABLE_PUBLISHING);
        enableRateLimit = attributes.get(Keys.ZENOH_ENABLE_RATE_LIMIT);
    }

    @Override
    public void setInputs(ObservableValues inputs) throws NodeException {
        dataIn = inputs.get(0).checkBits(bits[0], this, 0).addObserverToValue(this);
    }

    @Override
    public void readInputs() throws NodeException {
        if (!enablePublishing || enableRateLimit) {
            return;
        }
        sendData();
    }

    @Override
    public void writeOutputs() throws NodeException {
    }

    @Override
    public ObservableValues getOutputs() throws PinException {
        return ObservableValues.EMPTY_LIST;
        // return new ObservableValues(dataOut, dataAvail);
    }

    @Override
    public void init(Model model) throws NodeException {
        Session session = SessionHolder.INSTANCE.getSession();
        try {
            if (enablePublishing) {
                publisher = session.declarePublisher(KeyExpr.tryFrom(this.zenohKeyExpr)).res();
                // publish initial value usually 0
                sendData();
            }
            if (enableQuerying) {
                queryable = session.declareQueryable(KeyExpr.tryFrom(this.zenohKeyExpr)).with((query) -> {
                    System.out.println("Received query: " + query);
                    try {
                        long value = dataIn.getValue();
                        ByteBuffer buffer = ByteBuffer.allocate(8);
                        buffer.putLong(value);
                        query.reply(KeyExpr.tryFrom(this.zenohKeyExpr)).success(new io.zenoh.value.Value(buffer.array(), new Encoding(Encoding.ID.APPLICATION_OCTET_STREAM, null))).res();
                    } catch (ZenohException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }).res();
            }
        } catch (ZenohException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void cleanup(Model model) {
        if (enablePublishing)
            publisher.close();
        if (enableQuerying)
            queryable.close();
    }

    @Override
    public void registerNodes(Model model) {
        super.registerNodes(model);
        model.addZenohSender(this);
        // model.addSignal(new Signal(label, output));
    }

    @Override
    public boolean publishingEnabled() {
        return enablePublishing;
    }

    @Override
    public boolean isDataChanged() {
        long value = dataIn.getValue();
        return value != lastDataSent;
    }

    @Override
    public void sendData() {
        if (!enablePublishing || !isDataChanged()) {
            return;
        }
        long value = dataIn.getValue();
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(value);
        publisher.put(new io.zenoh.value.Value(buffer.array(), new Encoding(Encoding.ID.APPLICATION_OCTET_STREAM, null))).res();
        lastDataSent = value;
    }

}
