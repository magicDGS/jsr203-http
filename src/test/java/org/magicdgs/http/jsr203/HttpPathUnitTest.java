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

    // TODO - create a real FileSytem
    private static final HttpFileSystem HTTP_FILE_SYSTEM =
            new HttpFileSystem(new HttpFileSystemProvider());

    // helper method to create an HttpPath from a File.toPath and the URI root
    private static HttpPath createHttpPath(final Path pathWithinRoot, final URI root)
            throws URISyntaxException {
        // create the URL string
        String url = root.toString();
        if (!pathWithinRoot.startsWith("/")) {
            url += "/";
        }
        url += pathWithinRoot.toString();

        final URI asUri = new URI(url);
        return new HttpPath(asUri, HTTP_FILE_SYSTEM);
    }

    /**
     * Gets test data for concordance between default {@link Path} methods and {@link HttpPath}.
     *
     * <p> uses {@link File#toPath()} objects to get teh path within the root, and a root
     * {@link URI} where the path is located. For example, <b>http://example.com/file.txt</b>
     * will be represented in this test data as <b>file.txt</b> ({@code File("file.txt).toPath()})
     * and <b>http://example.com</b> ({@code new URI("http://example.com)}).
     *
     * <p>Test using concordance data will test if the {@link Path} implementation for a
     * {@link File} produce the same result as the {@link HttpPath} implementation, by appending
     * to the beginning of the path the {@code root} URI.
     *
     * <p>Each row of the returned matrix is composed by a {@link Path} (path within root) and a
     * {@link URI} (root).
     *
     * @return matrix with test data.
     *
     * @throws URISyntaxException if some of the roots are failing.
     */
    @DataProvider
    public Object[][] getConcordanceTestData() throws URISyntaxException {
        final URI root = new URI("http://example.com");
        return new Object[][] {
                {new File("/").toPath(), root},
                {new File("/file.txt").toPath(), root},
                {new File("/directory").toPath(), root},
                {new File("/directory/file.txt").toPath(), root},
                {new File("/directory1/directory2").toPath(), root},
                {new File("/directory1/directory2/file.txt").toPath(), root}
        };
    }

    private static void concordantPathTest(final Path pathWithinRoot, final URI root,
            final UnaryOperator<Path> testMethod) throws Exception {
        // create the actual path to test
        final Path actual = testMethod.apply(createHttpPath(pathWithinRoot, root));
        final Path expected = testMethod.apply(pathWithinRoot);

        if (actual == null) {
            // if the method returns null for the actual Path, it should do the same for the expected
            Assert.assertNull(expected);
        } else {
            // same absolute status
            Assert.assertEquals(actual.isAbsolute(), expected.isAbsolute(),
                    // for debugging this incompatibility
                    "isAbsolute for HTTP/S (" + actual + ") different from " + expected);

            // create as an HTTP/path by appending the root to the URI
            final HttpPath expectedHttp = createHttpPath(expected, root);

            if (actual.isAbsolute()) {
                // test the String representation
                Assert.assertEquals(actual.toString(), expectedHttp.toString());
                // finally, check that the URI representation is the same
                Assert.assertEquals(actual.toUri(), expectedHttp.toUri());
            } else {
                // TODO - do not skip once relative Path is supported
                throw new SkipException("Cannot test concordance for relative path: " + actual);
            }
        }
    }

    @Test(dataProvider = "getConcordanceTestData")
    public void testGetRootConcordance(final Path pathWithinRoot, final URI root) throws Exception {
        concordantPathTest(pathWithinRoot, root, Path::getRoot);
    }

    @Test(dataProvider = "getConcordanceTestData")
    public void testGetFileNameConcordance(final Path pathWithinRoot, final URI root)
            throws Exception {
        concordantPathTest(pathWithinRoot, root, Path::getFileName);
    }

    @Test(dataProvider = "getConcordanceTestData")
    public void testGetParentConcordance(final Path pathWithinRoot, final URI root)
            throws Exception {
        concordantPathTest(pathWithinRoot, root, Path::getParent);
    }

    @Test(dataProvider = "getConcordanceTestData")
    public void testGetNameConcordance(final Path pathWithinRoot, final URI root) throws Exception {
        for (int count = pathWithinRoot.getNameCount() - 1; count > 0; count--) {
            // should be final to use in lambda
            final int i = count;
            concordantPathTest(pathWithinRoot, root, p -> p.getName(i));
        }
    }

    @Test
    public void testNormalizeSeveralSlashes() throws Exception {
        final URI uri = new URI("http://example.com//directory//file.txt");
        final HttpPath path = new HttpPath(uri, HTTP_FILE_SYSTEM);
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
