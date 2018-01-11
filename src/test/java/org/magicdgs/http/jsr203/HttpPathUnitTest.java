package org.magicdgs.http.jsr203;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class HttpPathUnitTest extends BaseTest {

    private final HttpFileSystemProvider TEST_FS_PROVIDER = new HttpFileSystemProvider();
    private final HttpFileSystem TEST_FS = new HttpFileSystem(TEST_FS_PROVIDER);

    @DataProvider
    public Object[][] invalidUris() {
        return new Object[][] {
                // NPE for null FileSystem and URI/URL
                {URI.create("http://example.com"), null, NullPointerException.class},
                {null, TEST_FS, NullPointerException.class},
                // IAE for no authority, and different scheme
                {URI.create("http:/file.txt"), TEST_FS, IllegalArgumentException.class},
                {URI.create("https://example.com/file.txt"), TEST_FS, IllegalArgumentException.class}
                // URI cannot have relative paths
        };
    }

    @Test(dataProvider = "invalidUris")
    public void testIllegalArgsForUriConstructor(final URI uri, final HttpFileSystem fs,
            final Class<Throwable> exceptionClass) {
        Assert.assertThrows(exceptionClass, () -> new HttpPath(uri, fs));
    }

    @DataProvider
    public Object[][] invalidUrls() throws Exception {
        return new Object[][] {
                // NPE for null FileSystem and URI/URL
                {new URL("http://example.com"), null, NullPointerException.class},
                {null, TEST_FS, NullPointerException.class},
                /// IAE for no authority, different scheme and relative path
                {new URL("http:/file.txt"), TEST_FS, IllegalArgumentException.class},
                {new URL("https://example.com/file.txt"), TEST_FS, IllegalArgumentException.class},
                {new URL("http", "example.com", "hello"), TEST_FS, IllegalArgumentException.class}
        };
    }

    @Test(dataProvider = "invalidUrls")
    public void testIllegalArgsForUrlConstructor(final URL url, final HttpFileSystem fs,
            final Class<Throwable> exceptionClass) {
        Assert.assertThrows(exceptionClass, () -> new HttpPath(url, fs));
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
    public void testToUri(final String uriString) {
        final URI uri = URI.create(uriString);
        final HttpPath path = new HttpPath(uri, TEST_FS);
        Assert.assertNotSame(path.toUri(), uri);
        Assert.assertEquals(path.toUri(), uri);
    }

    @Test(dataProvider = "validUriStrings")
    public void testToUriFromURL(final String uriString) throws MalformedURLException {
        final URL url = URI.create(uriString).toURL();
        final HttpPath path = new HttpPath(url, TEST_FS);
        Assert.assertEquals(path.toUri().toURL(), url);
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
    public void testCompareTo(final String first, final String second, final int result) {
        final HttpPath firstPath = new HttpPath(URI.create(first), TEST_FS);
        final HttpPath secondPath = new HttpPath(URI.create(second), TEST_FS);
        Assert.assertEquals(firstPath.compareTo(secondPath), result);
    }

    @Test
    public void testCompareToDifferentProviders() {
        final String uriString = "example.com";
        final HttpPath httpPath = new HttpPath(URI.create("http://" + uriString), TEST_FS);
        final HttpPath httpsPath = new HttpPath(URI.create("https://" + uriString), new HttpFileSystem(new HttpsFileSystemProvider()));
        Assert.assertThrows(ClassCastException.class, () -> httpPath.compareTo(httpsPath));
    }

    @Test(dataProvider = "compareToUriStrings")
    public void testEquals(final String first, final String second, final int result) {
        final HttpPath firstPath = new HttpPath(URI.create(first), TEST_FS);
        final HttpPath secondPath = new HttpPath(URI.create(second), TEST_FS);
        if (result == 0) {
            assertEqualsPath(firstPath, secondPath);
        } else {
            assertNotEqualsPath(firstPath, secondPath);
        }
    }

    @Test
    public void testEqualsDifferentObject() {
        final String uriString = "http://example.com";
        final HttpPath path = new HttpPath(URI.create(uriString), TEST_FS);
        Assert.assertFalse(path.equals(uriString));
    }

    @Test
    public void testEqualsSamObject() {
        final HttpPath path = new HttpPath(URI.create("http://example.com/index.html"), TEST_FS);
        assertEqualsPath(path, path);
    }

    @Test
    public void testEqualsDifferentProvider() {
        final String uriString = "example.com";
        final HttpPath httpPath = new HttpPath(URI.create("http://" + uriString), TEST_FS);
        final HttpPath httpsPath = new HttpPath(URI.create("https://" + uriString), new HttpFileSystem(new HttpsFileSystemProvider()));
        assertNotEqualsPath(httpPath, httpsPath);
    }

    @Test(dataProvider = "validUriStrings")
    public void testHashCodeSameObject(final String uriString) {
        final HttpPath path = new HttpPath(URI.create(uriString), TEST_FS);
        Assert.assertEquals(path.hashCode(), path.hashCode());
    }

    @Test(dataProvider = "validUriStrings")
    public void testHashCodeEqualObjects(final String uriString) {
        Assert.assertEquals(new HttpPath(URI.create(uriString), TEST_FS).hashCode(),
                new HttpPath(URI.create(uriString), TEST_FS).hashCode());
    }

    @Test(dataProvider = "validUriStrings")
    public void testToString(final String uriString) {
        final HttpPath path = new HttpPath(URI.create(uriString), TEST_FS);
        Assert.assertEquals(path.toString(), uriString);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testToFile() {
        new HttpPath(URI.create("http://example.com"), TEST_FS).toFile();
    }

    @DataProvider
    public Object[][] severalSlashesUris() {
        return new Object[][] {
                {"http://example.com//dir//file.txt", "http://example.com/dir/file.txt"},
                {"http://example.com/dir//file.txt", "http://example.com/dir/file.txt"},
                {"http://example.com//dir/file.txt", "http://example.com/dir/file.txt"}
        };
    }

    @Test(dataProvider = "severalSlashesUris")
    public void testNormalizeSeveralSlashes(final String withSlashes, final String withoutSlashes) {
        // this test that, independently of the number of slashes, the stored Path is normalized
        final URI severalSlashesUri = URI.create(withSlashes);
        final URI oneSlashUri = URI.create(withoutSlashes);
        Assert.assertNotEquals(severalSlashesUri, oneSlashUri, "URIs shouldn't be equal for this test");
        assertEqualsPath(
                new HttpPath(severalSlashesUri, TEST_FS),
                new HttpPath(oneSlashUri, TEST_FS));
    }
}
