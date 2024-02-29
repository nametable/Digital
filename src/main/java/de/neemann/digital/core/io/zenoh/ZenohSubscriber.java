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
import io.zenoh.prelude.KnownEncoding;
import io.zenoh.sample.Sample;
import io.zenoh.subscriber.Subscriber;

import java.io.IOException;
import java.nio.ByteBuffer;

import static de.neemann.digital.core.element.PinInfo.input;
import static de.neemann.digital.core.element.PinInfo.output;

/**
 * The ZenohSubscriber node
 */
public class ZenohSubscriber extends Node implements Element {

    /**
     * The Zenoh Subscriber description
     */
    public static final ElementTypeDescription DESCRIPTION = new ElementTypeDescription(ZenohSubscriber.class)
            .addAttribute(Keys.BITS)
            .addAttribute(Keys.ROTATE)
            .addAttribute(Keys.LABEL)
            .addAttribute(Keys.ZENOH_KEYEXPR);

    private ObservableValue dataOut;
    private final int[] bits;
    private final String zenohKeyExpr;
    private Model model;

    private Subscriber subscriber;

    /**
     * Creates a new instance
     *
     * @param attributes The components attributes
     */
    public ZenohSubscriber(ElementAttributes attributes) {
        bits = new int[] { attributes.getBits() };
        dataOut = new ObservableValue("out", bits[0]).setPinDescription(DESCRIPTION);
        zenohKeyExpr = attributes.get(Keys.ZENOH_KEYEXPR);
    }

    @Override
    public void setInputs(ObservableValues inputs) throws NodeException {
        throw new NodeException(Lang.get("err_noInputsAvailable"));
    }

    @Override
    public void readInputs() throws NodeException {
    }

    @Override
    public void writeOutputs() throws NodeException {
    }

    @Override
    public ObservableValues getOutputs() {
        return dataOut.asList();
    }

    public void onSample(Sample sample) {
        byte[] payload = sample.getValue().getPayload();
        
        System.out.println("Received sample: " + payload);
        ByteBuffer buffer = ByteBuffer.wrap(payload);

        // todo: handle error when buffer is not big enough
        long testInt = buffer.getLong();

        model.modify(() -> dataOut.setValue(testInt));
    }

    @Override
    public void init(Model model) throws NodeException {

        Session session = SessionHolder.INSTANCE.getSession();
        this.model = model;

        try {
            subscriber = session.declareSubscriber(KeyExpr.tryFrom(this.zenohKeyExpr))
                    .with(sample -> this.onSample(sample)).res();
            subscriber.getReceiver();

            session.get(KeyExpr.tryFrom(this.zenohKeyExpr)).res();
        } catch (ZenohException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void cleanup(Model model) {
        System.out.println("Cleaning up ZenohSubscriber");
        subscriber.close();
    }

}
