package org.magicdgs.http.jsr203;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class URLSeekableByteChannelUnitTest extends BaseTest {

    // helper method to get the GitHub pages as an URL object
    private static URL getGithubPagesUrl(final String fileName) {
        try {
            return new URL(getGithubPagesFileUrl(fileName));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Should not happen", e);
        }
    }

    // helper method to get teh Docs files as an URL object
    private static Path getDocsPath(final String fileName) {
        return new File(getPathFromLocalDocsFile(fileName)).toPath();
    }


    @Test(expectedExceptions = FileNotFoundException.class)
    public void testNonExistentUrl() throws Exception {
        new URLSeekableByteChannel(getGithubPagesUrl("not_existent.txt"));
    }

    @Test
    public void testUnsupportedMethods() throws Exception {
        try (final URLSeekableByteChannel channel =
                new URLSeekableByteChannel(getGithubPagesUrl("file1.txt"))) {
            // cannot write
            Assert.assertThrows(NonWritableChannelException.class,
                    () -> channel.write(ByteBuffer.allocate(10)));
            // cannot truncate
            Assert.assertThrows(NonWritableChannelException.class, () -> channel.truncate(10));
        }
    }

    @Test(dataProvider = "getDocsFilesForTesting", dataProviderClass = GitHubResourcesIntegrationTest.class)
    public void testSizeFromResources(final String fileName) throws Exception {
        final URL urlFile = getGithubPagesUrl(fileName);
        final Path localFile = getDocsPath(fileName);
        try (final URLSeekableByteChannel urlChannel = new URLSeekableByteChannel(urlFile);
                final SeekableByteChannel localChannel = Files.newByteChannel(localFile,
                        StandardOpenOption.READ)) {
            Assert.assertEquals(urlChannel.size(), localChannel.size());
        }
    }

    @Test
    public void testGetPosition() throws Exception {
        // open channel
        try (final URLSeekableByteChannel channel = new URLSeekableByteChannel(
                getGithubPagesUrl("file1.txt"))) {
            int currentPosition = 0;
            Assert.assertEquals(channel.position(), currentPosition);
            final int bufferSize = Math.round(channel.size() / 10f);
            final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            for (int i = bufferSize; i <= channel.size(); i += bufferSize) {
                channel.read(buffer);
                Assert.assertEquals(channel.position(), i);
                buffer.rewind();
            }
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testIllegalPosition() throws Exception {
        new URLSeekableByteChannel(getGithubPagesUrl("file1.txt")).position(-1);
    }

    @DataProvider
    public Iterator<Object[]> seekData() {
        return Stream.of(GitHubResourcesIntegrationTest.getDocsFilesForTesting())
                .map(data -> (String) data[0]).map(fileName ->
                        new Object[] {
                                getGithubPagesUrl(fileName),
                                10,
                                getDocsPath(fileName)
                        }).iterator();
    }

    @Test(dataProvider = "seekData")
    public void testSeekBeforeRead(final URL testUrl, final long position, final Path localFile)
            throws Exception {
        try (final URLSeekableByteChannel actual = new URLSeekableByteChannel(testUrl);
                final SeekableByteChannel expected = Files.newByteChannel(localFile)) {
            testReadSize((int) expected.size(),
                    actual.position(position),
                    expected.position(position));
        }
    }

    @Test(dataProvider = "seekData")
    public void testSeekToBeginning(final URL testUrl, final long position, final Path localFile)
            throws Exception {
        try (final URLSeekableByteChannel actual = new URLSeekableByteChannel(testUrl);
                final SeekableByteChannel expected = Files.newByteChannel(localFile)) {
            testReadSize((int) expected.size(),
                    // first position and then come back to 0
                    actual.position(position).position(0),
                    expected);

            // assert that the size is the same after seek
            Assert.assertEquals(actual.size(), expected.size());
        }
    }

    @Test(dataProvider = "seekData")
    public void testSeekToSamePosition(final URL testUrl, final long position, final Path localFile)
            throws Exception {
        try (final URLSeekableByteChannel actual = new URLSeekableByteChannel(testUrl);
                final SeekableByteChannel expected = Files.newByteChannel(localFile)) {
            testReadSize((int) expected.size(),
                    // seek twice to the same position is equal to seek only once
                    actual.position(position).position(position),
                    expected.position(position));

            // assert that the size is the same after seek
            Assert.assertEquals(actual.size(), expected.size());
        }
    }

    @Test(dataProvider = "seekData")
    public void testSeekShouldReopen(final URL testUrl, final long position, final Path localFile)
            throws Exception {
        try (final URLSeekableByteChannel actual = new URLSeekableByteChannel(testUrl);
                final SeekableByteChannel expected = Files.newByteChannel(localFile)) {
            testReadSize((int) expected.size(),
                    // seek first to 10 bytes more, and then to the requested position
                    actual.position(position + 10).position(position),
                    expected.position(position));

            // assert that the size is the same after seek
            Assert.assertEquals(actual.size(), expected.size());
        }
    }

    private static void testReadSize(final int size,
            final URLSeekableByteChannel actual, final SeekableByteChannel expected)
            throws Exception {
        final ByteBuffer expectedBuffer = ByteBuffer.allocate(size);
        final ByteBuffer actualBuffer = ByteBuffer.allocate(size);
        Assert.assertEquals(
                // seek to position and then to the beginning of the file again
                actual.read(expectedBuffer),
                expected.read(actualBuffer),
                "different number of bytes read");
        expectedBuffer.rewind();
        actualBuffer.rewind();
        Assert.assertEquals(expectedBuffer.array(), actualBuffer.array(),
                "different byte[] after read");
    }

    @Test
    public void testClose() throws Exception {
        // open channel
        final URLSeekableByteChannel channel =
                new URLSeekableByteChannel(getGithubPagesUrl("file1.txt"));
        Assert.assertTrue(channel.isOpen());
        // close channel
        channel.close();
        Assert.assertFalse(channel.isOpen());

        // assert that several methods thrown with the corresponding exception
        // 1. position
        Assert.assertThrows(ClosedChannelException.class, () -> channel.position());
        Assert.assertThrows(ClosedChannelException.class, () -> channel.position(10));
        // 2. size
        Assert.assertThrows(ClosedChannelException.class, () -> channel.size());
        // 3. read
        Assert.assertThrows(ClosedChannelException.class,
                () -> channel.read(ByteBuffer.allocate(1)));
    }
}