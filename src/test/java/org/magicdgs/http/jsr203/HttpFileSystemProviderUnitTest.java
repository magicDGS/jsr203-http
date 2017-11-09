package org.magicdgs.http.jsr203;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class HttpFileSystemProviderUnitTest extends BaseTest {

    @Test
    public void testGetScheme() throws Exception {
        Assert.assertEquals(new HttpFileSystemProvider().getScheme(), "http");
    }

}