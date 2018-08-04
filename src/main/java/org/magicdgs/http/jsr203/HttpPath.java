package org.magicdgs.http.jsr203;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;

/**
 * {@link Path} for HTTP/S.
 *
 * <p>The HTTP/S paths holds the following information:
 *
 * <ul>
 *
 * <li>
 * The {@link HttpFileSystem} originating the path. The protocol is retrieved, if necessary,
 * from the provider of the File System.
 * </li>
 *
 * <li>
 * The hostname and domain for the URL/URI in a single authority String.
 * </li>
 *
 * <li>
 * If present, the path component of the URL/URI.
 * </li>
 *
 * <li>
 * If present, the query and reference Strings.
 * </li>
 *
 * </ul>
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
final class HttpPath implements Path {

    // file system (indicates the scheme - HTTP or HTTPS)
    private final HttpFileSystem fs;

    // path - similar to other implementation of Path
    private final byte[] normalizedPath;

    // query for the URL (may be null)
    private final String query;
    // reference for the URL (may be null) / fragment for the URI representation
    private final String reference;

    /**
     * Internal constructor.
     *
     * @param fs             file system. Shouldn't be {@code null}.
     * @param query          query. May be {@code null}.
     * @param reference      reference. May be {@code null}.
     * @param normalizedPath normalized path (as a byte array). Shouldn't be {@code null}.
     *
     * @implNote does not perform any check for efficiency.
     */
    private HttpPath(final HttpFileSystem fs,
            final String query, final String reference,
            final byte... normalizedPath) {
        this.fs = fs;

        // optional query and reference components (may be null)
        this.query = query;
        this.reference = reference;

        // normalized path bytes (shouldn't be null)
        this.normalizedPath = normalizedPath;
    }

    /**
     * Creates a new Path in the provided {@link HttpFileSystem}, with optional query and reference.
     *
     * @param fs file system representing the base URL (scheme and authority).
     * @param path path component for the URL (required).
     * @param query query  component for the URL (optional).
     * @param reference reference component for the URL (optional).
     */
    HttpPath(final HttpFileSystem fs, final String path, final String query, final String reference) {
        this(Utils.nonNull(fs, () -> "null fs"), query, reference,
                getNormalizedPathBytes(Utils.nonNull(path, () -> "null path")));
    }

    @Override
    public HttpFileSystem getFileSystem() {
        return fs;
    }

    @Override
    public boolean isAbsolute() {
        // TODO - change when we support relative Paths (https://github.com/magicDGS/jsr203-http/issues/12)
        return true;
    }

    @Override
    public Path getRoot() {
        // root is a Path with only the byte array
        return new HttpPath(fs, null, null);
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
        // different FileSystems return false
        if (!this.getFileSystem().equals(other.getFileSystem())) {
            return false;
        }

        return startsWith(((HttpPath) other).normalizedPath);
    }

    @Override
    public boolean startsWith(final String other) {
        // throw if null
        Utils.nonNull(other, () -> "null other");
        // normalize the path and check with the byte method
        return startsWith(getNormalizedPathBytes(other));
    }

    /**
     * Private method to test startsWith only for the path component.
     *
     * <p>The contract for this method is the same as {@link #startsWith(Path)} (Path)}, but only
     * for the path component.
     *
     * @param other the other path component.
     * @return {@code true} if {@link #normalizedPath} ends with {@code other}; {@code false}
     * otherwise.
     */
    private boolean startsWith(final byte[] other) {
        // the other can still end in '/', so we should trim
        final int olen = getLastIndexWithoutTrailingSlash(other);

        // the other path component cannot have a larger than this for startWith
        if (olen > normalizedPath.length) {
            return false;
        }

        // check the bytes of the normalized path
        int i;
        for (i = 0; i <= olen; i++) {
            if (normalizedPath[i] != other[i]) {
                return false;
            }
        }

        // finally check the name boundary
        return i >= this.normalizedPath.length
                || this.normalizedPath[i] == HttpUtils.HTTP_PATH_SEPARATOR_CHAR;
    }

    @Override
    public boolean endsWith(final Path other) {
        // different FileSystems return false
        if (!this.getFileSystem().equals(other.getFileSystem())) {
            return false;
        }

        // compare the path component
        return endsWith(((HttpPath) other).normalizedPath);
    }

    @Override
    public boolean endsWith(final String other) {
        // throw if null
        Utils.nonNull(other, () -> "null other");
        // normalize the path and check with the byte method
        return endsWith(getNormalizedPathBytes(other));
    }

    /**
     * Private method to test endsWith only for the path component.
     *
     * <p>The contract for this method is the same as {@link #endsWith(Path)}, but only for the
     * path component.
     *
     * @param other the other path component.
     * @return {@code true} if {@link #normalizedPath} ends with {@code other}; {@code false}
     * otherwise.
     */
    private boolean endsWith(final byte[] other) {
        // get the last index to check
        int olast = getLastIndexWithoutTrailingSlash(other);
        // get the last index to check
        int last = getLastIndexWithoutTrailingSlash(this.normalizedPath);

        // early termination if the length is 0 (last index = -1)
        if (olast == -1) {
            return last == -1;
        }
        // early termination if the other is larger
        if (last < olast) {
            return false;
        }

        // iterate over the bytes to check if they are the same
        for (; olast >= 0; olast--, last--) {
            if (other[olast] != this.normalizedPath[last]) {
                return false;
            }
        }

        // TODO: this codepath is not exercised at all because we always have
        // TODO: absolute paths, and thus the first statement is always true
        // TODO: this will change in the future (https://github.com/magicDGS/jsr203-http/issues/12)
        // final check for name boundary
        return other[olast + 1] == HttpUtils.HTTP_PATH_SEPARATOR_CHAR
                || last == -1 || this.normalizedPath[last] == HttpUtils.HTTP_PATH_SEPARATOR_CHAR;
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
        try {
            return new URI(fs.provider().getScheme(),
                    fs.getAuthority(),
                    new String(normalizedPath, HttpUtils.HTTP_PATH_CHARSET),
                    query, reference);
        } catch (final URISyntaxException e) {
            throw new IOError(e);
        }
    }

    @Override
    public Path toAbsolutePath() {
        if (isAbsolute()) {
            return this;
        }
        // TODO - change when we support relative Paths (https://github.com/magicDGS/jsr203-http/issues/12)
        throw new IllegalStateException("Should not appear a relative HTTP/S paths (unsupported)");
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

    /**
     * {@inheritDoc}
     *
     * @implNote comparison of every component of the HTTP/S path is case-sensitive, except the
     * scheme and the authority.
     * @implNote if the query and/or reference are present, this method order the one without any
     * of them first.
     */
    @Override
    public int compareTo(final Path other) {
        if (this == other) {
            return 0;
        }

        final HttpPath httpOther = (HttpPath) other;
        // object comparison - should be from the same provider
        if (fs.provider() != httpOther.fs.provider()) {
            throw new ClassCastException();
        }

        // first check the authority (case insensitive)
        int comparison = fs.getAuthority().compareToIgnoreCase(httpOther.fs.getAuthority());
        if (comparison != 0) {
            return comparison;
        }

        // then check the path
        final int len1 = normalizedPath.length;
        final int len2 = httpOther.normalizedPath.length;
        final int n = Math.min(len1, len2);
        for (int k = 0; k < n; k++) {
            // this is case sensitive
            comparison = Byte.compare(this.normalizedPath[k], httpOther.normalizedPath[k]);
            if (comparison != 0) {
                return comparison;
            }
        }
        comparison = len1 - len2;
        if (comparison != 0) {
            return comparison;
        }

        // compare the query if present
        comparison = Comparator.nullsFirst(String::compareTo).compare(this.query, httpOther.query);
        if (comparison != 0) {
            return comparison;
        }

        // otherwise, just return the value of comparing the fragment
        return Comparator.nullsFirst(String::compareTo)
                .compare(this.reference, httpOther.reference);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote it uses the {@link #compareTo(Path)} method.
     */
    @Override
    public boolean equals(final Object other) {
        try {
            return compareTo((Path) other) == 0;
        } catch (ClassCastException e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote Includes all the components of the path in a case-sensitive way, except the scheme
     * and the authority.
     */
    @Override
    public int hashCode() {
        // TODO - maybe we should cache (https://github.com/magicDGS/jsr203-http/issues/18)
        int h = fs.hashCode();
        for (int i = 0; i < normalizedPath.length; i++) {
            h = 31 * h + (normalizedPath[i] & 0xff);
        }
        // this is safe for null query and reference
        h = 31 * h + Objects.hash(query, reference);
        return h;
    }

    @Override
    public String toString() {
        // TODO - maybe we should cache (https://github.com/magicDGS/jsr203-http/issues/18)
        // adding scheme, authority and normalized path
        final StringBuilder sb = new StringBuilder(fs.provider().getScheme()) // scheme
                .append("://")
                .append(fs.getAuthority()) // authority
                .append(new String(normalizedPath, HttpUtils.HTTP_PATH_CHARSET));
        if (query != null) {
            sb.append('?').append(query);
        }
        if (reference != null) {
            sb.append('#').append(reference);
        }
        return sb.toString();
    }


    /////////////////////////////////////////
    // helper methods for path as byte[]

    private static byte[] getNormalizedPathBytes(final String path) {
        // TODO - change when we support relative Paths (https://github.com/magicDGS/jsr203-http/issues/12)
        if (!path.isEmpty() && !path.startsWith(HttpUtils.HTTP_PATH_SEPARATOR_STRING)) {
            throw new InvalidPathException(path, "Relative HTTP/S path are not supported");
        }

        if (HttpUtils.HTTP_PATH_SEPARATOR_STRING.equals(path) || path.isEmpty()) {
            return new byte[0];
        }
        final int len = path.length();

        char prevChar = 0;
        for (int i = 0; i < len; i++) {
            char c = path.charAt(i);
            if (isDoubleSeparator(prevChar, c)) {
                return getNormalizedPathBytes(path, len, i - 1);
            }
            prevChar = checkNotNull(path, c);
        }
        if (prevChar == HttpUtils.HTTP_PATH_SEPARATOR_CHAR) {
            return getNormalizedPathBytes(path, len, len - 1);
        }

        return path.getBytes(HttpUtils.HTTP_PATH_CHARSET);
    }

    private static byte[] getNormalizedPathBytes(final String path, final int len,
            final int offset) {
        // get first the last offset
        int lastOffset = len;
        while (lastOffset > 0
                && path.charAt(lastOffset - 1) == HttpUtils.HTTP_PATH_SEPARATOR_CHAR) {
            lastOffset--;
        }
        if (lastOffset == 0) {
            // early termination
            return new byte[] {HttpUtils.HTTP_PATH_SEPARATOR_CHAR};
        }
        // byte output stream
        try (final ByteArrayOutputStream os = new ByteArrayOutputStream(len)) {
            if (offset > 0) {
                os.write(path.substring(0, offset).getBytes(HttpUtils.HTTP_PATH_CHARSET));
            }
            char prevChar = 0;
            for (int i = offset; i < len; i++) {
                char c = path.charAt(i);
                if (isDoubleSeparator(prevChar, c)) {
                    continue;
                }
                prevChar = checkNotNull(path, c);
                os.write(c);
            }

            return os.toByteArray();
        } catch (final IOException e) {
            throw new Utils.ShouldNotHappenException(e);
        }
    }

    private static boolean isDoubleSeparator(final char prevChar, final char c) {
        return c == HttpUtils.HTTP_PATH_SEPARATOR_CHAR
                && prevChar == HttpUtils.HTTP_PATH_SEPARATOR_CHAR;
    }

    private static char checkNotNull(final String path, char c) {
        if (c == '\u0000') {
            throw new InvalidPathException(path, "Null character not allowed in path");
        }
        return c;
    }

    /**
     * Gets the last index to consider in the path bytes.
     *
     * <p>If the lst index is a trailing slash {@link HttpUtils#HTTP_PATH_SEPARATOR_CHAR}, it
     * should not be considered for some operations. This method takes into account that problem.
     *
     * @param path bytes representing the path.
     * @return last index of path to consider.
     */
    private static int getLastIndexWithoutTrailingSlash(final byte[] path) {
        int len = path.length - 1;
        if (len > 0 && path[len] == HttpUtils.HTTP_PATH_SEPARATOR_CHAR) {
            len--;
        }
        return len;
    }
}
