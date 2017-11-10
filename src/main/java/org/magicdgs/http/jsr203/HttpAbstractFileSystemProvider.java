package org.magicdgs.http.jsr203;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;

/**
 * Abstract {@link FileSystemProvider} for {@link HttpFileSystem}.
 *
 * <p>HTTP/S are handled in the same way in jsr203-http, but every protocol requires its own
 * provider to return its scheme with {@link #getScheme()}.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
abstract class HttpAbstractFileSystemProvider extends FileSystemProvider {

    /**
     * {@inheritDoc}
     *
     * @implNote should return a valid http/s scheme.
     */
    @Override
    public abstract String getScheme();

    @Override
    public final HttpFileSystem newFileSystem(final URI uri, final Map<String, ?> env)
            throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public final HttpFileSystem getFileSystem(final URI uri) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public final Path getPath(final URI uri) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public final SeekableByteChannel newByteChannel(final Path path,
            final Set<? extends OpenOption> options, final FileAttribute<?>... attrs)
            throws IOException {
        throw new UnsupportedOperationException("Not implemented");
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
        throw new UnsupportedOperationException("Not implemented");
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
}
