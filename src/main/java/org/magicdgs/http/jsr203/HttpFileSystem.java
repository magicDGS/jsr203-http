package org.magicdgs.http.jsr203;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
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

    // TODO - remove this constructor (https://github.com/magicDGS/jsr203-http/issues/17)
    HttpFileSystem() {
        this.provider = null;
    }

    /**
     * Construct a new FileSystem.
     *
     * @param provider non {@code null} provider that generated this HTTP/S File System.
     */
    HttpFileSystem(final HttpAbstractFileSystemProvider provider) {
        if (provider == null) {
            throw new NullPointerException("Null FileSystemProvider");
        }
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

    @Override
    public String getSeparator() {
        throw new UnsupportedOperationException("Not implemented");
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
        throw new UnsupportedOperationException("Not implemented");
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
