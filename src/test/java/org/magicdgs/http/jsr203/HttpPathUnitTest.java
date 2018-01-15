package org.magicdgs.http.jsr203;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class HttpPathUnitTest extends BaseTest {

    private static final HttpFileSystemProvider TEST_FS_PROVIDER = new HttpFileSystemProvider();
    private static final String TEST_AUTHORITY = "example.com";
    private final HttpFileSystem TEST_FS = new HttpFileSystem(TEST_FS_PROVIDER, TEST_AUTHORITY);

    private static final HttpPath createPathFromUriOnTestProvider(final URI uri) {
        return new HttpPath(
                new HttpFileSystem(TEST_FS_PROVIDER, uri.getAuthority()),
                uri.getPath(), uri.getQuery(), uri.getFragment());
    }

    private static final HttpPath createPathFromUriStringOnTestProvider(final String uriString) {
        return createPathFromUriOnTestProvider(URI.create(uriString));
    }

    @DataProvider
    public Object[][] invalidConstructorArgs() {
        return new Object[][] {
                {"relative_path", TEST_FS, IllegalArgumentException.class},
                {"null_\0_in_path", TEST_FS, InvalidPathException.class}
        };
    }

    @Test(dataProvider = "invalidConstructorArgs")
    public void testInvalidConstruction(final String path, final HttpFileSystem fs, Class<Throwable> exception) {
        Assert.assertThrows(exception, () -> new HttpPath(fs, path, null, null));
    }

    @DataProvider
    public Object[][] authoritiesToTest() {
        return new Object[][] {
                {TEST_AUTHORITY},
                {"example.org"},
                {"hello.worl.net"}
        };
    }

    @Test(dataProvider = "authoritiesToTest")
    public void testGetRoot(final String authority) {
        final HttpFileSystem fs = new HttpFileSystem(TEST_FS_PROVIDER, authority);
        final HttpPath testPath = new HttpPath(fs, "/example.html", null, null);
        // should only be one root, this might fail if the FileSystem returns more
        for (final Path root : fs.getRootDirectories()) {
            assertEqualsPath(root, testPath.getRoot());
        }
    }

    @DataProvider
    public Object[][] validUriStrings() {
        return new Object[][] {
                {"http://example.com"},
                {"http://example.com/index.html"},
                {"http://example.com/file.txt?query=hello+world"},
                {"http://example.com/file.pdf#1"},
                {"http://example.com/file.txt?query=hello+world#2"},
                {"http://example.com/directory/file.gz"},
                {"http://example.com/directory/file.gz?query=hello+world"},
                {"http://example.com/directory/file.pdf#1"},
                {"http://example.com/file.gz?query=hello+world#2"},
        };
    }

    @Test(dataProvider = "validUriStrings")
    public void testToUri(final String uriString) throws MalformedURLException {
        final URI uri = URI.create(uriString);
        final HttpPath path = createPathFromUriOnTestProvider(uri);
        Assert.assertNotSame(path.toUri(), uri);
        Assert.assertEquals(path.toUri(), uri);
        Assert.assertEquals(path.toUri().toURL(), uri.toURL());
    }

    @DataProvider
    public Object[][] compareToUriStrings() {
        // default values for testing
        final String auth1 = "example.com";
        final String file1 = "file1.txt";
        final String query1 = "query=true";
        final String ref1 = "1";
        // against the following
        final String auth2 = "example.org";
        final String file2 = "file2.txt";
        final String query2 = "query=false";
        final String ref2 = "2";


        return new Object[][] {
                // completely equal addresses (incrementing components
                {
                        "http://" + auth1,
                        "http://" + auth1,
                        0
                },
                {
                        "http://" + auth1 + "/" + file1,
                        "http://" + auth1 + "/" + file1,
                        0
                },
                {
                        "http://" + auth1 + "/" + file1 + "?" + query1,
                        "http://" + auth1 + "/" + file1 + "?" + query1,
                        0
                },
                {
                        "http://" + auth1 + "/" + file1 + "?" + query1 + "#" + ref1,
                        "http://" + auth1 + "/" + file1 + "?" + query1 + "#" + ref1,
                        0
                },
                // case-insensitive authority
                {
                        "http://" + auth1.toLowerCase(),
                        "http://" + auth1.toUpperCase(),
                        0
                },
                // authority order
                {
                        "http://" + auth1,
                        "http://" + auth2,
                        auth1.compareTo(auth2)
                },
                // authority order independent of file name
                {
                        "http://" + auth1 + "/" + file1,
                        "http://" + auth2 + "/" + file2,
                        auth1.compareTo(auth2)
                },
                {
                        "http://" + auth1 + "/" + file2,
                        "http://" + auth2 + "/" + file1,
                        auth1.compareTo(auth2)
                },
                // file order for same authority
                {
                        "http://" + auth1 + "/" + file1,
                        "http://" + auth1 + "/" + file2,
                        file1.compareTo(file2)
                },
                // including different lengths (e.g., compressed)
                {
                        "http://" + auth1 + "/" + file1,
                        "http://" + auth1 + "/" + file1 + ".gz",
                        // difference in length (".gz" = 3 chars)
                        -3
                },
                // and it is independent of the query
                {
                        "http://" + auth1 + "/" + file1 + "?" + query1,
                        "http://" + auth1 + "/" + file2 + "?" + query2,
                        file1.compareTo(file2)
                },
                {
                        "http://" + auth1 + "/" + file1 + "?" + query2,
                        "http://" + auth1 + "/" + file2 + "?" + query1,
                        file1.compareTo(file2)
                },
                // query order if case of equal authority and file
                {
                        "http://" + auth1 + "/" + file1 + "?" + query1,
                        "http://" + auth1 + "/" + file1 + "?" + query2,
                        query1.compareTo(query2)
                },
                // and it is independent of the ref
                {
                        "http://" + auth1 + "/" + file1 + "?" + query1 + "#" + ref1,
                        "http://" + auth1 + "/" + file1 + "?" + query2 + "#" + ref2,
                        query1.compareTo(query2)
                },
                {
                        "http://" + auth1 + "/" + file1 + "?" + query1 + "#" + ref2,
                        "http://" + auth1 + "/" + file1 + "?" + query2 + "#" + ref1,
                        query1.compareTo(query2)
                },
                // only different references
                {
                        "http://" + auth1 + "/" + file1 + "?" + query1 + "#" + ref1,
                        "http://" + auth1 + "/" + file1 + "?" + query1 + "#" + ref2,
                        ref1.compareTo(ref2)
                },
        };
    }

    @Test(dataProvider = "compareToUriStrings")
    public void testCompareTo(final String uriString1, final String uriString2, final int result) {
        final HttpPath firstPath = createPathFromUriStringOnTestProvider(uriString1);
        final HttpPath secondPath = createPathFromUriStringOnTestProvider(uriString2);
        Assert.assertEquals(firstPath.compareTo(secondPath), result);
    }

    @Test
    public void testCompareToDifferentProviders() {
        final String path = "/index.html";
        final HttpPath httpPath = new HttpPath(TEST_FS, path, null, null);
        final HttpPath httpsPath = new HttpPath(
                new HttpFileSystem(new HttpsFileSystemProvider(), TEST_AUTHORITY),
                path, null, null);
        Assert.assertThrows(ClassCastException.class, () -> httpPath.compareTo(httpsPath));
    }

    @Test(dataProvider = "compareToUriStrings")
    public void testEquals(final String first, final String second, final int result) {
        final HttpPath firstPath = createPathFromUriStringOnTestProvider(first);
        final HttpPath secondPath = createPathFromUriStringOnTestProvider(second);
        if (result == 0) {
            assertEqualsPath(firstPath, secondPath);
        } else {
            assertNotEqualsPath(firstPath, secondPath);
        }
    }

    @Test
    public void testEqualsDifferentObject() {
        final String uriString = "http://example.com";
        final HttpPath path = createPathFromUriStringOnTestProvider(uriString);
        Assert.assertFalse(path.equals(uriString));
    }

    @Test
    public void testEqualsSamObject() {
        final HttpPath path = createPathFromUriStringOnTestProvider("http://example.com/index.html");
        assertEqualsPath(path, path);
    }

    @Test
    public void testEqualsDifferentProvider() {
        final HttpPath httpPath = createPathFromUriStringOnTestProvider("http://" + TEST_AUTHORITY);
        final HttpPath httpsPath = new HttpPath(
                new HttpFileSystem(new HttpsFileSystemProvider(), TEST_AUTHORITY),
                "", null, null);
        assertNotEqualsPath(httpPath, httpsPath);
    }

    @Test(dataProvider = "validUriStrings")
    public void testHashCodeSameObject(final String uriString) {
        final HttpPath path = createPathFromUriStringOnTestProvider(uriString);
        Assert.assertEquals(path.hashCode(), path.hashCode());
    }

    @Test(dataProvider = "validUriStrings")
    public void testHashCodeEqualObjects(final String uriString) {
        Assert.assertEquals(createPathFromUriStringOnTestProvider(uriString).hashCode(),
                createPathFromUriStringOnTestProvider(uriString).hashCode());
    }

    @Test(dataProvider = "validUriStrings")
    public void testToString(final String uriString) {
        final HttpPath path = createPathFromUriStringOnTestProvider(uriString);
        Assert.assertEquals(path.toString(), uriString);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testToFile() {
        new HttpPath(TEST_FS, "", null, null).toFile();
    }

    @DataProvider
    public Object[][] severalSlashesPaths() {
        return new Object[][] {
                {"//dir//file.txt", "/dir/file.txt"},
                {"/dir//file.txt", "/dir/file.txt"},
                {"//dir/file.txt", "/dir/file.txt"}
        };
    }

    @Test(dataProvider = "severalSlashesPaths")
    public void testNormalizeSeveralSlashes(final String withSlashes, final String withoutSlashes) {
        // this test that, independently of the number of slashes, the stored Path is normalized
        assertEqualsPath(
                new HttpPath(TEST_FS, withSlashes, null, null),
                new HttpPath(TEST_FS, withoutSlashes, null, null));
    }
}
