package org.magicdgs.http.jsr203;

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
import java.util.Set;

/**
 * Read-only HTTP/S FileSystem.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
final class HttpFileSystem extends FileSystem {

    private final HttpAbstractFileSystemProvider provider;

    HttpFileSystem(final HttpAbstractFileSystemProvider provider) {
        this.provider = provider;
    }

    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean isOpen() {
        throw new UnsupportedOperationException("Not implemented");
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
        throw new UnsupportedOperationException("Not implemented");
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
        final String uriString;
        if (more.length == 0) {
            uriString = first;
        } else {
            final StringBuilder builder = new StringBuilder(first);
            for (final String part: more) {
                // ignore empty parts
                if (!part.isEmpty()) {
                    builder.append(HttpUtils.HTTP_PATH_SEPARATOR_STRING);
                    builder.append(part);
                }
            }
            uriString = builder.toString();
        }

        try {
            // TODO - should be URL instead?
            final URI uri = new URI(uriString);
            if (!uri.getScheme().equalsIgnoreCase(provider.getScheme())) {
                // TODO - should be mismatch exception?
                throw new InvalidPathException(uriString, "invalid scheme");
            }
            // TODO - rest of the checking should be done in HttpPath for URI
            return new HttpPath(uri, this);
        } catch (URISyntaxException e) {
            // convert into IPE
            throw new InvalidPathException(uriString, e.getReason(), e.getIndex());
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
}
