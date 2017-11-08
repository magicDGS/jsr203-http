package org.magicdgs.http.jsr203;

/**
 * Read-only {@link java.nio.file.spi.FileSystemProvider} for HTTP.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class HttpFileSystemProvider extends HttpAbstractFileSystemProvider {

    /** Scheme for HTTP files. */
    public static final String SCHEME = "http";

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
