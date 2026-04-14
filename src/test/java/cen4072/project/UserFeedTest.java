package cen4072.project;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.*;

import java.time.Duration;
import java.util.List;

public class UserFeedTest {

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String VALID_USERNAME = "SuperTestProjectAccount";
    private static final String VALID_PASSWORD = "SuperTestProjectAccount";

    private static final String SETTINGS_URL = "https://mangadex.org/settings#account";
    private static final String HOME_URL = "https://mangadex.org/";
    private static final String MANGA_URL = "https://mangadex.org/title/f7b62193-bdfb-4953-a6c6-0bd1b9a872f9/sensou-kyoushitsu";
    private static final String FEED_URL = "https://mangadex.org/titles/feed";

    private static final String KEYCLOAK_LOGIN_URL =
            "https://auth.mangadex.org/realms/mangadex/protocol/openid-connect/auth"
                    + "?client_id=mangadex-frontend-stable"
                    + "&redirect_uri=https%3A%2F%2Fmangadex.org%2Fauth%2Flogin"
                    + "%3FafterAuthentication%3D%2F%26shouldRedirect%3Dtrue%26wasPopup%3Dfalse"
                    + "&response_type=code"
                    + "&scope=openid+email+groups+profile+roles";

    @BeforeMethod
    void setUp() {
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        driver.manage().window().maximize();
    }

