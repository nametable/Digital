package de.neemann.digital.core.io.zenoh;

import static de.neemann.digital.core.element.PinInfo.input;

import de.neemann.digital.core.Model;
import de.neemann.digital.core.NodeException;
import de.neemann.digital.core.element.ElementAttributes;
import de.neemann.digital.core.element.ElementTypeDescription;
import de.neemann.digital.core.element.Keys;

import de.neemann.digital.core.memory.Register;
import io.zenoh.Session;
import io.zenoh.exceptions.ZenohException;
import io.zenoh.keyexpr.KeyExpr;
import io.zenoh.prelude.Encoding;
import io.zenoh.prelude.KnownEncoding;
import io.zenoh.publication.Publisher;
import io.zenoh.queryable.Queryable;
import io.zenoh.subscriber.Subscriber;

import java.nio.ByteBuffer;

/**
 * An extended version of the Register class that uses Zenoh to publish changes to the register's value
 * and subscribe to external change requests.
 */
public class ZenohRegister extends Register implements ZenohDataSender {

    /**
     * The Zenoh registers {@link ElementTypeDescription}
     */
    public static final ElementTypeDescription DESCRIPTION = new ElementTypeDescription(ZenohRegister.class, input("D"),
            input("C").setClock(), input("en"))
            .addAttribute(Keys.ROTATE)
            .addAttribute(Keys.BITS)
            .addAttribute(Keys.LABEL)
            .addAttribute(Keys.INVERTER_CONFIG)
            .addAttribute(Keys.IS_PROGRAM_COUNTER)
            .addAttribute(Keys.VALUE_IS_PROBE)
            .addAttribute(Keys.ZENOH_KEYEXPR)
            .addAttribute(Keys.ZENOH_ENABLE_PUBLISHING)
            .addAttribute(Keys.ZENOH_ENABLE_RATE_LIMIT);

    private final String baseZenohKeyExpr;
    private final boolean enablePublishing; // whether or not to enable publishing
    private final boolean enableRateLimit;

    private Publisher changePublisher;
    private Subscriber setSubscriber;
    private Queryable getQueryable;
    private Queryable infoQueryable;

    private long lastDataSent;

    public ZenohRegister(ElementAttributes attributes) {
        super(attributes);
        baseZenohKeyExpr = attributes.get(Keys.ZENOH_KEYEXPR);
        enablePublishing = attributes.get(Keys.ZENOH_ENABLE_PUBLISHING);
        enableRateLimit = attributes.get(Keys.ZENOH_ENABLE_RATE_LIMIT);
    }
    
    @Override
    public void writeOutputs() throws NodeException {
        q.setValue(value);
        if (!enableRateLimit) {
            sendData();
        }
    }

    @Override
    public void init(Model model) throws NodeException {
        Session session = SessionHolder.INSTANCE.getSession();
        try {
            if (enablePublishing)
                changePublisher = session.declarePublisher(KeyExpr.tryFrom(this.baseZenohKeyExpr + "/changes")).res();
            setSubscriber = session.declareSubscriber(KeyExpr.tryFrom(this.baseZenohKeyExpr + "/set")).with(sample -> {
                ByteBuffer buffer = ByteBuffer.wrap(sample.getValue().getPayload());
                this.value = buffer.getLong();
                model.modify(() -> q.setValue(this.value));
                sendData();
            }).res();

            getQueryable = session.declareQueryable(KeyExpr.tryFrom(this.baseZenohKeyExpr + "/get")).with(query -> {
                try {
                    ByteBuffer buffer = ByteBuffer.allocate(8);
                    buffer.putLong(this.value);
                    query.reply(query.getKeyExpr())
                            .success(new io.zenoh.value.Value(buffer.array(), new Encoding(KnownEncoding.APP_OCTET_STREAM)))
                            .res();
                } catch (ZenohException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }).res();

            infoQueryable = session.declareQueryable(KeyExpr.tryFrom(this.baseZenohKeyExpr + "/info")).with(query -> {
                System.out.println("Received query: " + query);
                try {
                    byte[] labelBytes = this.label.getBytes();
                    ByteBuffer buffer = ByteBuffer.allocate(8 + labelBytes.length);
                    buffer.putInt(this.bits);
                    buffer.putInt(labelBytes.length);
                    buffer.put(labelBytes);
                    query.reply(query.getKeyExpr())
                            .success(new io.zenoh.value.Value(buffer.array(), new Encoding(KnownEncoding.APP_OCTET_STREAM)))
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

    @Override
    public void registerNodes(Model model) {
        super.registerNodes(model);
        model.addZenohSender(this);
    }

    @Override
    public boolean publishingEnabled() {
        return enablePublishing;
    }

    @Override
    public boolean isDataChanged() {
        return this.value != lastDataSent;
    }

    @Override
    public void sendData() {
        boolean isChanging = this.value != this.lastDataSent;
        if (isChanging && enablePublishing) {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(8);
                buffer.putLong(this.value);
                changePublisher.put(new io.zenoh.value.Value(buffer.array(), new Encoding(KnownEncoding.APP_OCTET_STREAM))).res();
                lastDataSent = this.value;
            } catch (ZenohException e) {
                e.printStackTrace();
            }
        }
    }

}
