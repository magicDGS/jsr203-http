package org.magicdgs.http.jsr203;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract {@link FileSystemProvider} for {@link HttpFileSystem}.
 *
 * <p>HTTP/S are handled in the same way in jsr203-http, but every protocol requires its own
 * provider to return its scheme with {@link #getScheme()}.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
abstract class HttpAbstractFileSystemProvider extends FileSystemProvider {

    // map of authorities and FileSystem - using a concurrent implementation for being tread-safe
    private final Map<String, HttpFileSystem> fileSystems = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     *
     * @implNote should return a valid http/s scheme.
     */
    @Override
    public abstract String getScheme();

    // check the conditions for an URI, and return it if it is correct
    private URI checkUri(final URI uri) {
        // non-null URI
        Utils.nonNull(uri, () -> "null URI");
        // non-null authority
        Utils.nonNull(uri.getAuthority(),
                () -> String.format("%s requires URI with authority: invalid %s", this, uri));
        // check the scheme (sanity check)
        if (!getScheme().equalsIgnoreCase(uri.getScheme())) {
            throw new ProviderMismatchException(String.format("Invalid scheme for %s: %s",
                    this, uri.getScheme()));
        }
        return uri;
    }

    @Override
    public final HttpFileSystem newFileSystem(final URI uri, final Map<String, ?> env)
            throws IOException {
        checkUri(uri);

        if (fileSystems.containsKey(uri.getAuthority())) {
            throw new FileSystemAlreadyExistsException("URI: " + uri);
        }

        return fileSystems.computeIfAbsent(uri.getAuthority(),
                (auth) -> new HttpFileSystem(this, auth));
    }

    @Override
    public final HttpFileSystem getFileSystem(final URI uri) {
        final HttpFileSystem fs = fileSystems.get(checkUri(uri).getAuthority());
        if (fs == null) {
            throw new FileSystemNotFoundException("URI: " + uri);
        }
        return fs;
    }

    @Override
    public final HttpPath getPath(final URI uri) {
        checkUri(uri);
        return fileSystems
                .computeIfAbsent(uri.getAuthority(), (auth) -> new HttpFileSystem(this, auth))
                .getPath(uri);
    }

    @Override
    public final SeekableByteChannel newByteChannel(final Path path,
            final Set<? extends OpenOption> options, final FileAttribute<?>... attrs)
            throws IOException {
        Utils.nonNull(path, () -> "null path");
        Utils.nonNull(options, () -> "null options");

        if (options.isEmpty() ||
                (options.size() == 1 && options.contains(StandardOpenOption.READ))) {
            // convert Path to URI and check it to see if there is a mismatch with the provider
            final URL url = checkUri(path.toUri()).toURL();
            // throw if the URL does not exists
            if (!HttpUtils.exists(url)) {
                throw new NoSuchFileException(url.toString());
            }
            // return a URL SeekableByteChannel
            return new URLSeekableByteChannel(checkUri(path.toUri()).toURL());
        }
        throw new UnsupportedOperationException(
                String.format("Only %s is supported for %s, but %s options(s) are provided",
                        StandardOpenOption.WRITE, this, options));
    }

    @Override
    public final DirectoryStream<Path> newDirectoryStream(final Path dir,
            final DirectoryStream.Filter<? super Path> filter) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** Unsupported method. */
    @Override
    public final void createDirectory(final Path dir, final FileAttribute<?>... attrs)
            throws IOException {
        throw new UnsupportedOperationException(this.getClass().getName() +
                " is read-only: cannot create directory");
    }

    /** Unsupported method. */
    @Override
    public final void delete(final Path path) throws IOException {
        throw new UnsupportedOperationException(this.getClass().getName() +
                " is read-only: cannot delete directory");
    }

    @Override
    public final void copy(final Path source, final Path target, CopyOption... options)
            throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** Unsupported method. */
    @Override
    public final void move(final Path source, final Path target, final CopyOption... options)
            throws IOException {
        throw new UnsupportedOperationException(this.getClass().getName() +
                " is read-only: cannot move paths");
    }

    @Override
    public final boolean isSameFile(final Path path, final Path path2) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public final boolean isHidden(final Path path) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public final FileStore getFileStore(final Path path) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public final void checkAccess(final Path path, final AccessMode... modes) throws IOException {
        Utils.nonNull(path, () -> "null path");
        // get the URI (use also for exception messages)
        final URI uri = checkUri(path.toUri());
        if (!HttpUtils.exists(uri.toURL())) {
            throw new NoSuchFileException(uri.toString());
        }
        for (AccessMode access : modes) {
            switch (access) {
                case READ:
                    break;
                case WRITE:
                case EXECUTE:
                    throw new AccessDeniedException(uri.toString());
                default:
                    throw new UnsupportedOperationException("Unsupported access mode: " + access);
            }
        }
    }

    @Override
    public final <V extends FileAttributeView> V getFileAttributeView(final Path path,
            final Class<V> type, final LinkOption... options) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public final <A extends BasicFileAttributes> A readAttributes(final Path path,
            final Class<A> type, final LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public final Map<String, Object> readAttributes(final Path path, final String attributes,
            final LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public final void setAttribute(final Path path, final String attribute, final Object value,
            final LinkOption... options) throws IOException {
        throw new UnsupportedOperationException(this.getClass().getName() +
                " is read-only: cannot set attributes to paths");
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
