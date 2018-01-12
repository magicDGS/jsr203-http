package org.magicdgs.http.jsr203;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
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

    @Test(dataProvider = "getDocsFilesForTesting", dataProviderClass = GitHubResourcesIntegrationTest.class)
    public void testExistingUrls(final String fileName) throws IOException {
        Assert.assertTrue(HttpUtils.exists(getGithubPagesFileUrl(fileName)));
    }

    @DataProvider
    public Object[][] nonExistantUrlStrings() {
        return new Object[][] {
                // unknown host
                {"http://www.unknown_host.com"},
                // non existant resource
                {"http://www.example.com/non_existant.html"}
        };
    }

    @Test(dataProvider = "nonExistantUrlStrings")
    public void testNonExistingUrl(final String urlString) throws IOException {
        final URL noExistant = URI.create(urlString).toURL();
        Assert.assertFalse(HttpUtils.exists(noExistant));
    }
}