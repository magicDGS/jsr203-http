package org.magicdgs.http.jsr203;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Tests for concordance between the default implementation of {@link Path} methods and
 * {@link HttpPath}.
 *
 * <p>The aim of this tests is to check that the resolution of methods in the path component for
 * the URL implemented in {@link HttpPath}, the resolution and the methods provides the same answer
 * as the {@link File} implementation (through its default {@link Path} implementation.
 *
 * <p>Tests are TODO - write more information
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class HttpPathConcordanceDefaultPathTest extends BaseTest {

    /// FileSystem for testing (HTTP, so no HTTPS urls)
    private static final HttpFileSystem HTTP_FILE_SYSTEM =
            new HttpFileSystem(new HttpFileSystemProvider());

    private static final URI EXAMPLE_ROOT;

    static {
        try {
            EXAMPLE_ROOT = new URI("http://example.com");
        } catch (URISyntaxException e) {
            throw new RuntimeException("Should not happen");
        }
    }

    // helper method to create an HttpPath from a File.toPath and the URI root
    private static HttpPath createHttpPath(final Path pathWithinRoot)
            throws URISyntaxException {
        // create the URL string
        String url = EXAMPLE_ROOT.toString();
        if (!pathWithinRoot.startsWith("/")) {
            url += "/";
        }
        url += pathWithinRoot.toString();

        return new HttpPath(new URI(url), HTTP_FILE_SYSTEM);
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
        return new Object[][] {
                {new File("/").toPath()},
                {new File("/file.txt").toPath()},
                {new File("/directory").toPath()},
                {new File("/directory/file.txt").toPath()},
                {new File("/directory1/directory2").toPath()},
                {new File("/directory1/directory2/file.txt").toPath()}
        };
    }

    private static void concordantPathTest(final Path pathWithinRoot,
            final UnaryOperator<Path> testMethod) throws Exception {
        // create the actual path to test
        final Path actual = testMethod.apply(createHttpPath(pathWithinRoot));
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
            final HttpPath expectedHttp = createHttpPath(expected);

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
    public void testGetRootConcordance(final Path pathWithinRoot) throws Exception {
        concordantPathTest(pathWithinRoot, Path::getRoot);
    }

    @Test(dataProvider = "getConcordanceTestData")
    public void testGetFileNameConcordance(final Path pathWithinRoot)
            throws Exception {
        concordantPathTest(pathWithinRoot, Path::getFileName);
    }

    @Test(dataProvider = "getConcordanceTestData")
    public void testGetParentConcordance(final Path pathWithinRoot)
            throws Exception {
        concordantPathTest(pathWithinRoot, Path::getParent);
    }

    @Test(dataProvider = "getConcordanceTestData")
    public void testGetNameConcordance(final Path pathWithinRoot) throws Exception {
        for (int count = pathWithinRoot.getNameCount() - 1; count > 0; count--) {
            // should be final to use in lambda
            final int i = count;
            concordantPathTest(pathWithinRoot, p -> p.getName(i));
        }
    }

    @DataProvider
    public Iterator<Object[]> getResolveTestData() throws Exception {
        final Object[][] concordanceData = getConcordanceTestData();
        final List<Object[]> toReturn = new ArrayList<>(concordanceData.length * concordanceData.length);
        for (int i = 0; i < concordanceData.length; i++) {
            for (int j = 0; j < concordanceData.length; j++) {
                final Object[] iConcordanceData = concordanceData[i];
                final Object[] jConcordanceData = concordanceData[j];
                final Object[] data = new Object[iConcordanceData.length + jConcordanceData.length];
                System.arraycopy(iConcordanceData, 0, data, 0, iConcordanceData.length);
                System.arraycopy(jConcordanceData, 0, data, iConcordanceData.length, jConcordanceData.length);
                toReturn.add(data);
            }
        }
        return toReturn.iterator();
    }

    private static void concordantPathTestResolver(final Path pathWithinRoot,
            final Function<Path, Path> testMethod) throws Exception {
        // create the actual path to test
        final Path actual = testMethod.apply(createHttpPath(pathWithinRoot));
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
            final HttpPath expectedHttp = createHttpPath(expected);

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

    @Test(dataProvider = "getResolveTestData")
    public void testResolveSiblingConcordance(final Path pathWithinRoot, final Path toResolve) throws Exception {
        final HttpPath httpToResolve = createHttpPath(toResolve);
        concordantPathTestResolver(pathWithinRoot, (path) -> {
            if (path instanceof HttpPath) {
                return path.resolveSibling(httpToResolve);
            } else {
                return path.resolveSibling(toResolve);
            }
        });
    }

    @Test(dataProvider = "getResolveTestData")
    public void testResolveConcordance(final Path pathWithinRoot, final Path toResolve) throws Exception {
        final HttpPath httpToResolve = createHttpPath(toResolve);
        concordantPathTestResolver(pathWithinRoot, (path) -> {
            if (path instanceof HttpPath) {
                return path.resolve(httpToResolve);
            } else {
                return path.resolve(toResolve);
            }
        });
    }
}
