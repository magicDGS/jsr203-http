package org.magicdgs.http.jsr203;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.util.Collections;
import java.util.Map;

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
