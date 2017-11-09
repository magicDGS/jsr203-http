package org.magicdgs.http.jsr203;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class HttpFileSystemUnitTest {

    @Test
    public void testIsReadOnly() throws Exception {
        Assert.assertTrue(new HttpFileSystem().isReadOnly());
    }

}