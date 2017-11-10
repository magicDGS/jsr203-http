package org.magicdgs.http.jsr203;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

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
     * Gets the full URL name for a file in the project's GitHub-pages.
     *
     * <p>Calling this method with the same file name as in {@link #getLocalDocsFullPathName(String)}
     * retrieves the equivalent file in the GitHub-pages URL.
     *
     * @param baseName the base name of the file (including intermediate directories).
     *
     * @return full URL string in GitHub-pages.
     */
    public static String getGithubPagesFullUrlName(final String baseName) {
        return GITHUP_PAGES_BASE_URL + baseName;
    }

    /**
     * Gets the URL for a file in the project GitHub-pages.
     *
     * @param baseName the base name of the file (including intermediate directories).
     *
     * @return URL string in GitHub-pages.
     *
     * @throws AssertionError if the URL is malformed (unexpected).
     * @see #getGithubPagesFileUrl(String)
     */
    public static URL getGithubPagesFileUrl(final String baseName) {
        try {
            return new URL(getGithubPagesFullUrlName(baseName));
        } catch (MalformedURLException e) {
            throw new AssertionError("Unexpected error for GitHub file " + baseName, e);
        }
    }

    /**
     * Gets the full path name for a file in docs directory.
     *
     * <p>Calling this method with the same file name as in {@link #getGithubPagesFullUrlName(String)}
     * retrieves the equivalent file in the local file system.
     *
     * @param baseName the base name of the file (including intermediate directories).
     *
     * @return path in the local file system.
     */
    public static String getLocalDocsFullPathName(final String baseName) {
        return DOCS_BASE_PATH + baseName;
    }

    /**
     * Gets the Path for a file in docs directory.
     *
     * @param baseName the base name of the file (including intermediate directories).
     *
     * @return path in the local file system.
     *
     * @see #getLocalDocsFullPathName(String)
     */
    public static Path getLocalDocsFilePath(final String baseName) {
        final Path path = new File(getLocalDocsFullPathName(baseName)).toPath();
        return path;
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
