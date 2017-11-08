package org.magicdgs.http.jsr203;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Base test class with useful methods.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class BaseTest {

    // base URL for the repository
    private static final String GITHUP_PAGES_BASE_URL = "https://magicdgs.github.io/jsr203-http/";

    // base path for a file in the docs folder
    private static final String DOCS_BASE_PATH = "docs/";

    /**
     * Gets the URL for a file in the project GitHub-pages.
     *
     * <p>Calling this method with the same file name as in {@link #getPathFromLocalDocsFile(String)}
     * retrieves the equivalent file in the GitHub-pages URL.
     *
     * @param fileName the name of the file (including intermediate directories).
     *
     * @return URL string in GitHub-pages.
     */
    public static String getGithubPagesFileUrl(final String fileName) {
        return GITHUP_PAGES_BASE_URL + fileName;
    }

    /**
     * Gets the path for a file in docs directory.
     *
     * <p>Calling this method with the same file name as in {@link #getGithubPagesFileUrl(String)}
     * retrieves the equivalent file in the local file system.
     *
     * @param fileName the name of the file (including intermediate directories).
     *
     * @return path in the local file system.
     */
    public static String getPathFromLocalDocsFile(final String fileName) {
        return DOCS_BASE_PATH + fileName;
    }


    /**
     * Read all bytes from a provided HTTP/S {@link URL}.
     *
     * <p>WARNING: do not use with huge files.
     *
     * @param httpUrl URL to read.
     *
     * @return byte array containing all the bytes in the file.
     */
    public static byte[] readAllBytes(final URL httpUrl) throws Exception {
        final HttpURLConnection connection = (HttpURLConnection) httpUrl.openConnection();
        try {
            // open the connection and the input stream
            // open the connection and try to guess the length of the output
            connection.connect();
            final int length = connection.getContentLength();

            // open the input stream
            final InputStream is = connection.getInputStream();

            // if the content is available, allocate directly the byte array
            if (length != -1) {
                final byte[] result = new byte[length];
                final int read = is.read(result);
                if (read != length) {
                    throw new RuntimeException(
                            "Read " + read + " bytes, but " + length + "expected");
                }
                return result;
            }

            // otherwise, read with a buffer
            byte[] buffer = new byte[0xFFFF];
            final ByteArrayOutputStream bso = new ByteArrayOutputStream();
            for (int len; (len = is.read(buffer)) != -1; ) {
                bso.write(buffer, 0, len);
            }

            return bso.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

}
