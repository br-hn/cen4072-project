

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.*;

import java.nio.file.Paths;
import java.time.Duration;

public class UserProfileTest {

    WebDriver driver;
    WebDriverWait wait;

    private static final String VALID_USERNAME = "SuperTestProjectAccount";
    private static final String VALID_PASSWORD = "SuperTestProjectAccount";

    private static final String PROFILE_IMAGE_PATH = Paths.get("src/main/resources/ProfilePicture.png").toAbsolutePath().toString();

    private static final String SETTINGS_URL   = "https://mangadex.org/settings#account";
    private static final String HOME_URL        = "https://mangadex.org/";

    private WebElement getProfileEditModal() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'md-modal__box')]")
        ));
    }

    private WebElement getAvatarSubsection() {
        return wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//h2[normalize-space()='Avatar']/ancestor::div[contains(@class,'flex-col') and contains(@class,'gap-2')][1]")
        ));
    }

    private WebElement getBannerSubsection() {
        return wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//h2[normalize-space()='Banner']/ancestor::div[contains(@class,'flex-col') and contains(@class,'gap-2')][1]")
        ));
    }

    private WebElement waitForFileInputInSection(String sectionName) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//section[.//h2[normalize-space()='" + sectionName + "']]//input[@type='file']")
        ));
    }

    @BeforeMethod
    public void setup() {

        ChromeOptions options = new ChromeOptions();

        driver = new ChromeDriver(options);

        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        driver.manage().window().maximize();
        driver.get("https://mangadex.org/settings");
    }

    @AfterMethod
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }



    public void performLogin(String username, String password, Boolean rememberme) throws InterruptedException {
        driver.get(SETTINGS_URL);


        // Click the "Log In" button on the settings page to open the Keycloak flow
        WebElement loginBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class,'md-btn')]" +
                        "//span[contains(normalize-space(),'Log In')]/..")));
        loginBtn.click();

        // Fill in Keycloak credentials
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

        // Wait for the post-login redirect back to mangadex.org
        wait.until(ExpectedConditions.urlContains("mangadex.org"));
        // Allow post-login JS / cookie writes to settle — flakiness fix for Test 1
        Thread.sleep(3000);
    }





    @Test
    public void testUploadNewProfilePicture() throws InterruptedException {
        performLogin(VALID_USERNAME, VALID_PASSWORD, false);
        driver.get(SETTINGS_URL);

        wait.until(ExpectedConditions.urlContains("settings"));

        WebElement accountFilterBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[contains(@class,'button-menu')]//button[.//span[normalize-space()='Account']]")
        ));
        accountFilterBtn.click();

        WebElement editProfileBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@id='profile']//button[contains(normalize-space(.),'Edit Profile')]")
        ));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", editProfileBtn);

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'md-modal__box')]")
        ));

        // ---------- REMOVE EXISTING AVATAR (if present) ----------
        try {
            WebElement avatarSubsection = getAvatarSubsection();

            WebElement hoverTarget = avatarSubsection.findElement(
                    By.cssSelector("div.relative.group")
            );

            new Actions(driver).moveToElement(hoverTarget).perform();
            Thread.sleep(500);

            WebElement removeButton = hoverTarget.findElement(
                    By.xpath(".//button[.//svg[contains(@class,'feather-x')]]")
            );

            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", removeButton);
            System.out.println("Existing avatar removed.");

            Thread.sleep(1000);

        } catch (NoSuchElementException | TimeoutException e) {
            System.out.println("No existing avatar to remove.");
        }

        // ---------- UPLOAD NEW AVATAR ----------
        WebElement avatarSubsection = getAvatarSubsection();
        WebElement avatarFileInput = avatarSubsection.findElement(
                By.xpath(".//input[@type='file']")
        );

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].style.display='block';" +
                        "arguments[0].style.visibility='visible';" +
                        "arguments[0].style.opacity='1';",
                avatarFileInput
        );

        avatarFileInput.sendKeys(PROFILE_IMAGE_PATH);
        Thread.sleep(1500);

        // ---------- SAVE ----------
        WebElement saveButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[contains(@class,'md-modal__box')]//button[@type='submit' and .//span[normalize-space()='Save']]")
        ));

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", saveButton);

        Thread.sleep(3000);
        System.out.println("Profile picture uploaded and saved successfully.");
    }




    @Test
    public void testUploadNewBanner() throws InterruptedException {
        performLogin(VALID_USERNAME, VALID_PASSWORD, false);
        driver.get(SETTINGS_URL);

        wait.until(ExpectedConditions.urlContains("settings"));

        WebElement accountFilterBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[contains(@class,'button-menu')]//button[.//span[normalize-space()='Account']]")
        ));
        accountFilterBtn.click();

        WebElement editProfileBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@id='profile']//button[contains(normalize-space(.),'Edit Profile')]")
        ));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", editProfileBtn);

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'md-modal__box')]")
        ));

        // ---------- REMOVE EXISTING BANNER (if present) ----------
        try {
            WebElement bannerSubsection = getBannerSubsection();

            WebElement hoverTarget = bannerSubsection.findElement(
                    By.cssSelector("div.relative.group")
            );

            new Actions(driver).moveToElement(hoverTarget).perform();
            Thread.sleep(500);

            WebElement removeButton = hoverTarget.findElement(
                    By.xpath(".//button[.//svg[contains(@class,'feather-x')]]")
            );

            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", removeButton);
            System.out.println("Existing banner removed.");

            Thread.sleep(1000);

        } catch (NoSuchElementException | TimeoutException e) {
            System.out.println("No existing banner to remove.");
        }

        // ---------- UPLOAD NEW BANNER ----------
        WebElement bannerSubsection = getBannerSubsection();
        WebElement bannerFileInput = bannerSubsection.findElement(
                By.xpath(".//input[@type='file']")
        );

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].style.display='block';" +
                        "arguments[0].style.visibility='visible';" +
                        "arguments[0].style.opacity='1';",
                bannerFileInput
        );

        bannerFileInput.sendKeys(PROFILE_IMAGE_PATH);
        Thread.sleep(1500);

        // ---------- SAVE ----------
        WebElement saveButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[contains(@class,'md-modal__box')]//button[@type='submit' and .//span[normalize-space()='Save']]")
        ));

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", saveButton);

        Thread.sleep(3000);
        System.out.println("Banner uploaded and saved successfully.");
    }

    @Test
    public void testRemoveProfilePicture() throws InterruptedException {
        performLogin(VALID_USERNAME, VALID_PASSWORD, false);
        driver.get(SETTINGS_URL);

        wait.until(ExpectedConditions.urlContains("settings"));

        WebElement accountFilterBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[contains(@class,'button-menu')]//button[.//span[normalize-space()='Account']]")
        ));
        accountFilterBtn.click();
        Thread.sleep(1000);

        WebElement editProfileBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@id='profile']//button[contains(normalize-space(.),'Edit Profile')]")
        ));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", editProfileBtn);

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'md-modal__box')]")
        ));
        Thread.sleep(1000);

        try {
            WebElement avatarContainer = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//div[contains(@class,'rounded-full') and contains(@class,'max-w-52')]//div[contains(@class,'relative') and contains(@class,'group')]")
            ));

            System.out.println("Avatar container found.");
            System.out.println("AVATAR CONTAINER HTML:");
            System.out.println(avatarContainer.getAttribute("outerHTML"));

            // Force overlay visible
            WebElement overlay = avatarContainer.findElement(
                    By.xpath(".//div[contains(@class,'absolute') and contains(@class,'inset-0')]")
            );

            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].style.opacity='1';" +
                            "arguments[0].style.visibility='visible';" +
                            "arguments[0].style.pointerEvents='auto';",
                    overlay
            );

            Thread.sleep(500);

            // SIMPLER: there is only one button inside this avatar container
            WebElement removeButton = avatarContainer.findElement(By.xpath(".//button"));

            System.out.println("Remove button found.");
            System.out.println(removeButton.getAttribute("outerHTML"));

            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", removeButton);
            System.out.println("Remove button clicked.");

            Thread.sleep(1000);

        } catch (NoSuchElementException | TimeoutException e) {
            System.out.println("No existing avatar to remove.");
            return;
        }

        // Click Save multiple times because one click does not reliably persist removal
        for (int i = 1; i <= 3; i++) {
            try {
                WebElement saveButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//div[contains(@class,'md-modal__box')]//button[@type='submit' and .//span[normalize-space()='Save']]")
                ));

                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", saveButton);
                System.out.println("Clicked Save (" + i + "/3)");

                Thread.sleep(1500);

            } catch (StaleElementReferenceException e) {
                i--;
                Thread.sleep(1000);
            }
        }

        Thread.sleep(3000);
        System.out.println("Profile picture removal attempt complete.");
    }

    @Test
    public void testCycleThroughProfileTabs() throws InterruptedException {
        performLogin(VALID_USERNAME, VALID_PASSWORD, false);

        driver.get("https://mangadex.org/user/me");
        wait.until(ExpectedConditions.urlContains("/user/me"));

        Thread.sleep(2000);

        // Get ALL tab elements by visible text
        String[] tabs = {"Uploads", "Info", "Lists", "Posts"};

        for (String tabName : tabs) {
            WebElement tab = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[contains(@class,'cursor-pointer')][.//div[normalize-space()='" + tabName + "']]")
            ));

            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", tab);
            System.out.println("Clicked tab: " + tabName);

            Thread.sleep(2000);

            // Assert the tab is now active (opacity-100 OR aria-current)
            boolean isActive = tab.getAttribute("class").contains("opacity-100")
                    || "page".equals(tab.getAttribute("aria-current"));

            Assert.assertTrue(isActive, tabName + " tab did not activate properly");
        }
    }

    @Test
    public void testConnectExternalAccountRedirect() throws InterruptedException {
        performLogin(VALID_USERNAME, VALID_PASSWORD, false);

        driver.get("https://mangadex.org/user/me");
        wait.until(ExpectedConditions.urlContains("/user/me"));

        Thread.sleep(2000);

        // ---------- GO TO INFO TAB ----------
        WebElement infoTab = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[contains(@class,'cursor-pointer')][.//div[normalize-space()='Info']]")
        ));

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", infoTab);
        System.out.println("Opened Info tab");

        Thread.sleep(2000);

        // ---------- CLICK "CONNECT EXTERNAL ACCOUNT" ----------
        WebElement connectBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[.//span[contains(normalize-space(),'Connect an external account')]]")
        ));

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", connectBtn);
        System.out.println("Opened external account modal");

        // Wait for modal
        WebElement modal = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'md-modal__box')]")
        ));

        Thread.sleep(2000);

        // ---------- CLICK NAMICOMI BUTTON ----------
        WebElement namicomiBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[.//p[normalize-space()='NamiComi']]")
        ));

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", namicomiBtn);
        System.out.println("Clicked NamiComi");

        Thread.sleep(3000);

        // ---------- VERIFY REDIRECT ----------
        String currentUrl = driver.getCurrentUrl();
        System.out.println("Redirected URL: " + currentUrl);

        Assert.assertTrue(currentUrl.contains("auth.namicomi.com"),
                "Expected redirect to NamiComi auth, but got: " + currentUrl);
    }


}
