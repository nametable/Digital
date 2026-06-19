package de.neemann.digital.core.io.zenoh;

import io.zenoh.Config;
import io.zenoh.Session;
import io.zenoh.Zenoh;

public final class SessionHolder {
    /**
     * The singleton instance
     */
    public static final SessionHolder INSTANCE = new SessionHolder();

    private Session session;

    public Session getSession() {
        if (session == null) {
            try {
                Config config = Config.fromJson("{\"mode\":\"peer\",\"transport\":{\"link\":{\"tx\":{\"queue\":{\"batching\":{\"enabled\":false}}}}}}");
                session = Zenoh.open(config);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return session;
    }
}