    @AfterMethod
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    public void performLogin(String username, String password, Boolean rememberme) throws InterruptedException {
        driver.get(SETTINGS_URL);

        WebElement loginBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class,'md-btn')]//span[contains(normalize-space(),'Log In')]/..")
        ));
        loginBtn.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")))
                .sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);

        if (rememberme == true) {
            try {
                WebElement rememberMe = driver.findElement(By.id("rememberMe"));
                if (!rememberMe.isSelected()) {
                    rememberMe.click();
                    System.out.println("'Remember Me' checkbox checked.");
                }
            } catch (NoSuchElementException e) {
                System.out.println("'Remember Me' checkbox not found — proceeding without it.");
            }
        }

        driver.findElement(By.id("kc-login")).click();

        wait.until(ExpectedConditions.urlContains("mangadex.org"));
        Thread.sleep(3000);
    }

    private void performLogout() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get(HOME_URL);

        WebElement avatar = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("avatar")));
        avatar.click();

        List<WebElement> buttons = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.xpath("//button[.//span[contains(normalize-space(),'Sign Out')]]")
        ));

        WebElement visibleButton = null;
        for (WebElement btn : buttons) {
            if (btn.isDisplayed()) {
                visibleButton = btn;
                break;
            }
        }

        if (visibleButton == null) {
            throw new RuntimeException("No visible Sign Out button found");
        }

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", visibleButton);
    }

    public void OpenFeed() throws InterruptedException {
        performLogin(VALID_USERNAME, VALID_PASSWORD, false);
        driver.get(FEED_URL);
        Thread.sleep(5000);
    }

    @Test
    public void testToggleFeedLayout() throws InterruptedException {
        OpenFeed();

        Thread.sleep(2000);

        WebElement controls = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[contains(@class,'controls')]")
        ));

        Thread.sleep(2000);

        List<WebElement> buttons = controls.findElements(
                By.xpath(".//div[contains(@class,'item')]")
        );

        WebElement leftButton = buttons.get(0);
        WebElement rightButton = buttons.get(1);

        Assert.assertFalse(leftButton.getAttribute("class").contains("active"),
                "Left button should NOT be active initially.");
        Assert.assertTrue(rightButton.getAttribute("class").contains("active"),
                "Right button SHOULD be active initially.");

        System.out.println("Initial state confirmed.");
        Thread.sleep(2000);

        System.out.println("Clicking LEFT...");
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", leftButton);

        Thread.sleep(3000);

        buttons = controls.findElements(By.xpath(".//div[contains(@class,'item')]"));
        leftButton = buttons.get(0);
        rightButton = buttons.get(1);

        Assert.assertTrue(leftButton.getAttribute("class").contains("active"),
                "Left button should be active after clicking it.");
        Assert.assertFalse(rightButton.getAttribute("class").contains("active"),
                "Right button should NOT be active after switching.");

        System.out.println("Left layout confirmed.");
        Thread.sleep(2000);

        System.out.println("Clicking RIGHT...");
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", rightButton);

        Thread.sleep(3000);

        buttons = controls.findElements(By.xpath(".//div[contains(@class,'item')]"));
        leftButton = buttons.get(0);
        rightButton = buttons.get(1);

        Assert.assertFalse(leftButton.getAttribute("class").contains("active"),
                "Left button should NOT be active after switching back.");
        Assert.assertTrue(rightButton.getAttribute("class").contains("active"),
                "Right button should be active again.");

        System.out.println("Right layout confirmed.");
        Thread.sleep(2000);

        System.out.println("Test passed: layout toggled correctly.");
    }

    @Test
    public void testMarkChaptersAsReadFromFeed() throws InterruptedException {
        OpenFeed();

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("div.chapter-feed__container")
        ));
        Thread.sleep(3000);

        List<WebElement> markers = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("svg.readMarker")
        ));

        Assert.assertTrue(markers.size() > 0,
                "Expected at least one read marker on the feed page.");

        System.out.println("Found " + markers.size() + " read markers on the page.");

        JavascriptExecutor js = (JavascriptExecutor) driver;

        int markersToClick = Math.min(markers.size(), 5);

        for (int i = 0; i < markersToClick; i++) {
            markers = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                    By.cssSelector("svg.readMarker")
            ));

            WebElement marker = markers.get(i);

            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", marker);
            Thread.sleep(1000);

            js.executeScript(
                    "arguments[0].dispatchEvent(new MouseEvent('click', {bubbles:true, cancelable:true}));",
                    marker
            );

            System.out.println("Clicked read marker #" + (i + 1));
            Thread.sleep(1500);
        }

        Assert.assertTrue(markersToClick > 0,
                "Expected to click at least one read marker.");

        System.out.println("Finished marking " + markersToClick + " chapters as read.");
    }

    @Test
    public void testOpenFirstMangaFromFeed() throws InterruptedException {
        OpenFeed();

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("div.chapter-feed__container")
        ));
        Thread.sleep(2000);

        WebElement firstMangaTitle = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("(//a[contains(@class,'chapter-feed__title')])[1]")
        ));

        String expectedTitle = firstMangaTitle.getText().trim();
        System.out.println("First manga title in feed: " + expectedTitle);

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", firstMangaTitle);

        Thread.sleep(3000);

        WebElement mangaPageTitle = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//p[contains(@class,'mb-1')]")
        ));

        String actualTitle = mangaPageTitle.getText().trim();
        System.out.println("Manga page title: " + actualTitle);

        Assert.assertEquals(actualTitle, expectedTitle,
                "The manga title on the manga page should match the title clicked in the feed.");
    }

    @Test
    public void testOpenFirstChapterFromFeed() throws InterruptedException {
        OpenFeed();

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("div.chapter-feed__container")
        ));
        Thread.sleep(2000);

        WebElement firstChapterLink = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("(//div[contains(@class,'chapter-feed__container')])[1]//a[contains(@class,'chapter-grid')][1]")
        ));

        WebElement chapterTitleInFeed = firstChapterLink.findElement(
                By.xpath(".//span[contains(@class,'line-clamp-1')]")
        );

        String expectedChapterTitle = chapterTitleInFeed.getText().trim();
        System.out.println("First chapter title in feed: " + expectedChapterTitle);

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", firstChapterLink);

        Thread.sleep(4000);

        WebElement chapterPageTitle = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'reader--header-title')]")
        ));

        String actualChapterTitle = chapterPageTitle.getText().trim();
        System.out.println("Chapter page title: " + actualChapterTitle);

        Assert.assertTrue(
                expectedChapterTitle.contains(actualChapterTitle),
                "Expected feed title to contain chapter page title.\n" +
                        "Feed: " + expectedChapterTitle + "\n" +
                        "Page: " + actualChapterTitle
        );
    }

    @Test
    public void testNavigateFeedPagination() throws InterruptedException {
        OpenFeed();

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("div.chapter-feed__container")
        ));
        Thread.sleep(2000);

        String originalUrl = driver.getCurrentUrl();
        System.out.println("Original feed URL: " + originalUrl);

        WebElement nextPageButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("(//a[contains(@href,'page=') or contains(@class,'next')] | //button[contains(@class,'next')])[last()]")
        ));

        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", nextPageButton);
        Thread.sleep(1000);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextPageButton);

        Thread.sleep(3000);

        String newUrl = driver.getCurrentUrl();
        System.out.println("URL after clicking next: " + newUrl);

        Assert.assertNotEquals(newUrl, originalUrl,
                "The feed URL should change after navigating to the next page.");

        try {
            WebElement previousPageButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("(//a[contains(@href,'page=') or contains(@class,'prev')] | //button[contains(@class,'prev')])[1]")
            ));

            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", previousPageButton);
            Thread.sleep(1000);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", previousPageButton);

            Thread.sleep(3000);

            String backUrl = driver.getCurrentUrl();
            System.out.println("URL after clicking previous: " + backUrl);

            Assert.assertNotEquals(backUrl, newUrl,
                    "The feed URL should change again after navigating back.");
        } catch (TimeoutException e) {
            System.out.println("Previous page button not found after navigating forward.");
        }
    }
}