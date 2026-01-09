package de.neemann.digital.core.io.zenoh;

import static de.neemann.digital.core.element.PinInfo.input;

import de.neemann.digital.core.Model;
import de.neemann.digital.core.NodeException;
import de.neemann.digital.core.element.ElementAttributes;
import de.neemann.digital.core.element.ElementTypeDescription;
import de.neemann.digital.core.element.ImmutableList;
import de.neemann.digital.core.element.Keys;

import de.neemann.digital.core.memory.Register;
import io.zenoh.Session;
import io.zenoh.bytes.Encoding;
import io.zenoh.bytes.ZBytes;
import io.zenoh.exceptions.ZError;
import io.zenoh.keyexpr.KeyExpr;
import io.zenoh.pubsub.Publisher;
import io.zenoh.pubsub.Subscriber;
import io.zenoh.query.Queryable;

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

    private final String baseZenohKeyExprStr;
    private final boolean enablePublishing; // whether or not to enable publishing
    private final boolean enableRateLimit;

    private Publisher changePublisher;
    private Subscriber setSubscriber;
    private Queryable getQueryable;

    private long lastDataSent;

    public ZenohRegister(ElementAttributes attributes) {
        super(attributes);
        baseZenohKeyExprStr = attributes.get(Keys.ZENOH_KEYEXPR);
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
            if (enablePublishing) {
                changePublisher = session.declarePublisher(KeyExpr.tryFrom(this.baseZenohKeyExprStr + "/changes"));
            }
            setSubscriber = session.declareSubscriber(KeyExpr.tryFrom(this.baseZenohKeyExprStr + "/set"), sample -> {
                ByteBuffer buffer = ByteBuffer.wrap(sample.getPayload().toBytes());
                this.value = buffer.getLong();
                model.modify(() -> q.setValue(this.value));
                sendData();
            });

            getQueryable = session.declareQueryable(KeyExpr.tryFrom(this.baseZenohKeyExprStr), query -> {
                try {
                    ByteBuffer buffer = ByteBuffer.allocate(8);
                    buffer.putLong(this.value);
                    query.reply(query.getKeyExpr(), ZBytes.from(buffer.array()));
                } catch (ZError e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });
        } catch (ZError e) {
            throw new NodeException("Invalid Zenoh key expression: \"" + this.baseZenohKeyExprStr + "\"", this, -1, new ImmutableList<>());
        }
    }

    @Override
    public void cleanup(Model model) {
        changePublisher.close();
        setSubscriber.close();
        getQueryable.close();
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
                changePublisher.put(ZBytes.from(buffer.array()));
                lastDataSent = this.value;
            } catch (ZError e) {
                e.printStackTrace();
            }
        }
    }

}
