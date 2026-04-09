package utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
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

    @BeforeTest
    protected void getCookie() throws UnsupportedEncodingException, IOException {
        var stream = getClass().getClassLoader().getResourceAsStream("cookie.json");
        
        cookie = new String(stream.readAllBytes(), "utf-8");
    }

    @BeforeMethod
    protected void setup() {
        driver = new EdgeDriver();

        wait = new FluentWait<>(driver)
            .withTimeout(Duration.ofSeconds(8))
            .pollingEvery(Duration.ofMillis(300));

        driver.get(MANGADEX + "/404");

        ((JavascriptExecutor)driver).executeScript("localStorage.setItem('md', arguments[0])", cookie);

        driver.navigate().back();
    }

    @AfterMethod
    protected void cleanup() {
        if (driver != null) {
            driver.quit();
        }
    }

    public List<SearchResult> getSearchResults() throws Exception {

        var elements = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("manga-card")));

        return elements.stream().map(card -> new SearchResult(card)).toList();
    }
    
}
