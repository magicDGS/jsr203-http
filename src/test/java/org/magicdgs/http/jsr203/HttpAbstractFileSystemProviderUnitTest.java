package org.magicdgs.http.jsr203;

import org.mockito.internal.util.collections.Sets;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class HttpAbstractFileSystemProviderUnitTest extends BaseTest {

    private static final URI TEST_BASE_URI = URI.create("http://example.com");

    private static final Map<String, ?> TEST_ENV = Collections.emptyMap();


    @Test
    public void testNewFileSystem() throws IOException {
        final HttpFileSystemProvider provider = new HttpFileSystemProvider();
        final HttpFileSystem fs = provider.newFileSystem(TEST_BASE_URI, TEST_ENV);

        // test the returned filesystem
        Assert.assertNotNull(fs);
        Assert.assertSame(fs.provider(), provider, "provider");
        Assert.assertEquals(fs.getAuthority(), TEST_BASE_URI.getAuthority(), "authority");

        // assert that generating a new FileSystem for the same URI throws
        Assert.assertThrows(FileSystemAlreadyExistsException.class,
                () -> provider.newFileSystem(TEST_BASE_URI, TEST_ENV));
    }

    @Test
    public void testGetFileSystem() throws IOException {
        final HttpFileSystemProvider provider = new HttpFileSystemProvider();

        // first test that a no created FS throws an exception
        Assert.assertThrows(FileSystemNotFoundException.class,
                () -> provider.getFileSystem(TEST_BASE_URI));

        // creates a new FileSystem and assert that it is not null
        final HttpFileSystem fs = provider.newFileSystem(TEST_BASE_URI, TEST_ENV);
        Assert.assertNotNull(fs);

        // test that it returns the same for the URI used
        Assert.assertSame(provider.getFileSystem(TEST_BASE_URI), fs);
    }

    @DataProvider
    public Object[][] pathStrings() {
        return new Object[][] {
                {"/"},
                {"/file.txt"},
                {"/dir/file.txt"},
                {"/file.txt?query=1+1"},
                {"/file.txt#3"}
        };
    }

    @Test(dataProvider = "pathStrings")
    public void testGetPath(final String path) throws IOException {
        final URI uri = URI.create(String.format("%s://%s%s",
                TEST_BASE_URI.getScheme(), TEST_BASE_URI.getAuthority(), path));

        final HttpFileSystemProvider provider = new HttpFileSystemProvider();

        // expected Path
        final HttpPath expected = new HttpPath(provider.newFileSystem(TEST_BASE_URI, TEST_ENV),
                uri.getPath(), uri.getQuery(), uri.getFragment());
        // actual Path
        final HttpPath actual = provider.getPath(uri);

        // test that the new Path is not the same, but equal to the other
        Assert.assertNotSame(actual, expected);
        assertEqualsPath(actual, expected);
    }

    @DataProvider
    public Object[][] invalidUris() {
        return new Object[][] {
                // null URI
                {null, IllegalArgumentException.class},
                // URI without authority (e.g., file URI)
                {new File("/example.txt").toURI(), IllegalArgumentException.class},
                // URI with different scheme
                {URI.create("ftp://example.org/file.txt"), ProviderMismatchException.class}
        };
    }

    @Test(dataProvider = "invalidUris")
    public void testInvalidUris(final URI uri, Class<? extends Throwable> expectedException) throws Exception {
        Assert.assertThrows(expectedException, () -> new HttpFileSystemProvider().getPath(uri));
    }

    @DataProvider
    public Object[][] invalidArgsForByteChannel() {
        final HttpFileSystemProvider http = new HttpFileSystemProvider();
        final HttpsFileSystemProvider https = new HttpsFileSystemProvider();
        final HttpPath httpPath = http.getPath(URI.create("http://example.org/file.txt"));
        return new Object[][] {
                // null path or options
                {http, null, Collections.emptySet(), IllegalArgumentException.class},
                {http, httpPath, null, IllegalArgumentException.class},
                // mismatching Path
                {http, https.getPath(URI.create("https://example.org/file.txt")), Collections.emptySet(), ProviderMismatchException.class},
                // non existent file
                {http, httpPath, Collections.emptySet(), NoSuchFileException.class},
                // UNSUPPORTED BYTE CHANNELS (e.g., writing)
                // if only an option that it is not supported
                {http, httpPath, Collections.singleton(StandardOpenOption.WRITE), UnsupportedOperationException.class},
                // if two options, even if the supported one is requested
                {http, httpPath, Sets.newSet(StandardOpenOption.READ, StandardOpenOption.APPEND), UnsupportedOperationException.class}
        };
    }

    @Test(dataProvider = "invalidArgsForByteChannel")
    public void testInvalidByteChannels(final HttpAbstractFileSystemProvider provider, Path path,
            final Set<OpenOption> options,final Class<? extends Throwable> expectedException)
            throws Exception {
        Assert.assertThrows(expectedException, () -> provider.newByteChannel(path, options));
    }

    @DataProvider
    public Object[][] deniedAccess() {
        return new Object[][] {
            {AccessMode.EXECUTE},
            {AccessMode.WRITE}
        };
    }

    @Test(dataProvider= "deniedAccess", expectedExceptions = AccessDeniedException.class)
    public void testCheckAccessDenied(final AccessMode deniedAcces) throws Exception {
        final HttpsFileSystemProvider https = new HttpsFileSystemProvider();
        final HttpPath path = https.getPath(getGithubPagesFileUrl("file1.txt").toURI());
        https.checkAccess(path, deniedAcces);
    }

    @Test
    public void testCheckAccessRead() throws Exception {
        final HttpsFileSystemProvider https = new HttpsFileSystemProvider();
        final HttpPath path = https.getPath(getGithubPagesFileUrl("file1.txt").toURI());
        // this shouldn't throw
        https.checkAccess(path, AccessMode.READ);
    }

    @Test(expectedExceptions = NoSuchFileException.class)
    public void testCheckAccessNoSuchFile() throws Exception {
        final HttpsFileSystemProvider https = new HttpsFileSystemProvider();
        final HttpPath path = https.getPath(getGithubPagesFileUrl("no_exists.txt").toURI());
        https.checkAccess(path, AccessMode.READ);
    }

    // test provider for unsupported operations
    private static final HttpAbstractFileSystemProvider TEST_NULL_PROVIDER =
            new HttpAbstractFileSystemProvider() {
                @Override
                public String getScheme() {
                    // return null scheme
                    return null;
                }
            };

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testCreateDirectoryIsUnsupported() throws Exception {
        TEST_NULL_PROVIDER.createDirectory(null);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testDeleteIsUnsupported() throws Exception {
        TEST_NULL_PROVIDER.delete(null);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testMoveIsUnsupported() throws Exception {
        TEST_NULL_PROVIDER.move(null, null);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testSetAttributeIsUnsupported() throws Exception {
        TEST_NULL_PROVIDER.setAttribute(null, null, null);
    }
}
