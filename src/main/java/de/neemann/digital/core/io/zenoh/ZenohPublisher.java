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
import io.zenoh.publication.Publisher;

import java.io.IOException;
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
            // .addAttribute(Keys.TELNET_ESCAPE)
            // .addAttribute(Keys.PORT);

    // private final ObservableValue dataOut;
    // private final ObservableValue dataAvail;
    // private final int port;
    // private final boolean telnetEscape;
    private ObservableValue dataIn;
    private final int[] bits;
    private final String zenohKeyExpr;
    // private ObservableValue clockValue;
    // private ObservableValue writeEnable;
    // private ObservableValue readEnableValue;
    // private Server server;
    // private boolean lastClock;
    // private boolean readEnable;

    private Publisher publisher;

    /**
     * Creates a new instance
     *
     * @param attributes The components attributes
     */
    public ZenohPublisher(ElementAttributes attributes) {
        bits = new int[]{attributes.getBits()};
        zenohKeyExpr = attributes.get(Keys.ZENOH_KEYEXPR);
        // dataOut = new ObservableValue("out", 8)
        //         .setToHighZ()
        //         .setPinDescription(DESCRIPTION);
        // dataAvail = new ObservableValue("av", 1)
        //         .setPinDescription(DESCRIPTION);
        // port = attributes.get(Keys.PORT);
        // telnetEscape = attributes.get(Keys.TELNET_ESCAPE);
    }

    @Override
    public void setInputs(ObservableValues inputs) throws NodeException {
        dataIn = inputs.get(0).checkBits(bits[0], this, 0).addObserverToValue(this);
        // clockValue = inputs.get(1).checkBits(1, this, 1).addObserverToValue(this);
        // writeEnable = inputs.get(2).checkBits(1, this, 2);
        // readEnableValue = inputs.get(3).checkBits(1, this, 3).addObserverToValue(this);
    }

    @Override
    public void readInputs() throws NodeException {
        long value = dataIn.getValue();
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(value);
        try {
            publisher.put(new io.zenoh.value.Value(buffer.array(), new Encoding(KnownEncoding.APP_INTEGER))).res();
        } catch (ZenohException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // boolean clock = clockValue.getBool();
        // readEnable = readEnableValue.getBool();
        // if (clock & !lastClock) {
        //     if (writeEnable.getBool())
        //         server.send((int) dataIn.getValue());
        //     if (readEnable)
        //         server.deleteOldest();
        // }
        // lastClock = clock;
    }

    @Override
    public void writeOutputs() throws NodeException {
        // if (readEnable)
        //     dataOut.setValue(server.getData());
        // else
        //     dataOut.setToHighZ();

        // dataAvail.setBool(server.hasData());
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
        } catch (KeyExprException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // try {
        // } catch (IOException e) {
        //     throw new NodeException(Lang.get("err_couldNotCreateServer"), e);
        // }
        // server.setTelnetEscape(telnetEscape);
        // server.setTelnetNode(this, model);
    }

}
