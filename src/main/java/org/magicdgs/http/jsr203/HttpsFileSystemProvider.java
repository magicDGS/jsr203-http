package org.magicdgs.http.jsr203;

/**
 * Read-only {@link java.nio.file.spi.FileSystemProvider} for HTTPS.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class HttpsFileSystemProvider extends HttpAbstractFileSystemProvider {

    /** Scheme for HTTPS files. */
    public static final String SCHEME = "https";

    /**
     * {@inheritDoc}
     *
     * @return {@link #SCHEME}.
     */
    @Override
    public final String getScheme() {
        return SCHEME;
    }
}
