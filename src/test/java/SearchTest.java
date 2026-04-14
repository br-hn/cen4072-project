import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static utils.MangaDexUtils.*;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;
import org.testng.annotations.*;

import utils.BaseTest;
import utils.MangaDexUtils.SearchResult;

public class SearchTest extends BaseTest {

    WebElement getSearchBar() {
        return wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".page-container input[title='Search']")));
    }

    void search() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(., 'Search')]"))).click();
        Thread.sleep(3000);
    }

    @Test
    public void testUrlContainsSearch() throws InterruptedException {
        
        var query = "kagurabachi";

        driver.get(MANGADEX + "/titles");

        var search = getSearchBar();

        search.sendKeys(query);

        search.sendKeys(Keys.ENTER);

        Thread.sleep(1000);

        var params = getQueryParams(driver.getCurrentUrl());

        assertEquals(query, params.get("q"));
    }

    public void testSearchByTitle() throws Exception {

        var query = "One Piece";

        driver.get(MANGADEX + "/titles");

        var search = getSearchBar();

        search.sendKeys(query);

        search.sendKeys(Keys.ENTER);

        Thread.sleep(2500);

        var results = getSearchResults();

        for (SearchResult result : results) {

            boolean matches = 
                result.title().toLowerCase().contains("one piece") ||
                result.title().toLowerCase().contains("one-piece") ||
                result.description().toLowerCase().contains("one piece");

            System.out.println(result.title());

            if (!matches) {
                var metadata = getMangaMetadata(result.id(), driver);

                matches = metadata.get("Alternative Titles")
                    .stream()
                    .anyMatch(title -> title.toLowerCase().contains("one piece"));
            }

            Assert.assertTrue(matches);
        }

        Thread.sleep(7000);
    }

    @Test
    public void testCaseInsensitiveSearch() throws Exception {
        String[] queries = { "naruto", "Naruto", "NARUTO" };

        String[] results = new String[3];

        driver.get(MANGADEX + "/titles");

        var searchBar = getSearchBar();

        for (int i = 0; i < queries.length; i++) {
            searchBar.clear();
            searchBar.sendKeys(queries[i]);

            search();

            Thread.sleep(1000);

            var manga = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("manga-card")));

            var result = new SearchResult(manga);

            results[i] = result.id();
        }

        Assert.assertTrue(results[0].equals(results[1]) && results[1].equals(results[2]));
    }

    @Test
    public void testNonEnglishInput() throws Exception {
        var query = "進撃の巨人";
        var id = "304ceac3-8cdb-4fe7-acf7-2b6ff7a60613";

        driver.get(MANGADEX + "/titles");

        var searchBar = getSearchBar();

        searchBar.sendKeys(query + Keys.ENTER);

        Thread.sleep(1500);

        var results = getSearchResults();

        assertTrue(
            results.stream().map(r -> r.id()).anyMatch(id::contains)
        );
        
    }

    @Test
    public void testSearchByAlternateTitles() throws Exception {

        var id = "b0b721ff-c388-4486-aa0f-c2b0bb321512";

        String[] queries = {
            "Frieren: Beyond Journey's End",
            "Frieren at the Funeral",
            "Frieren: Remnants Of The Departed",
            "Sousou no Frieren"
        };

        driver.get(MANGADEX + "/titles");

        var searchBar = getSearchBar();

        for (String query : queries) {
            searchBar.clear();
            searchBar.sendKeys(query + Keys.ENTER);

            Thread.sleep(1000);

            var results = getSearchResults();   

            assertTrue(
                results.stream().map(r -> r.id()).anyMatch(id::contains)
            );
        }
    }

}
