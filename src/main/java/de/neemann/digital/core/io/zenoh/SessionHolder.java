package de.neemann.digital.core.io.zenoh;

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
                session = Session.open();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return session;
    }
}
