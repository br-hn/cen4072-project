package cen4072.project.utils;

import java.time.Duration;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public class BaseTest {
    protected WebDriver driver;
    protected FluentWait<WebDriver> wait;

    @BeforeMethod
    void setup() {
        driver = new EdgeDriver();

        wait = new FluentWait<>(driver)
            .withTimeout(Duration.ofSeconds(8))
            .pollingEvery(Duration.ofMillis(300));
    }

    @AfterMethod
    void cleanup() {
        if (driver != null) {
            driver.quit();
        }
    }
    
}
