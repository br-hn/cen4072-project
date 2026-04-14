import static utils.MangaDexUtils.*;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import utils.BaseTest;

public class DetailsPageTest extends BaseTest {

    static <T> boolean isSorted(List<T> list, Comparator<T> comp) {
        for (int i = 1; i < list.size(); i++) {
            if (comp.compare(list.get(i - 1), list.get(i)) > 0) {
                return false;
            }
        }
        return true;
    }

    @Test
    public void testMangaDetailsLoadsAllMetadata() {
        // One Punch Man
        var id = "d8a959f7-648e-4c8d-8f23-f1f3f8e129f3";

        var metadata = getMangaMetadata(id, driver);

        Assert.assertTrue(metadata.get("Authors").contains("ONE"));
        Assert.assertTrue(metadata.get("Artists").contains("Murata Yuusuke"));
        Assert.assertTrue(metadata.get("Genres").contains("Action"));
        Assert.assertTrue(metadata.get("Status").get(0).contains("Ongoing"));
        Assert.assertFalse(metadata.get("Sypnosis").isEmpty());
    }

    @Test
    public void testAlternativeTitles() {
        
        // Astro Boy
        var id = "ca4c84bb-7272-45aa-a22d-dc1282b52372";

        var metadata = getMangaMetadata(id, driver);

        var altTitles = metadata.get("Alternative Titles");

        Assert.assertTrue(altTitles.contains("Astro Boy"));
        Assert.assertTrue(altTitles.contains("Mighty Atom"));
        Assert.assertTrue(altTitles.contains("Tetsuwan Atom"));
    }

    @Test
    public void testTagSearch() throws InterruptedException {

        // One Punch Man
        var id = "d8a959f7-648e-4c8d-8f23-f1f3f8e129f3";

        var actionUrl = "https://mangadex.org/tag/391b0423-d847-456f-aff0-8b0cfc03066b/action";

        driver.get(MANGADEX + "/title/" + id);

        driver.manage().window().maximize();

        var actionTag = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='genres_undefined' and contains(., 'Action')]")));

        actionTag.click();

        Assert.assertEquals(driver.getCurrentUrl(), actionUrl);
    }

    @Test
    public void testChapterOrder() throws InterruptedException {
        var id = "5896e05d-3900-4947-baf8-403a9d8fa5ec";

        driver.get(MANGADEX + "/title/" + id);

        var sortingBtn = wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//*[contains(@style, 'grid-area: content')]//button[contains(., 'Descending') or contains(., 'Ascending')]")
        ));

        Supplier<List<Float>> getChapters = () -> 
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("chapter-header")))
            .stream()
            .map(chap -> chap.findElement(By.cssSelector("span")).getText().split(" ")[1])
            .map(Float::valueOf)
            .toList();

        for (int i = 0; i < 2; i++) {
            
            var chapters = getChapters.get();

            switch (sortingBtn.getText()) {
                case "Descending": assertTrue(isSorted(chapters, Comparator.reverseOrder()));
                    break;
                case "Ascending": assertTrue(isSorted(chapters, Comparator.naturalOrder()));
                    break;
                default: throw new AssertionError();
            };

            sortingBtn.click();

            Thread.sleep(500);
        }
    }

    @Test
    public void testDetailTabs() throws Exception {
        var id = "1d8e66b6-eb63-431c-b32f-ca0c31ef0d4e";

        driver.get(MANGADEX + "/title/" + id);

        Consumer<String> clickTab = (name) -> wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a.select__tab[href*='tab=" + name + "']"))).click();

        /// Art tab
        clickTab.accept("art");

        var covers = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.cssSelector("[style='grid-area: content;'] a img[alt='Cover image']")));

        assertFalse(covers.isEmpty());

        /// Related tab
        clickTab.accept("related");

        var results = getSearchResults();
        
        // original manga (non-color)
        var expected = "edaae213-67c7-4a6c-ad5f-141001891741";

        assertTrue(
            results.stream().map(r -> r.id()).anyMatch(expected::contains)
        );

        /// Recommendations tab
        clickTab.accept("recommendations");

        var recs = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.cssSelector("[style='grid-area: content;'] a img[alt='Cover image']")));

        assertFalse(recs.isEmpty());


    }
}
