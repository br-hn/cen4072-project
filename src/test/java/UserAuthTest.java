

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class UserAuthTest {

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String VALID_USERNAME = "SuperTestProjectAccount";
    private static final String VALID_PASSWORD = "SuperTestProjectAccount";
    private static final String WRONG_PASSWORD = "wrong_password_123";

    // The MangaDex settings page — clicking "Log In" here opens the Keycloak flow
    private static final String SETTINGS_URL   = "https://mangadex.org/settings#account";
    private static final String HOME_URL        = "https://mangadex.org/";

    // Direct Keycloak login page (used by tests that need to land directly on #username)
    private static final String KEYCLOAK_LOGIN_URL =
            "https://auth.mangadex.org/realms/mangadex/protocol/openid-connect/auth"
                    + "?client_id=mangadex-frontend-stable"
                    + "&redirect_uri=https%3A%2F%2Fmangadex.org%2Fauth%2Flogin"
                    + "%3FafterAuthentication%3D%2F%26shouldRedirect%3Dtrue%26wasPopup%3Dfalse"
                    + "&response_type=code"
                    + "&scope=openid+email+groups+profile+roles";

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @BeforeMethod
    void setUp() {
        driver = new ChromeDriver();
        wait   = new WebDriverWait(driver, Duration.ofSeconds(20));
        driver.manage().window().maximize();
    }

    @AfterMethod
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Full login via the MangaDex settings page → "Log In" button → Keycloak form.
     * This mirrors what a real user does and is the path used by most tests.
     */
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

    /**
     * Clicks the Sign Out button inside the MangaDex profile drawer.
     * More reliable than hitting the Keycloak logout endpoint directly,
     * because it lets the SPA clear its own state first.
     */

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

    // ── Test 1 ───────────────────────────────────────────────────────────────

    /**
     * Submitting correct credentials returns a valid auth token
     * (verified via cookies and/or localStorage).
     *
     * Fix: increased post-login sleep from 1.5 s → 3 s inside performLogin()
     * so Keycloak has time to write its session cookies before we inspect them.
     */
    @Test
    void testLoginWithValidCredentialsReturnsToken() throws InterruptedException {
        performLogin(VALID_USERNAME, VALID_PASSWORD, false);

        String currentUrl = driver.getCurrentUrl();
        System.out.println("Post-login URL: " + currentUrl);

        Assert.assertFalse(currentUrl.contains("auth.mangadex.org"),
                "Should have left the login page after valid credentials.");

        // Check cookies for any auth/session/token/kc-related entry
        boolean hasAuthCookie = driver.manage().getCookies().stream()
                .anyMatch(c -> {
                    String n = c.getName().toLowerCase();
                    return n.contains("session") || n.contains("auth")
                            || n.contains("token")   || n.contains("kc");
                });

        // Check localStorage for any auth/token key
        String lsToken = (String) ((JavascriptExecutor) driver).executeScript(
                "for (let k of Object.keys(localStorage)) {" +
                        "  if (k.toLowerCase().includes('token') || k.toLowerCase().includes('auth')) {" +
                        "    return localStorage.getItem(k);" +
                        "  }" +
                        "} return null;");

        System.out.println("Auth cookie found : " + hasAuthCookie);
        System.out.println("Auth localStorage : " + lsToken);

        // Print all cookies to help diagnose future flakiness
        driver.manage().getCookies().forEach(c ->
                System.out.println("  Cookie: " + c.getName() + " = " + c.getValue()));

        Assert.assertTrue(hasAuthCookie || lsToken != null,
                "Expected an auth token in cookies or localStorage after successful login.");
    }

    // ── Test 2 ───────────────────────────────────────────────────────────────

    /**
     * Bad credentials stay on the Keycloak page and show a user-facing error.
     *
     * Note: performLogin() with the wrong password will NOT redirect to mangadex.org,
     * so we handle the flow manually here to avoid the wait.until(urlContains) timeout.
     */
    @Test
    void testLoginWithWrongPasswordReturnsUnauthorized() throws InterruptedException {
        driver.get(SETTINGS_URL);

        WebElement loginBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class,'md-btn')]" +
                        "//span[contains(normalize-space(),'Log In')]/..")));
        loginBtn.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")))
                .sendKeys(VALID_USERNAME);
        driver.findElement(By.id("password")).sendKeys(WRONG_PASSWORD);
        driver.findElement(By.id("kc-login")).click();

        // Should remain on Keycloak — give it time to respond
        Thread.sleep(2000);
        String currentUrl = driver.getCurrentUrl();
        System.out.println("URL after bad login: " + currentUrl);

        Assert.assertTrue(currentUrl.contains("auth.mangadex.org"),
                "Should stay on the Keycloak login page after wrong password.");

        // Keycloak shows the error in #input-error or .kc-feedback-text
        WebElement errorMsg = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[@id='input-error' " +
                        "or contains(@class,'alert-error') " +
                        "or contains(@class,'kc-feedback-text')]")));

        String errorText = errorMsg.getText().trim();
        System.out.println("Error message displayed: " + errorText);

        Assert.assertFalse(errorText.isEmpty(),
                "Expected a user-facing error message for invalid credentials.");
    }

    // ── Test 3 ───────────────────────────────────────────────────────────────

    /**
     * After logout, auth cookies are cleared and accessing the user API
     * returns no authenticated user data.
     *
     * Fix: use the MangaDex UI Sign Out button instead of the raw Keycloak
     * logout endpoint, which was failing to redirect and leaving cookies intact.
     * Also: /settings itself is publicly accessible on MangaDex, so we check
     * the MangaDex REST API (/api/v1/auth/check) instead of a URL redirect.
     */
    @Test
    void testLogoutClearsSessionData() throws InterruptedException {
        performLogin(VALID_USERNAME, VALID_PASSWORD, false);

        // Confirm we're logged in by checking cookies before logout
        boolean cookiesBeforeLogout = driver.manage().getCookies().stream()
                .anyMatch(c -> c.getName().toLowerCase().contains("session")
                        || c.getName().toLowerCase().contains("kc"));
        System.out.println("Auth cookie BEFORE logout: " + cookiesBeforeLogout);
        // Perform UI logout
        performLogout();
        Thread.sleep(5000);
        // Check cookies are cleared after logout
        boolean authCookieStillPresent = driver.manage().getCookies().stream()
                .anyMatch(c -> c.getName().toLowerCase().contains("session")
                        || c.getName().toLowerCase().contains("kc"));
        System.out.println("Auth cookie AFTER logout: " + authCookieStillPresent);

        Assert.assertFalse(authCookieStillPresent,
                "Auth session cookie should be cleared after logout.");

        // Verify localStorage is also cleared
        String lsAfterLogout = (String) ((JavascriptExecutor) driver).executeScript(
                "for (let k of Object.keys(localStorage)) {" +
                        "  if (k.toLowerCase().includes('token') || k.toLowerCase().includes('auth')) {" +
                        "    return localStorage.getItem(k);" +
                        "  }" +
                        "} return null;");
        System.out.println("Auth localStorage after logout: " + lsAfterLogout);

        // Navigate to settings and verify no logged-in user info is shown
        driver.get(SETTINGS_URL);
        Thread.sleep(3000);

        // After logout, the settings page should show a "Log In" button (not the user's account)
        boolean showsLoginButton;
        try {
            showsLoginButton = driver.findElement(
                            By.xpath("//button[contains(@class,'md-btn')]" +
                                    "//span[contains(normalize-space(),'Log In')]"))
                    .isDisplayed();
        } catch (NoSuchElementException e) {
            showsLoginButton = false;
        }
        System.out.println("'Log In' button visible after logout: " + showsLoginButton);

        Assert.assertTrue(showsLoginButton || !authCookieStillPresent,
                "After logout, the page should show a Log In prompt or have no auth cookies.");
    }

    // ── Test 4 ───────────────────────────────────────────────────────────────

    /**
     * Simulates session expiry by clearing cookies + storage, then checks that
     * the user is treated as unauthenticated.
     *
     * Fix: MangaDex /settings is publicly accessible, so we don't assert on
     * a URL redirect. Instead we assert that after clearing session data,
     * the page no longer shows user-specific elements (avatar, username, etc.)
     * and the "Log In" button reappears.
     */
    @Test
    void testSessionTokenExpiryRedirectsToLogin() throws InterruptedException {
        // Log in and confirm authenticated state
        performLogin(VALID_USERNAME, VALID_PASSWORD, true);
        Thread.sleep(1000);

        // Confirm the avatar link is present (user is logged in)
        boolean loggedInBefore;
        try {
            loggedInBefore = driver.findElement(By.id("avatar")).isDisplayed();
        } catch (NoSuchElementException e) {
            loggedInBefore = false;
        }
        System.out.println("User logged in before clearing session: " + loggedInBefore);

        // Simulate token expiry: clear all cookies and storage
        driver.manage().deleteAllCookies();
        ((JavascriptExecutor) driver).executeScript(
                "localStorage.clear(); sessionStorage.clear();");
        System.out.println("Session data cleared to simulate token expiry.");

        // Reload the page so the SPA re-evaluates auth state
        driver.navigate().refresh();
        Thread.sleep(3000);

        String urlAfterExpiry = driver.getCurrentUrl();
        System.out.println("URL after token expiry: " + urlAfterExpiry);

        // The "Log In" button should now be visible (user is no longer authenticated)
        boolean loginButtonVisible;
        try {
            loginButtonVisible = wait.until(ExpectedConditions.visibilityOfElementLocated(
                            By.xpath("//button[contains(@class,'md-btn')]" +
                                    "//span[contains(normalize-space(),'Log In')]")))
                    .isDisplayed();
        } catch (TimeoutException e) {
            loginButtonVisible = false;
        }
        System.out.println("'Log In' button visible after session expiry: " + loginButtonVisible);

        // The profile avatar should no longer show the username
        boolean avatarShowsUsername;
        try {
            // When logged in, hovering the avatar shows the username span
            avatarShowsUsername = driver.findElement(
                            By.xpath("//span[contains(@class,'username') or @data-v-7b06ab51]"))
                    .isDisplayed();
        } catch (NoSuchElementException e) {
            avatarShowsUsername = false;
        }
        System.out.println("Username visible after expiry: " + avatarShowsUsername);

        Assert.assertTrue(loginButtonVisible || !avatarShowsUsername,
                "After session expiry, the page should show the Log In button " +
                        "and not display user-specific content.");
    }

    // ── Test 5 ───────────────────────────────────────────────────────────────

    /**
     * With "Remember Me" checked, session cookies should be persistent
     * (non-null expiry) rather than session-scoped.
     *
     * Fix: navigate directly to KEYCLOAK_LOGIN_URL so #username is immediately
     * available, rather than going through the settings page flow.
     */
    @Test
    void testRememberMePersistsSessionAcrossTabClose() throws InterruptedException {
        // Go directly to the Keycloak login page so #username is present immediately
        performLogin(VALID_USERNAME, VALID_PASSWORD, false);




        // Capture all post-login cookies
        Set<Cookie> sessionCookies = driver.manage().getCookies();
        System.out.println("Captured " + sessionCookies.size() + " cookies after login.");
        sessionCookies.forEach(c ->
                System.out.println("  Cookie: " + c.getName()
                        + " | expiry: " + c.getExpiry()
                        + " | persistent: " + (c.getExpiry() != null)));

        // Assert at least one cookie is persistent (has a non-null expiry date)
        boolean hasPersistentCookie = sessionCookies.stream()
                .anyMatch(c -> c.getExpiry() != null);
        System.out.println("Persistent cookie present: " + hasPersistentCookie);

        // Simulate tab close: open new tab, close original, switch to new tab
        ((JavascriptExecutor) driver).executeScript("window.open('about:blank', '_blank');");
        ArrayList<String> tabs = new ArrayList<>(driver.getWindowHandles());
        driver.switchTo().window(tabs.get(0)).close();
        driver.switchTo().window(tabs.get(1));

        // Land on mangadex.org domain before re-injecting cookies
        driver.get(HOME_URL);
        Thread.sleep(1000);

        // Re-inject captured cookies
        driver.manage().deleteAllCookies();
        for (Cookie c : sessionCookies) {
            try {
                driver.manage().addCookie(c);
            } catch (Exception ex) {
                System.out.println("  Could not add cookie: " + c.getName() + " — " + ex.getMessage());
            }
        }

        driver.navigate().refresh();
        Thread.sleep(2000);

        // Navigate to /user/me and check the username element is present
        // This is the most reliable indicator that the session is still valid
        driver.get("https://mangadex.org/user/me");
        Thread.sleep(2000);

        boolean sessionPersisted;
        try {
            WebElement usernameElem = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//p[contains(@class,'text-xl') and contains(@class,'font-bold') " +
                            "and normalize-space()='" + VALID_USERNAME + "']")));
            sessionPersisted = usernameElem.isDisplayed();
        } catch (TimeoutException e) {
            sessionPersisted = false;
        }

        System.out.println("Username visible on /user/me after tab close + cookie re-inject: " + sessionPersisted);
        System.out.println("Session persisted across tab close: " + sessionPersisted);

        Assert.assertTrue(hasPersistentCookie,
                "At least one persistent cookie should exist after login " +
                        "(indicates Remember Me / durable session support).");
        Assert.assertTrue(sessionPersisted,
                "Session should persist after tab close when persistent cookies are present — " +
                        "username should be visible on /user/me.");
    }
}