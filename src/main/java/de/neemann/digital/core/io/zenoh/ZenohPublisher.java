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
import de.neemann.digital.lang.Lang;
import io.zenoh.Session;
import io.zenoh.exceptions.KeyExprException;
import io.zenoh.exceptions.ZenohException;
import io.zenoh.keyexpr.KeyExpr;
import io.zenoh.prelude.Encoding;
import io.zenoh.prelude.KnownEncoding;
import io.zenoh.publication.Publisher;
import io.zenoh.queryable.Queryable;

import java.nio.ByteBuffer;

import static de.neemann.digital.core.element.PinInfo.input;

/**
 * The telnet node
 */
public class ZenohPublisher extends Node implements Element {

    /**
     * The telnet server description
     */
    public static final ElementTypeDescription DESCRIPTION = new ElementTypeDescription(ZenohPublisher.class,
            input("in"))
            .addAttribute(Keys.BITS)
            .addAttribute(Keys.ROTATE)
            .addAttribute(Keys.LABEL)
            .addAttribute(Keys.ZENOH_KEYEXPR);

    private ObservableValue dataIn;
    private final int[] bits;
    private final String zenohKeyExpr;

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
    }

    @Override
    public void setInputs(ObservableValues inputs) throws NodeException {
        dataIn = inputs.get(0).checkBits(bits[0], this, 0).addObserverToValue(this);
    }

    @Override
    public void readInputs() throws NodeException {
        long value = dataIn.getValue();
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(value);
        try {
            publisher.put(new io.zenoh.value.Value(buffer.array(), new Encoding(KnownEncoding.APP_OCTET_STREAM))).res();
        } catch (ZenohException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
            publisher = session.declarePublisher(KeyExpr.tryFrom(this.zenohKeyExpr)).res();
            queryable = session.declareQueryable(KeyExpr.tryFrom(this.zenohKeyExpr)).with((query) -> {
                System.out.println("Received query: " + query);
                try {
                    long value = dataIn.getValue();
                    ByteBuffer buffer = ByteBuffer.allocate(8);
                    buffer.putLong(value);
                    query.reply(KeyExpr.tryFrom(this.zenohKeyExpr)).success(new io.zenoh.value.Value(buffer.array(), new Encoding(KnownEncoding.APP_OCTET_STREAM))).res();
                } catch (ZenohException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }).res();
        } catch (ZenohException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void cleanup(Model model) {
        publisher.close();
        queryable.close();
    }

}
