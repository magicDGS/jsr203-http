package org.magicdgs.http.jsr203;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;

/**
 * {@link Path} for HTTP/S.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
final class HttpPath implements Path {

    // logger for the class
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpPath.class);

    // file system (indicates the scheme - HTTP or HTTPS)
    private final HttpFileSystem fs;

    // authority part for the URL
    private final String authority;

    // path and offsets of separator - similar to other implementation of Path
    private final byte[] normalizedPath;
    private volatile int[] offsets;

    // query for the URL (may be null)
    private final String query;
    // reference for the URL (may be null) / fragment for the URI representation
    private final String reference;

    /**
     * Constructor for an URL.
     *
     * @param uri HTTP/S URI.
     * @param fs file system used to create the path.
     */
    HttpPath(final URI uri, final HttpFileSystem fs) {
        if (uri == null) {
            throw new IllegalArgumentException("Null URI");
        }
        if (fs == null) {
            throw new IllegalArgumentException("Null FS");
        }

        // store the FileSystem
        this.fs = fs;

        // TODO - check that the scheme is the same as the HtppFileSystem
        // TODO - although maybe it should be tested before...
        // TODO - at least it shouldn't be null
        // uri.getScheme();

        // TODO - authority should be not null?
        this.authority = uri.getAuthority();

        // optional part of the URL
        this.query = uri.getQuery();
        this.reference = uri.getFragment();

        // get the normalized path
        this.normalizedPath = getNormalizedPathBytes(uri.getPath());
    }

    /**
     * Constructor for an URL.
     *
     * @param url HTTP/S URL.
     * @param fs file system used to create the path.
     */
    // TODO - is this really necessary?
    HttpPath(final URL url, final HttpFileSystem fs) {
        if (url == null) {
            throw new IllegalArgumentException("Null URL");
        }

        if (fs == null) {
            throw new IllegalArgumentException("Null FS");
        }

        // store the FileSystem
        this.fs = fs;

        // TODO - check that the scheme is the same as the HtppFileSystem
        // TODO - although maybe it should be tested before...
        // TODO - at least it shouldn't be null
        // url.getProtocol();

        // TODO - authority should be not null?
        // TODO - should throw InvalidPathException if not
        this.authority = url.getAuthority();

        // optional part of the URL
        this.query = url.getQuery();
        this.reference = url.getRef();

        // get the normalized path
        this.normalizedPath = getNormalizedPathBytes(url.getPath());
    }


    /**
     * Internal constructor.
     *
     * @param fs file system. Shouldn't be {@code null}.
     * @param authority authority. Shouldn't be {@code null}.
     * @param query query. May be {@code null}.
     * @param reference reference. May be {@code null}.
     * @param normalizedPath normalized path (as a byte array). Shouldn't be {@code null}.
     *
     * @implNote does not perform any check for efficiency.
     */
    private HttpPath(final HttpFileSystem fs, final String authority,
            final String query, final String reference,
            final byte... normalizedPath) {
        this.fs = fs;
        this.authority = authority;

        // optional query and reference components (may be null)
        this.query = query;
        this.reference = reference;

        // normalized path bytes (shouldn't be null)
        this.normalizedPath = normalizedPath;

        // TODO - remove this once the relative Path is supported
        if (!isAbsolute()) {
            LOGGER.warn(
                    "Relative HTTP/S path {} is not completely functional in the current implementation. This limitation might be removed in the future",
                    this);
        }
    }

    @Override
    public FileSystem getFileSystem() {
        return fs;
    }

    @Override
    public boolean isAbsolute() {
        return this.normalizedPath.length == 0
                || normalizedPath[0] == HttpUtils.HTTP_PATH_SEPARATOR_CHAR;
    }

    @Override
    public HttpPath getRoot() {
        // always root (authority)
        return new HttpPath(fs, authority, null, null);
    }

    @Override
    public HttpPath getFileName() {
        final int count = getNameCount();
        // no filename for no elements
        if (count == 0) {
            return null;
        }

        // already filename path representation
        if (count == 1 && normalizedPath.length > 0
                && normalizedPath[0] != HttpUtils.HTTP_PATH_SEPARATOR_CHAR) {
            return this;
        }

        // get a copy of the byte[] path from the last offset to the end
        final int lastOffset = offsets[count - 1] + 1;
        final int len = normalizedPath.length - lastOffset;
        final byte[] filename = new byte[len];
        System.arraycopy(normalizedPath, lastOffset, filename, 0, len);

        // propagate the query and the reference, because it belongs to the file
        return new HttpPath(fs, authority, query, reference, filename);
    }

    @Override
    public Path getParent() {
        final int count = getNameCount();
        // no parent for no elements
        if (count == 0) {
            return null;
        }

        final int len = offsets[count - 1];

        // the parent is the root if the length is zero
        if (len <= 0) {
            return getRoot();
        }

        final byte[] parent = new byte[len];
        System.arraycopy(normalizedPath, 0, parent, 0, len);

        // TODO - propagate query and reference?
        return new HttpPath(fs, authority, null, null, parent);

    }

    @Override
    public int getNameCount() {
        initOffsets();
        return offsets.length;
    }

    @Override
    public Path getName(final int index) {
        final int count = getNameCount();
        if (count == 0 || index < 0 || index >= count) {
            throw new IllegalArgumentException("Invalid index: " + index);
        }

        final int begin = offsets[index] + (index == 0 ? 0 : 1);
        final int len;
        if (index == (offsets.length - 1)) {
            len = normalizedPath.length - begin;
        } else {
            len = offsets[index + 1] - begin - 1;
        }

        final byte[] name = new byte[len];
        System.arraycopy(normalizedPath, begin, name, 0, len);

        // TODO - propagate query and reference?
        // TODO - at least this should return the same as the getFileName for the index of the name
        return new HttpPath(fs, authority, null, null, name);

    }

    @Override
    public Path subpath(final int beginIndex, final int endIndex) {
        final int count = getNameCount();
        if (beginIndex < 0 || beginIndex >= count || endIndex > count || beginIndex >= endIndex) {
            throw new IllegalArgumentException();
        }

        // starting offset and length
        final int begin = offsets[beginIndex];
        final int len;
        if (endIndex == offsets.length) {
            len = normalizedPath.length - begin;
        } else {
            len = offsets[endIndex] - begin - 1;
        }

        byte[] subpath = new byte[len];
        System.arraycopy(normalizedPath, begin, subpath, 0, len);

        // TODO - propagate query and reference?
        // TODO - at least this should return the same as the getFileName for the index of the name
        return new HttpPath(fs, authority, null, null, subpath);
    }

    @Override
    public boolean startsWith(final Path other) {
        // as the contract says - different provider return false
        if (!(other instanceof HttpPath)
                || this.getFileSystem().provider() != other.getFileSystem().provider()) {
            return false;
        }
        final HttpPath that = (HttpPath) other;

        // both should be absolute or not
        // both should haveearly termination for different absolute status or other larger than this
        if (this.isAbsolute() != that.isAbsolute()
                // the other cannot have a larger path that this to start with
                || that.normalizedPath.length > this.normalizedPath.length
                // both authority should be the same to start with the same path
                || !this.authority.equals(that.authority)) {
            return false;
        }

        // check the bytes of the normalized path
        int i;
        for (i = 0; i < that.normalizedPath.length; i++) {
            if (this.normalizedPath[i] != that.normalizedPath[i]) {
                return false;
            }
        }

        // finally check the name boundary
        return i >= this.normalizedPath.length
                || this.normalizedPath[i] == HttpUtils.HTTP_PATH_SEPARATOR_CHAR;
    }

    @Override
    public boolean startsWith(final String other) {
        // if we want to test if  http://example.com/file.txt starts with "file.txt" then this
        // method should fail with InvalidPathException, because the scheme and authority aren't
        // provided and thus it cannot be tested
        // TODO - maybe we can relax this by returning false by anything that cannot be converted
        // TODO - into an HttpPath with the instance FileSystem to mimic the behaviour of endsWith
        return startsWith(fs.getPath(other));
    }

    @Override
    public boolean endsWith(final Path other) {
        // as the contract says - different provider return false
        if (!(other instanceof HttpPath)
                || this.getFileSystem().provider() != other.getFileSystem().provider()) {
            return false;
        }
        final HttpPath that = (HttpPath) other;

        // both should be absolute or not
        if (that.isAbsolute() && !this.isAbsolute()) {
            return false;
        }

        // finally, compare the path component
        return endsWith(that.normalizedPath);
    }

    @Override
    public boolean endsWith(final String other) {
        // first try to parse the other as an valid URL for converting to a HTTP/S path
        try {
            return endsWith(fs.getPath(other));
        } catch (final InvalidPathException e) {
            // in the case that "other" is not a valid HTTP/S URL, treat it as a Path component
            // this will allow to test if http://example.com/directory/file.txt ends with "file.txt"
            // TODO - the contract suggest to throw InvalidPathException if the String cannot be
            // TODO - converted into a Path and this is not completely true with this implementation
            // TODO - e.g., testing if https://example.com/file.txt ends with "file://file.txt"
            // TODO - should throw because there is a mismatch with the providers
            // TODO - I do not think that this is an issue, but might be a problem for users
            return endsWith(getNormalizedPathBytes(other));
        }
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
        int olast = other.length - 1;
        if (olast > 0 && other[olast] == '/') {
            olast--;
        }

        // get the last index to check
        int last = this.normalizedPath.length - 1;
        if (last > 0 && this.normalizedPath[last] == '/') {
            last--;
        }

        // early termination if the length is 0
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

        // final check for name boundary
        return other[olast + 1] == HttpUtils.HTTP_PATH_SEPARATOR_CHAR
                || last == -1 || this.normalizedPath[last] == HttpUtils.HTTP_PATH_SEPARATOR_CHAR;
    }

    @Override
    public Path normalize() {
        // TODO - implement relative HTTP/S path?
        LOGGER.warn("Normalizing HTTP/S paths does not have any effect in the current implementation: as a consequence, relative HTTP/S paths are not supported.");
        return this;
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
        /// TODO - implement relative HTTP/S path?
        LOGGER.warn("HTTP/S paths cannot be relative in the current implementation. Returning the same path ({}) independently of {}", this, other);
        return this;
    }

    @Override
    public URI toUri() {
        try {
            return new URI(fs.provider().getScheme(), authority,
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
        LOGGER.error("Non absolute HTTP/S paths are not supported current implementation. Unable to retrieve absolute path for {}", this);
        throw new UnsupportedOperationException();
    }

    @Override
    public Path toRealPath(final LinkOption... options) throws IOException {
        if (options.length == 0) {
            // TODO - this might be easy to implement if we can get where re-directs are pointing
            // TODO - but maybe it is difficult if an HTTP/S server contains a file that it's a
            // TODO - symbolic link. In that case, I do not known if it is possible to find the file
            // TODO - without download it
            LOGGER.warn("Following symbolic links for HTTP/S paths is not supported (e.g., re-directions)");
        }
        // TODO - implement contract behaviour:
        // TODO - implement check for existence of the URL and throw IOException if it does not
        // TODO - without following re-directions
        LOGGER.warn("HTTP/S real path retrieval for {} is not checked for existence in the current implementation", this);
        return this;
    }

    /** Unsupported method. */
    @Override
    public File toFile() {
        throw new UnsupportedOperationException(this.getClass() + " cannot be converted to a File");
    }

    @Override
    public WatchKey register(final WatchService watcher, final WatchEvent.Kind<?>[] events,
            final WatchEvent.Modifier... modifiers) throws IOException {
        // TODO - support WatchKeys?
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchKey register(final WatchService watcher, final WatchEvent.Kind<?>... events)
            throws IOException {
        // TODO - support WatchKeys?
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Path> iterator() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int compareTo(final Path other) {
        if (this == other) {
            return 0;
        }

        final HttpPath httpOther = (HttpPath) other;
        // object comparison - should be from the same provider
        if (this.fs != httpOther.fs) {
            throw new ClassCastException();
        }

        // first check the authority (case insensitive)
        int comparison = this.authority.compareToIgnoreCase(httpOther.authority);
        if (comparison != 0) {
            return comparison;
        }

        // then check the path
        final int len1 = normalizedPath.length;
        final int len2 = httpOther.normalizedPath.length;
        final int n = Math.min(len1, len2);
        for(int k = 0; k < n; k++) {
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
        return Comparator.nullsFirst(String::compareTo).compare(this.reference, httpOther.reference);
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof Path) {
            return compareTo((Path) other) == 0;
        }
        return false;
    }

    // TODO - should cache lazily?
    @Override
    public int hashCode() {
        // TODO - should make lower case for hashCode?
        int h = authority.hashCode();
        for (int i = 0; i < normalizedPath.length; i++) {
            h = 31 * h + (normalizedPath[i] & 0xff);
        }
        // this is safe for null query and reference
        h = 31 * h + Objects.hash(query, reference);
        return h;
    }

    @Override
    public String toString() {
        // TODO - should cache lazily?
        final StringBuilder sb = new StringBuilder(authority);
        if (!isAbsolute()) {
            // relative indicator - TODO should be documented or somewhere
            sb.append('~');
        }
        sb.append(new String(normalizedPath, HttpUtils.HTTP_PATH_CHARSET));
        return sb.toString();
    }


    // create offset list if not already created
    // should be called after normalize
    private void initOffsets() {
        if (offsets == null) {
            // count names
            int count = 0;
            int index = 0;
            while (index < normalizedPath.length) {
                final byte c = normalizedPath[index++];
                if (c == HttpUtils.HTTP_PATH_SEPARATOR_CHAR) {
                    count++;
                    index++;
                }
            }
            // populate offsets
            final int[] result = new int[count];
            count = 0;
            index = 0;
            while (index < normalizedPath.length) {
                final byte c = normalizedPath[index];
                if (c == HttpUtils.HTTP_PATH_SEPARATOR_CHAR) {
                    result[count++] = index++;
                    index++;
                } else {
                    // assumes that redundant separators are already removed
                    index++;
                }
            }
            synchronized (this) {
                if (offsets == null)
                    offsets = result;
            }
            // should check if DEBUG is enabled to do not construct the Strings for the normalized
            // path and the offsets
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Initialized offsets for {}: {}",
                        new String(normalizedPath, HttpUtils.HTTP_PATH_CHARSET),
                        Arrays.toString(offsets));
            }
        }
    }


    /////////////////////////////////////////
    // helper method for path as byte[]

    private static byte[] getNormalizedPathBytes(final String path) {
        if (HttpUtils.HTTP_PATH_SEPARATOR_STRING.equals(path) || path.isEmpty()) {
            return new byte[0];
        }
        final int len = path.length();

        char prevChar = 0;
        for(int i = 0; i < len; i++) {
            char c = path.charAt(i);
            if (isDoubleSeparator(prevChar, c)) {
                return getNormalizedPathBytes(path, len, i - 1);
            }
            prevChar = checkNotNul(path, c);
        }
        if (prevChar == HttpUtils.HTTP_PATH_SEPARATOR_CHAR) {
            return getNormalizedPathBytes(path, len, len - 1);
        }

        return path.getBytes(HttpUtils.HTTP_PATH_CHARSET);
    }

    private static byte[] getNormalizedPathBytes(final String path, final int len, final int offset) {
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
                prevChar = checkNotNul(path, c);
                os.write(c);
            }

            return os.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Should not happen");
        }
    }

    private static boolean isDoubleSeparator(final char prevChar, final char c) {
        return c == HttpUtils.HTTP_PATH_SEPARATOR_CHAR
                && prevChar == HttpUtils.HTTP_PATH_SEPARATOR_CHAR;
    }

    private static char checkNotNul(String input, char c) {
        if (c == '\u0000') {
            throw new InvalidPathException(input, "Nul character not allowed");
        }
        return c;
    }
}
