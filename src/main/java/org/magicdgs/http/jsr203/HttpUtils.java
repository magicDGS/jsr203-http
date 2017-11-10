package org.magicdgs.http.jsr203;

import java.net.HttpURLConnection;
import java.net.URLConnection;

/**
 * Utility classes for working with HTTP/S connections and URLs.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class HttpUtils {

    // utility class - cannot be instantiated
    private HttpUtils() {}

    /**
     * Disconnects the {@link URLConnection} if it is an instance of {@link HttpURLConnection}.
     *
     * @param connection the connection to be disconnected.
     */
    public static void disconnect(final URLConnection connection) {
        if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection) connection).disconnect();
        }
    }
}
