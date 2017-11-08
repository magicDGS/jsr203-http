package org.magicdgs.http.jsr203;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;

/**
 * Implementation for a {@link SeekableByteChannel} for {@link URL} open as a connection.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 * @implNote this seekable byte channel is read-only..
 */
class URLSeekableByteChannel implements SeekableByteChannel {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // url and proxy for the file
    private final URL url;

    // current position of the SeekableByteChannel
    private long position = 0;

    // the size of the whole file (-1 is not initialized)
    private long size = -1;

    private ReadableByteChannel channel = null;
    private InputStream backedStream = null;


    URLSeekableByteChannel(final URL url) throws IOException {
        this.url = url;
        // and instantiate the stream/channel at position 0
        instantiateChannel(this.position);
    }

    @Override
    public int read(final ByteBuffer dst) throws IOException {
        final int read = channel.read(dst);
        this.position += read;
        return read;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        throw new NonWritableChannelException();
    }

    @Override
    public long position() throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
        return position;
    }

    @Override
    public URLSeekableByteChannel position(long newPosition) throws IOException {
        if (newPosition < 0) {
            throw new IllegalArgumentException("Cannot seek a negative position");
        }
        if (!isOpen()) {
            throw new ClosedChannelException();
        }

        if (this.position < newPosition) {
            // if the current position is before, do not open a new connection
            // but skip the bytes until the new position
            final long bytesToSkip = newPosition - this.position;
            backedStream.skip(bytesToSkip);
        } else if (this.position > newPosition) {
            // in this case, we require to re-instantiate the channel
            // opening at the new position - and closing the previous
            close();
            instantiateChannel(newPosition);
        }

        // updates to the new position
        this.position = newPosition;

        return this;
    }

    @Override
    public long size() throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
        if (size == -1) {
            final URLConnection connection = url.openConnection();
            connection.connect();
            // try block for always disconnect the connection
            try {
                size = connection.getContentLengthLong();
                // if the size is still -1, it means that it is unavailable
                if (size == -1) {
                    throw new IOException("Unable to retrieve content length for " + url);
                }
            } finally {
                // disconnect if possible
                disconnect(connection);
            }
        }
        return size;
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        throw new NonWritableChannelException();
    }

    @Override
    public boolean isOpen() {
        return channel.isOpen();
    }

    @Override
    public void close() throws IOException {
        // this should close also the backed stream
        channel.close();
    }

    // open a readable byte channel for the requrested position
    private void instantiateChannel(final long position) throws IOException {
        final URLConnection connection = url.openConnection();
        if (position > 0) {
            logger.debug("Setting Range to {}", position);
            // set the range if the position is different from 0
            connection.setRequestProperty("Range", String.format("bytes=%s-", position));
        }
        // get the channel from the backed stream
        backedStream = connection.getInputStream();
        channel = Channels.newChannel(backedStream);
    }

    /**
     * Disconnects the {@link URLConnection} if it is an instance of {@link HttpURLConnection}.
     *
     * @param connection the connection to be disconnected.
     */
    private static void disconnect(final URLConnection connection) {
        if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection) connection).disconnect();
        }
    }
}
