package org.magicdgs.http.jsr203;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.InvalidPathException;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class HttpFileSystemUnitTest extends BaseTest {

    private static final HttpFileSystemProvider TEST_PROVIDER = new HttpFileSystemProvider();
    private static final String TEST_AUTHORITY = "example.com";

    @DataProvider
    public Object[][] nullArgs() {
        return new Object[][] {
                {null, null},
                {null, TEST_AUTHORITY},
                {TEST_PROVIDER, null}
        };
    }

    @Test(dataProvider = "nullArgs", expectedExceptions = IllegalArgumentException.class)
    public void testNullArguments(final HttpAbstractFileSystemProvider provider,
            final String authority) {
        new HttpFileSystem(provider, authority);
    }

    @Test
    public void testIsReadOnly() {
        Assert.assertTrue(new HttpFileSystem(TEST_PROVIDER, TEST_AUTHORITY).isReadOnly());
    }

    @Test
    public void testAlwaysOpen() {
        final HttpFileSystem fs = new HttpFileSystem(TEST_PROVIDER, TEST_AUTHORITY);
        Assert.assertTrue(fs.isOpen());
        fs.close();
        Assert.assertTrue(fs.isOpen());
    }


    @DataProvider
    public Object[][] validPaths() {
        final HttpFileSystem fs = new HttpFileSystem(TEST_PROVIDER, TEST_AUTHORITY);
        final String file = "/file.txt";
        final String dir = "/dir";
        final String query = "query=hello+world";
        final String ref = "1245";
        final String[] empty = new String[0];
        return new Object[][] {
                // the root path
                {fs, "", empty,
                        new HttpPath(fs, "", null, null)},
                // only paths
                {fs, file, empty,
                        new HttpPath(fs, file, null, null)},
                {fs, dir + file, empty,
                        new HttpPath(fs, dir + file, null, null)},
                {fs, dir, new String[] {file},
                        new HttpPath(fs, dir + file, null, null)},
                {fs, dir + dir, new String[] {file},
                        new HttpPath(fs, dir + dir + file, null, null)},
                {fs, dir, new String[] {dir, file},
                        new HttpPath(fs, dir + dir + file, null, null)},
                // only path + query
                {fs, dir + file + '?' + query, empty,
                        new HttpPath(fs, dir + file, query, null)},
                // only path + reference
                {fs, dir + file + '#' + ref, empty,
                        new HttpPath(fs, dir + file, null, ref)},
                // path + query + reference
                {fs, dir + file + '?' + query + '#' + ref, empty,
                        new HttpPath(fs, dir + file, query, ref)},
        };
    }

    @Test(dataProvider = "validPaths")
    public void testGetPath(final HttpFileSystem fs, final String first, final String[] more,
            final HttpPath expected) {
        assertEqualsPath(fs.getPath(first, more), expected);
    }

    @DataProvider
    public Object[][] invalidPaths() {
        final String[] empty = new String[0];
        return new Object[][] {
                // relative path
                {"directory", empty},
                // null containing path
                {"/directory\0null", empty},
                // joining invalid more
                {"/directory", new String[] {"null", "\0", "world"}}
        };
    }

    @Test(dataProvider = "invalidPaths", expectedExceptions = InvalidPathException.class)
    public void testInvalidGetPath(final String first, final String... more) {
        new HttpFileSystem(TEST_PROVIDER, TEST_AUTHORITY).getPath(first, more);
    }

    @DataProvider
    public Object[][] equalityData() {
        final HttpFileSystem test = new HttpFileSystem(TEST_PROVIDER, TEST_AUTHORITY);
        return new Object[][] {
                // same object
                {test, test, true},
                // different objects
                {test, new HttpFileSystem(TEST_PROVIDER, TEST_AUTHORITY), true},
                // not equal providers
                {test, new HttpFileSystem(new HttpsFileSystemProvider(), TEST_AUTHORITY), false},
                // not equal authorities
                {test, new HttpFileSystem(TEST_PROVIDER, TEST_AUTHORITY + ".org"), false}
        };
    }

    @Test(dataProvider = "equalityData")
    public void testEquals(final HttpFileSystem first, final HttpFileSystem second,
            final boolean expected) {
        Assert.assertEquals(first.equals(second), expected);
        Assert.assertEquals(second.equals(first), expected);
    }

    @Test
    public void testEqualsDifferentClasses() {
        Assert.assertFalse(new HttpFileSystem(TEST_PROVIDER, TEST_AUTHORITY)
                .equals(TEST_AUTHORITY));
    }


    @Test
    public void testHashCodeSameObject() {
        final HttpFileSystem fs = new HttpFileSystem(TEST_PROVIDER, TEST_AUTHORITY);
        Assert.assertEquals(fs.hashCode(), fs.hashCode());
    }

    @Test
    public void testHashCodeEqualObjects() {
        Assert.assertEquals(
                new HttpFileSystem(TEST_PROVIDER, TEST_AUTHORITY).hashCode(),
                new HttpFileSystem(TEST_PROVIDER, TEST_AUTHORITY).hashCode());
    }
}
