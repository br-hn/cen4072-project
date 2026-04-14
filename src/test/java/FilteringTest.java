import static utils.MangaDexUtils.*;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import utils.BaseTest;
import utils.MangaDexUtils.SearchResult;
 
public class FilteringTest extends BaseTest {

    String filterBtn(String filter) {
        return "//label[contains(., '" + filter + "')]/following-sibling::button[1]"; 
    }

    void showFilters() {
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(., 'Show filters')]"))).click();
    }

    void expandFilterDropdown(String filter) {
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath(filterBtn(filter)))).click();
    }

    void filterTags(String... tags) {

        for (var tag : tags) {

            WebElement el = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath(
                    "//label[contains(., 'Filter tags')]/following-sibling::button[1]/following-sibling::*[1]" +
                    "//span[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + 
                    (tag.startsWith("-") ? tag.substring(1) : tag) + "')]")
            ));

            el.click();

            if (tag.startsWith("-")) {
                el.click();
            }
        }
    }

    void filterSelect(String name, String... filters) {
        
        expandFilterDropdown(name);

        var options = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath(filterBtn(name) + "/following-sibling::ul[1]//li")));

        for (var opt : options) {
            for (var filter : filters) {
                if (opt.getAttribute("textContent").equals(filter)) {
                    opt.click();
                }
            }
        }
    }

    void search() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(., 'Search')]"))).click();
        Thread.sleep(3000);
    }

    void scrollToResult(SearchResult result) {
        new Actions(driver).scrollToElement(result.card()).perform();
    }

    @Test
    public void testMultipleTagsWithAnd() throws Exception {

        String[] tags = { "action", "fantasy" };

        driver.get(MANGADEX + "/titles");
        driver.manage().window().maximize();

        showFilters();

        expandFilterDropdown("Filter tags");

        filterTags(tags);

        search();

        Thread.sleep(3000);

        var results = getSearchResults();

        for (SearchResult result : results) {
            scrollToResult(result);
            assertTrue(result.tags().stream().anyMatch(tags[0]::equalsIgnoreCase));
            assertTrue(result.tags().stream().anyMatch(tags[1]::equalsIgnoreCase));
        }

        Thread.sleep(5000);
    }

    @Test
    public void testExcludeTag() throws Exception {
        String tag = "-romance";

        driver.get(MANGADEX + "/titles");
        driver.manage().window().maximize();

        showFilters();

        expandFilterDropdown("Filter tags");

        filterTags(tag);

        search();

        var results = getSearchResults();

        for (SearchResult result : results) {
            scrollToResult(result);
            assertFalse(result.tags().stream().anyMatch(tag::equalsIgnoreCase));
        }

        Thread.sleep(5000);
    }

    @Test
    public void testFilterByStatus() throws Exception {

        var status = "Completed";

        driver.get(MANGADEX + "/titles");
        driver.manage().window().maximize();

        showFilters();

        filterSelect("Publication Status", status);

        search();

        var results = getSearchResults();

        for (SearchResult result : results) {
            scrollToResult(result);

            assertEquals(result.getStatus(), "Completed");
        }

    }

    @Test
    public void testFilterByAuthor() throws Exception {

        var author = "Toriyama Akira";

        driver.get(MANGADEX + "/titles");

        // Show filter options
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(., 'Show filters')]"))).click();

        // Expand author box
        wait.until(ExpectedConditions
                .elementToBeClickable(By.xpath("//label[.//span[@title='Authors']]/following-sibling::button[1]")))
                .click();

        // Search for author
        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//label[.//span[@title='Authors']]/following-sibling::ul[1]//input[1]"))).sendKeys(author);

        // Select author
        wait.until(ExpectedConditions
                .elementToBeClickable(By.xpath("//label[.//span[@title='Authors']]/following-sibling::ul[1]/li[2]")))
                .click();

        // Press search button
        driver.findElement(By.xpath("//button[contains(., 'Search')]")).click();

        Thread.sleep(3000);

        var results = getSearchResults();

        for (SearchResult result : results) {
            scrollToResult(result);

            var metadata = getMangaMetadata(result.id(), driver);

            var authors = metadata.get("Authors");

            System.out.println(authors);

            assertTrue(authors.contains(author));
        }
    }

    @Test
    public void testResetFilters() throws Exception {

        String[] tags = { "fantasy", "-romance", "-slice of life" };

        String status = "Completed";

        driver.get(MANGADEX + "/titles");

        showFilters();

        expandFilterDropdown("Filter tags");
        filterTags(tags);

        new Actions(driver).sendKeys(Keys.ESCAPE).perform();

        filterSelect("Publication Status", status);


        search();

        Thread.sleep(2500);

        driver.findElement(By.xpath("//button[contains(., 'Reset filters')]")).click();

        Thread.sleep(2500);

        var results = getSearchResults();

        assertTrue(results.stream().anyMatch(r -> r.tags().contains("Romance") || r.tags().contains("Slice of Life")));
        assertTrue(results.stream().anyMatch(r -> !r.status().contains(status)));
        assertTrue(results.stream().anyMatch(r -> !r.tags().contains("Fantasy")));


    }

}
