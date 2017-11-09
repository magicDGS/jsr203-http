package org.magicdgs.http.jsr203;

import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class HttpPathUnitTest {

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testToFile() throws Exception {
        new HttpPath().toFile();
    }

}