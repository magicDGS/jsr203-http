package org.magicdgs.http.jsr203;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;

/**
 * {@link Path} for HTTP/S.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
final class HttpPath implements Path {

    @Override
    public FileSystem getFileSystem() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean isAbsolute() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Path getRoot() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Path getFileName() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Path getParent() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int getNameCount() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Path getName(final int index) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Path subpath(final int beginIndex, final int endIndex) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean startsWith(final Path other) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean startsWith(final String other) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean endsWith(final Path other) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean endsWith(final String other) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Path normalize() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Path resolve(final Path other) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Path resolve(final String other) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Path resolveSibling(final Path other) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Path resolveSibling(final String other) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Path relativize(final Path other) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public URI toUri() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Path toAbsolutePath() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Path toRealPath(final LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** Unsupported method. */
    @Override
    public File toFile() {
        throw new UnsupportedOperationException(this.getClass() + " cannot be converted to a File");
    }

    @Override
    public WatchKey register(final WatchService watcher, final WatchEvent.Kind<?>[] events,
            final WatchEvent.Modifier... modifiers) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public WatchKey register(final WatchService watcher, final WatchEvent.Kind<?>... events)
            throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Iterator<Path> iterator() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int compareTo(final Path other) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
