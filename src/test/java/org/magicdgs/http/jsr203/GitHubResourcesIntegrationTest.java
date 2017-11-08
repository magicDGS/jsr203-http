package org.magicdgs.http.jsr203;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
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
        final URI uri = new URI(getGithubPagesFileUrl(fileName));
        final HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
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
        Assert.assertTrue(new File(getPathFromLocalDocsFile(fileName)).exists());
    }

    @Test(dataProvider = "getDocsFilesForTesting")
    public void testGithubAndDocsResourcesAreEqual(final String fileName) throws Exception {
        final byte[] local = Files.readAllBytes(new File(getPathFromLocalDocsFile(fileName)).toPath());
        final byte[] github = readAllBytes(new URI(getGithubPagesFileUrl(fileName)).toURL());
        Assert.assertEquals(github, local);
    }
}
