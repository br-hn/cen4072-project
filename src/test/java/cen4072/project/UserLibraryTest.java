package cen4072.project;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.*;

import java.time.Duration;
import java.util.List;

public class UserLibraryTest {

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String VALID_USERNAME = "SuperTestProjectAccount";
    private static final String VALID_PASSWORD = "SuperTestProjectAccount";

    private static final String SETTINGS_URL = "https://mangadex.org/settings#account";
    private static final String HOME_URL = "https://mangadex.org/";
    private static final String MANGA_URL = "https://mangadex.org/title/f7b62193-bdfb-4953-a6c6-0bd1b9a872f9/sensou-kyoushitsu";
    private static final String LIBRARY_URL = "https://mangadex.org/titles/follows";

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

        if (rememberme) {
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

    private void performLogout() throws InterruptedException {
        WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get(HOME_URL);

        WebElement avatar = shortWait.until(ExpectedConditions.presenceOfElementLocated(By.id("avatar")));
        avatar.click();

        Thread.sleep(500);

        List<WebElement> buttons = shortWait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
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

    private WebElement getMangaLibraryStatusButton() {
        return wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//button[contains(@class,'primary') and contains(@class,'glow') and .//span]")
        ));
    }

    private String getMangaLibraryStatusButtonText() {
        return getMangaLibraryStatusButton().getText().trim();
    }

