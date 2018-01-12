package org.magicdgs.http.jsr203;

import org.mockito.Mockito;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.URLConnection;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class HttpUtilsUnitTest extends BaseTest {

    @DataProvider
    public Object[][] illegalArgumentsForRangeRequest() throws Exception {
        // create a Mocked URL connection that throws an assertion error when
        // the setRequestProperty is set
        final URLConnection mockedConnection = Mockito.mock(URLConnection.class);
        Mockito.doThrow(new AssertionError("Called setRequestProperty")).when(mockedConnection)
                .setRequestProperty(Mockito.anyString(), Mockito.anyString());

        return new Object[][] {
                // invalid start
                {mockedConnection, -1, 10},
                // invalid end
                {mockedConnection, 10, -2},
                // reverse request
                {mockedConnection, 100, 10}
        };
    }

    @Test(dataProvider = "illegalArgumentsForRangeRequest", expectedExceptions = IllegalArgumentException.class)
    public void testSetRangeRequestIllegalArguments(final URLConnection connection, final int start, final int end)
            throws Exception {
        HttpUtils.setRangeRequest(connection, start, end);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testDisconnectNull() {
        HttpUtils.disconnect(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSetRangeRequestNull() {
        HttpUtils.setRangeRequest(null, 10, 100);
    }
}