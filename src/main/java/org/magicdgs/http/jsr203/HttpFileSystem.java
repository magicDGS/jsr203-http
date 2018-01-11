package org.magicdgs.http.jsr203;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Set;

/**
 * Read-only HTTP/S FileSystem.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
final class HttpFileSystem extends FileSystem {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final HttpAbstractFileSystemProvider provider;

    // authority for this FileSystem
    private final String authority;

    /**
     * Construct a new FileSystem.
     *
     * @param provider non {@code null} provider that generated this HTTP/S File System.
     * @param authority non {@code null} authority for this HTTP/S File System.
     */
    HttpFileSystem(final HttpAbstractFileSystemProvider provider, final String authority) {
        this.provider = Utils.nonNull(provider, () -> "null provider");
        if (authority == null) {
            throw new NullPointerException("Null authority");
        }
        this.authority = authority;
    }

    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    /**
     * Gets the authority for this File System.
     *
     * @return the authority for this File System.
     */
    public String getAuthority() {
        return authority;
    }

    @Override
    public void close() {
        // TODO - this could remove the instance from the provider and let the JVM clean up
        logger.warn("{} is always open (do not close)", this.getClass());
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true}.
     */
    @Override
    public boolean isReadOnly() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link HttpUtils#HTTP_PATH_SEPARATOR_STRING}.
     */
    @Override
    public String getSeparator() {
        return HttpUtils.HTTP_PATH_SEPARATOR_STRING;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        // the root directory does not have the slash
        return Collections.singleton(new HttpPath(this, "", null, null));
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Path getPath(final String first, final String... more) {
        final String path = first + String.join(getSeparator(), more);

        if (!path.isEmpty() && !path.startsWith(getSeparator())) {
            throw new InvalidPathException(path, "Relative paths are not supported", 0);
        }

        try {
            // handle the Path with the URI to separate Path query and fragment
            // in addition, it checks for errors in the encoding (e.g., null chars)
            final URI uri = new URI(path);
            return new HttpPath(this, uri.getPath(), uri.getQuery(), uri.getFragment());
        } catch (URISyntaxException e) {
            throw new InvalidPathException(e.getInput(), e.getReason(), e.getIndex());
        }
    }

    @Override
    public PathMatcher getPathMatcher(final String syntaxAndPattern) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String toString() {
        return String.format("%s[%s]@%s", this.getClass().getSimpleName(), provider, hashCode());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other instanceof HttpFileSystem) {
            final HttpFileSystem ofs = (HttpFileSystem) other;
            return provider() == ofs.provider() && getAuthority().equalsIgnoreCase(ofs.getAuthority());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 31 * provider.hashCode() + getAuthority().toLowerCase().hashCode();
    }
}