    private void clickMangaLibraryStatusButton() {
        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class,'primary') and contains(@class,'glow') and .//span]")
        ));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
    }

    private WebElement waitForLibraryModal() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'md-modal__box')]")
        ));
    }

    private void openLibraryModalFromMangaPage() {
        String currentButtonText = getMangaLibraryStatusButtonText();
        System.out.println("Opening library modal from button text: " + currentButtonText);
        clickMangaLibraryStatusButton();
        waitForLibraryModal();
    }

    private WebElement getMangaTitleElement() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//p[contains(@class,'mb-1')]")
        ));
    }

    private String getMangaTitleText() {
        return getMangaTitleElement().getText().trim();
    }

    private void openReadingStatusDropdown() {
        WebElement dropdownInner = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[contains(@class,'md-modal__box')]//div[@role='button' and contains(@class,'md-select')]//div[contains(@class,'md-select-inner-wrap')]")
        ));

        new Actions(driver).moveToElement(dropdownInner).click().perform();

        wait.until(ExpectedConditions.attributeToBe(
                By.xpath("//div[contains(@class,'md-modal__box')]//div[@role='button' and contains(@class,'md-select')]"),
                "aria-expanded",
                "true"
        ));
    }

    private void selectReadingStatusOption(String status) {
        WebElement statusOption = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[@role='option' and normalize-space()='" + status + "' and not(contains(@style,'display: none'))]")
        ));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", statusOption);
    }

    private void clickModalConfirmButton() {
        WebElement confirmButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[contains(@class,'md-modal__box')]//button[" +
                        ".//span[normalize-space()='Add'] or .//span[normalize-space()='Update']]")
        ));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", confirmButton);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", confirmButton);
    }

    private void setMangaReadingStatus(String status) throws InterruptedException {
        openLibraryModalFromMangaPage();
        WebElement modal = waitForLibraryModal();

        openReadingStatusDropdown();
        selectReadingStatusOption(status);

        Thread.sleep(1000);
        clickModalConfirmButton();

        wait.until(ExpectedConditions.invisibilityOf(modal));
        Thread.sleep(500);
    }

    private void ensureMangaInLibrary() throws InterruptedException {
        driver.get(MANGA_URL);
        String buttonText = getMangaLibraryStatusButtonText();
        System.out.println("Current manga button text before ensure: " + buttonText);

        if (buttonText.equalsIgnoreCase("Add To Library")) {
            openLibraryModalFromMangaPage();
            WebElement modal = waitForLibraryModal();

            WebElement addBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[contains(@class,'md-modal__box')]//button[.//span[normalize-space()='Add']]")
            ));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", addBtn);

            wait.until(ExpectedConditions.invisibilityOf(modal));
            Thread.sleep(500);
        }
    }

    @Test
    void addToLibrary() throws InterruptedException {
        performLogin(VALID_USERNAME, VALID_PASSWORD, false);

        driver.get(MANGA_URL);

        String mangaTitle = getMangaTitleText();
        System.out.println("Manga title on page: " + mangaTitle);

        String currentButtonText = getMangaLibraryStatusButtonText();
        System.out.println("Current manga button text: " + currentButtonText);

        openLibraryModalFromMangaPage();
        WebElement modal = waitForLibraryModal();

        WebElement confirmButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[contains(@class,'md-modal__box')]//button[" +
                        ".//span[normalize-space()='Add'] or .//span[normalize-space()='Update']]")
        ));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", confirmButton);

        wait.until(ExpectedConditions.invisibilityOf(modal));
        Thread.sleep(500);

        driver.get(LIBRARY_URL);

        WebElement libraryTitleElement = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//a[contains(@class,'title')]//span[normalize-space()=\"" + mangaTitle + "\"]")
        ));

        String libraryTitle = libraryTitleElement.getText().trim();
        System.out.println("Manga title found in library: " + libraryTitle);

        Assert.assertEquals(libraryTitle, mangaTitle,
                "The manga title in the library should match the manga page title.");
    }

    @Test
    void removeFromLibrary() throws InterruptedException {
        performLogin(VALID_USERNAME, VALID_PASSWORD, false);

        ensureMangaInLibrary();
        driver.get(MANGA_URL);

        String mangaTitle = getMangaTitleText();
        System.out.println("Manga title on page: " + mangaTitle);

        openLibraryModalFromMangaPage();
        WebElement modal = waitForLibraryModal();

        openReadingStatusDropdown();
        selectReadingStatusOption("None");

        Thread.sleep(1000);

        WebElement updateButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[contains(@class,'md-modal__box')]//button[contains(@class,'primary') and contains(@class,'glow') and .//span[normalize-space()='Update']]")
        ));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", updateButton);

        wait.until(ExpectedConditions.invisibilityOf(modal));

        driver.get(LIBRARY_URL);

        boolean mangaStillInLibrary;
        try {
            driver.findElement(By.xpath(
                    "//a[contains(@class,'title')]//span[normalize-space()=\"" + mangaTitle + "\"]"
            ));
            mangaStillInLibrary = true;
        } catch (NoSuchElementException e) {
            mangaStillInLibrary = false;
        }

        System.out.println("Manga still in library: " + mangaStillInLibrary);

        Assert.assertFalse(mangaStillInLibrary,
                "The manga should no longer appear in the library after setting status to None.");
    }

    @Test
    public void testCycleAllLibraryStatuses() throws InterruptedException {
        performLogin(VALID_USERNAME, VALID_PASSWORD, false);

        driver.get(MANGA_URL);

        String[] statuses = {
                "Reading",
                "On Hold",
                "Dropped",
                "Plan to Read",
                "Completed",
                "Re-Reading",
                "None"
        };

        for (String status : statuses) {
            System.out.println("Setting status to: " + status);
            setMangaReadingStatus(status);
            Thread.sleep(2000);
        }

        System.out.println("Finished cycling through all library statuses.");
    }

    @Test
    public void testAddMangaToCompletedAndVerifyInLibrary() throws InterruptedException {
        performLogin(VALID_USERNAME, VALID_PASSWORD, false);

        driver.get(MANGA_URL);

        String mangaTitle = getMangaTitleText();
        System.out.println("Manga title on page: " + mangaTitle);

        setMangaReadingStatus("Completed");

        driver.get(LIBRARY_URL);

        WebElement completedTab = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(@class,'select__tab') and .//span[normalize-space()='Completed']]")
        ));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", completedTab);

        wait.until(ExpectedConditions.urlContains("tab=completed"));

        WebElement libraryTitleElement = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//a[contains(@class,'title')]//span[normalize-space()=\"" + mangaTitle + "\"]")
        ));

        String libraryTitle = libraryTitleElement.getText().trim();
        System.out.println("Manga title found in Completed tab: " + libraryTitle);

        Assert.assertEquals(libraryTitle, mangaTitle,
                "The manga should appear in the Completed tab of the library.");
    }

    @Test
    public void testLibraryNotAccessibleAfterLogout() throws InterruptedException {
        performLogin(VALID_USERNAME, VALID_PASSWORD, false);

        driver.get(LIBRARY_URL);

        boolean libraryVisibleBeforeLogout;
        try {
            libraryVisibleBeforeLogout = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//a[contains(@class,'title')]")
            )).isDisplayed();
        } catch (TimeoutException e) {
            libraryVisibleBeforeLogout = false;
        }

        System.out.println("Library visible before logout: " + libraryVisibleBeforeLogout);

        performLogout();
        Thread.sleep(2000);

        driver.get(LIBRARY_URL);
        Thread.sleep(2000);

        boolean loginButtonVisible;
        try {
            loginButtonVisible = driver.findElement(
                    By.xpath("//button[.//span[contains(normalize-space(),'Log In')]]")
            ).isDisplayed();
        } catch (NoSuchElementException e) {
            loginButtonVisible = false;
        }

        boolean libraryStillVisible;
        try {
            libraryStillVisible = driver.findElement(
                    By.xpath("//a[contains(@class,'title')]")
            ).isDisplayed();
        } catch (NoSuchElementException e) {
            libraryStillVisible = false;
        }

        System.out.println("Login button visible after logout: " + loginButtonVisible);
        System.out.println("Library visible after logout: " + libraryStillVisible);

        Assert.assertTrue(loginButtonVisible || !libraryStillVisible,
                "After logout, user should not have access to library content.");
    }
}