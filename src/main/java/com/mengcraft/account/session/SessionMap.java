package com.mengcraft.account.session;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created on 15-10-23.
 */
public class SessionMap extends ConcurrentHashMap<String, Session> {

    public SessionMap() {
        super(32);
    }

    /**
     * Return a <code>Session</code> and remove it from this container.
     *
     * @param key The player's name.
     * @return The <code>Session</code> if is exists and not outdated.
     */
    @Override
    public Session get(Object key) {
        return remove(key);
    }

    /**
     * @param key The player's name.
     * @return The <code>Session</code> if is exists and not outdated.
     * @see SessionMap#get(Object)
     */
    @Override
    public Session remove(Object key) {
        return checkOutdated(super.remove(key));
    }

    private Session checkOutdated(Session session) {
        return session == null ? null : session.isOutdated() ? null : session;
    }

}
