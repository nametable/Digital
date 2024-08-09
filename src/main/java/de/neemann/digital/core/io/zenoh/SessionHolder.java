package de.neemann.digital.core.io.zenoh;

import io.zenoh.Config;
import io.zenoh.Session;

public final class SessionHolder {
    /**
     * The singleton instance
     */
    public static final SessionHolder INSTANCE = new SessionHolder();

    private Session session;

    public Session getSession() {
        if (session == null) {
            try {
                Config config = Config.Companion.from("{\"transport\":{\"link\":{\"tx\":{\"batching\":false}}}}");
                session = Session.open(config);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return session;
    }
}
