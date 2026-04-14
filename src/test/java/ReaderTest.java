import static utils.MangaDexUtils.MANGADEX;

import java.time.Duration;
import java.util.Arrays;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import utils.BaseTest;

public class ReaderTest extends BaseTest {

    Boolean imageIsLoaded(WebElement img) {
        return (Boolean)((JavascriptExecutor)driver).executeScript("return arguments[0].complete && arguments[0].naturalWidth > 0", img);
    } 

    String pageNumXpath = "//*[contains(concat(' ', normalize-space(@class), ' '), ' page-number ')" +
                          "  and not(preceding-sibling::*)" +
                          "  and not(contains(., '?'))]";

    @Test
    public void testReaderLoadsImages() throws InterruptedException {
        var id = "f4b4b367-12c8-474d-aa49-6e61d3a62e84";

        var pages = 10;

        driver.get(MANGADEX + "/title/" + id);

        // Start Reading
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".chapter-grid:has(img[title='English'])"))).click();

        var pageCount = 0;

        wait.until(ExpectedConditions.urlContains("/chapter/"));

        while (driver.getCurrentUrl().startsWith(MANGADEX + "/chapter")) {
            pageCount++;
            var img = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".md--reader-chapter img[alt^='" + pageCount + "-']"))
            );

            Assert.assertTrue(imageIsLoaded(img));

            new Actions(driver).sendKeys(Keys.ARROW_RIGHT).perform();

            Thread.sleep(250);
        }

        Assert.assertEquals(pageCount, pages);
    }

    @Test
    public void testPageNumbers() throws Exception {
        var id = "f4b4b367-12c8-474d-aa49-6e61d3a62e84";

        driver.get(MANGADEX + "/title/" + id);

        // Start Reading
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".chapter-grid:has(img[title='English'])"))).click();

        var pageCount = 0;

        wait.until(ExpectedConditions.urlContains("/chapter/"));

        while (driver.getCurrentUrl().startsWith(MANGADEX + "/chapter")) {
            

            var pageNum = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath(pageNumXpath)
            ));

            pageCount++;

            assertEquals(pageNum.getAttribute("innerText"), String.valueOf(pageCount));

            new Actions(driver).sendKeys(Keys.ARROW_RIGHT).perform();

            Thread.sleep(250);
        }    
    }

    @Test
    public void testChapterPageUrl() throws InterruptedException {
        var url = "https://mangadex.org/chapter/52e138d5-ef57-46ad-a96d-151c2881aac0/12";

        driver.get(url);

        Thread.sleep(2000);

        var pageNum = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(pageNumXpath)));

        assertEquals(pageNum.getAttribute("innerText"), "12");
    }

    @Test
    public void testChapterTransition() throws InterruptedException {
        var chapter130 = "https://mangadex.org/chapter/db773f74-802e-4706-8866-536642620f1e";
        var chapter131 = "https://mangadex.org/chapter/f97178f9-3e38-4bac-be64-44564ec0a96e";

        driver.get(chapter130);

        Thread.sleep(1000);

        new Actions(driver)
            .sendKeys(Keys.ARROW_RIGHT)
            .pause(Duration.ofMillis(250))
            .sendKeys(Keys.ARROW_RIGHT)
            .pause(Duration.ofMillis(250))
            .sendKeys(Keys.ARROW_RIGHT)
            .perform();

        assertTrue(wait.until(ExpectedConditions.urlContains(chapter131)));
    }

    @Test
    public void testProgessSliders() throws InterruptedException {
        var id = "f4b4b367-12c8-474d-aa49-6e61d3a62e84";

        driver.get(MANGADEX + "/title/" + id);

        // Start Reading
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".chapter-grid:has(img[title='English'])"))).click();

        var dividers = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(".slider-dividers .prog-divider")));

        for (int i = 0; driver.getCurrentUrl().startsWith(MANGADEX + "/chapter"); i++) {
            
            if (i > 0) assertTrue(
                Arrays.stream(dividers.get(0).getAttribute("class").split(" "))
                    .anyMatch("read"::contains)
            );

            assertTrue(
                Arrays.stream(dividers.get(i).getAttribute("class").split(" "))
                    .anyMatch("read"::contains)          
            );

            new Actions(driver).sendKeys(Keys.ARROW_RIGHT).perform();
            Thread.sleep(250);
        }

    }

}
