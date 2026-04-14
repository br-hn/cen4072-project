package utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.testng.annotations.*;

import utils.MangaDexUtils.SearchResult;

import static utils.MangaDexUtils.*;

public class BaseTest {
    protected WebDriver driver;
    protected FluentWait<WebDriver> wait;

    protected String cookie;

    // @BeforeTest
    // protected void getCookie() throws UnsupportedEncodingException, IOException {
    //     var stream = getClass().getClassLoader().getResourceAsStream("cookie.json");
        
    //     cookie = new String(stream.readAllBytes(), "utf-8");
    // }

    @BeforeClass
    protected void setup() throws UnsupportedEncodingException, IOException {
        var stream = getClass().getClassLoader().getResourceAsStream("cookie.json");
        
        cookie = new String(stream.readAllBytes(), "utf-8");

        driver = new EdgeDriver();

        wait = new FluentWait<>(driver)
            .withTimeout(Duration.ofSeconds(8))
            .pollingEvery(Duration.ofMillis(300));

        driver.get(MANGADEX + "/404");

        ((JavascriptExecutor)driver).executeScript("localStorage.setItem('md', arguments[0])", cookie);

        driver.navigate().back();

        driver.manage().window().maximize();
    }

    @AfterMethod
    protected void reset() {
        var old = driver.getWindowHandle();

        driver.switchTo().newWindow(WindowType.TAB);
        var _new = driver.getWindowHandle();
        driver.switchTo().window(old);
        driver.close();

        // now only the new tab remains
        driver.switchTo().window(_new);
    }

    @AfterClass
    protected void cleanup() {
        if (driver != null) driver.quit();
    }

    public List<SearchResult> getSearchResults() throws Exception {

        var elements = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
            By.cssSelector(".manga-card:not(:has(.skeleton))")
        ));

        var results = new ArrayList<SearchResult>();

        for (var card : elements) {
            results.add(new SearchResult(card));
        }

        return results;
    }
    
}
