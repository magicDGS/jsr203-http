package org.magicdgs.http.jsr203;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class UtilsUnitTest extends BaseTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNonNullThrows() {
        final String errorMsg = "error message";
        try {
            Utils.nonNull(null, () -> errorMsg);
        } catch (IllegalArgumentException e) {
            // check that the passed method is the same
            Assert.assertSame(e.getMessage(), errorMsg);
            // rethrow for testing
            throw e;
        }
    }

    @Test
    public void testNonNullNotThrows() {
        final Object integer = 10;
        Assert.assertSame(Utils.nonNull(integer, () -> {throw new AssertionError("");}), integer);
    }
}