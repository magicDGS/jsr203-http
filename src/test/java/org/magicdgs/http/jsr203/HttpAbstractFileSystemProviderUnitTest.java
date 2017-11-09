package org.magicdgs.http.jsr203;

import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class HttpAbstractFileSystemProviderUnitTest extends BaseTest {

    // testing provider
    private static final HttpAbstractFileSystemProvider TEST_PROVIDER =
            new HttpAbstractFileSystemProvider() {
                @Override
                public String getScheme() {
                    // return null scheme
                    return null;
                }
            };

    @Test(expectedExceptions = UnsupportedOperationException.class)
    void testCreateDirectoryIsUnsupported() throws Exception {
        TEST_PROVIDER.createDirectory(null);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    void testDeleteIsUnsupported() throws Exception {
        TEST_PROVIDER.delete(null);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    void testMoveIsUnsupported() throws Exception {
        TEST_PROVIDER.move(null, null);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    void testSetAttributeIsUnsupported() throws Exception {
        TEST_PROVIDER.setAttribute(null, null, null);
    }

}