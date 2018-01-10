package org.magicdgs.http.jsr203;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.HttpURLConnection;
import java.nio.file.Files;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class GitHubResourcesIntegrationTest extends BaseTest {

    /**
     * Gets files that exists in the docs/ directory and that are already in GitHub-pages.
     *
     * @return one file name per data.
     */
    @DataProvider
    public static Object[][] getDocsFilesForTesting() {
        return new Object[][] {
                {"file1.txt"},
                {"directory/file2.txt"}
        };
    }

    @Test(dataProvider = "getDocsFilesForTesting")
    public void testGitHubResourcesExists(final String fileName) throws Exception {
        final HttpURLConnection connection = (HttpURLConnection)
                getGithubPagesFileUrl(fileName).openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.connect();
            Assert.assertEquals(connection.getResponseCode(), HttpURLConnection.HTTP_OK);
        } finally {
            connection.disconnect();
        }
    }

    @Test(dataProvider = "getDocsFilesForTesting")
    public void testLocalDocsResourcesExists(final String fileName) throws Exception {
        Assert.assertTrue(Files.exists(getLocalDocsFilePath(fileName)));
    }

    @Test(dataProvider = "getDocsFilesForTesting")
    public void testGithubAndDocsResourcesAreEqual(final String fileName) throws Exception {
        // 1. read the local file
        final byte[] local = Files.readAllBytes(getLocalDocsFilePath(fileName));
        // 2. read the GitHub-pages file
        final byte[] github = readAllBytes(getGithubPagesFileUrl(fileName));

        // assert that the same bytes were read
        Assert.assertEquals(github, local);
    }
}
