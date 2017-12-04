package org.magicdgs.http.jsr203;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.function.UnaryOperator;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class HttpPathUnitTest extends BaseTest {

    // FileSystem for testing (HTTP, so no HTTPS urls)
    private static final HttpFileSystem HTTP_FILE_SYSTEM = new HttpFileSystem(new HttpFileSystemProvider());

    @Test
    public void testNormalizeSeveralSlashes() throws Exception {
        final URI uri = new URI("http://example.com//directory//file.txt");
        final HttpPath path = new HttpPath(uri, HTTP_FILE_SYSTEM);
        Assert.assertNotEquals(path.toUri(), uri);
        Assert.assertEquals(path.toUri(), new URI("http://example.com/directory/file.txt"));
        Assert.assertEquals(path.toString(), "example.com/directory/file.txt");
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testToFile() throws Exception {
        // get an example file (does not need to exist
        new HttpPath(new URI("http://example.com/"), HTTP_FILE_SYSTEM)
                .toFile();
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
    public void testCompareTo(final String first, final String second, final int result)
            throws Exception {
        final HttpPath firstPath = new HttpPath(new URI(first), HTTP_FILE_SYSTEM);
        final HttpPath secondPath = new HttpPath(new URI(second), HTTP_FILE_SYSTEM);
        Assert.assertEquals(firstPath.compareTo(secondPath), result);
    }
}
