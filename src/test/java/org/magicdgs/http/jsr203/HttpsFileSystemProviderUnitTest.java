package org.magicdgs.http.jsr203;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class HttpsFileSystemProviderUnitTest extends BaseTest {

    @Test
    public void testGetScheme() throws Exception {
        Assert.assertEquals(new HttpsFileSystemProvider().getScheme(), "https");
    }

}