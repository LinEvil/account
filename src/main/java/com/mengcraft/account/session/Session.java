package com.mengcraft.account.session;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.System.currentTimeMillis;

/**
 * Created on 15-10-23.
 */
public class Session {

    private static final Random RANDOM = ThreadLocalRandom.current();

    private final int x;
    private final int y;
    private final int z;

    private final long outdated = currentTimeMillis() + 180000;

    public Session(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Session() {
        this.x = RANDOM.nextInt();
        this.y = RANDOM.nextInt();
        this.z = RANDOM.nextInt();
    }

    public boolean isOutdated() {
        return outdated > currentTimeMillis();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * x + y) + z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Session session = (Session) o;
        return x == session.x && y == session.y && z == session.z;
    }

}
