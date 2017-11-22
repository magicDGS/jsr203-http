package org.magicdgs.http.jsr203;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URLConnection;

/**
 * Utility classes for working with HTTP/S connections and URLs.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class HttpUtils {

    // key for 'Range' request
    private static final String RANGE_REQUEST_PROPERTY_KEY = "Range";
    // value for 'Range' request: START + POSITION + SEPARATOR (+ END)
    private static final String RANGE_REQUEST_PROPERTY_VALUE_START = "bytes=";
    private static final String RANGE_REQUEST_PROPERTY_VALUE_SEPARATOR = "-";

    // logger for HttpUtils
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);

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

    /**
     * Request a range of bytes for a {@link URLConnection}.
     *
     * @param connection the connection to request the range.
     * @param start      positive byte number to start the request.
     * @param end        positive byte number to end the request; {@code -1} if no bounded.
     *
     * @throws IllegalStateException    if the connection is already connected.
     * @throws IllegalArgumentException if the request is invalid.
     */
    public static void setRangeRequest(final URLConnection connection, final long start,
            final long end) {
        // setting the request range
        String request = RANGE_REQUEST_PROPERTY_VALUE_START
                + start
                + RANGE_REQUEST_PROPERTY_VALUE_SEPARATOR;
        // include end bound
        if (end != -1) {
            request += end;
        }

        // invalid request params should throw
        if (start < 0 || end < -1 || (end != -1 && end < start)) {
            throw new IllegalArgumentException("Invalid request: " + request);
        }

        LOGGER.debug("Request '{}' {} for {}", RANGE_REQUEST_PROPERTY_KEY, request, connection);
        // set the range if the position is different from 0
        connection.setRequestProperty(RANGE_REQUEST_PROPERTY_KEY, request);
    }

}
