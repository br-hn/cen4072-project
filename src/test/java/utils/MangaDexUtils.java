package utils;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class MangaDexUtils {

    public static final String MANGADEX = "https://mangadex.org";

    static final List<String> MetadataFields = List.of(
        "Authors",
        "Artists",
        "Genres",
        "Themes",
        "Demographic",
        "Alternative Titles"
    );

    public record SearchResult(
        String id,
        String title, 
        String description,
        String status,
        List<String> tags,
        WebElement card
    ) {
        public SearchResult(WebElement card) throws Exception {
            this(
                getMangaId(card.findElement(By.xpath(".//a")).getAttribute("href")),
                card.findElement(By.className("title")).getText(),
                card.findElement(By.className("description")).getText(),
                card.findElement(By.cssSelector("[style='grid-area: status;']")).getText(),
                card.findElements(By.cssSelector(".tags a[href^='/tag/']")).stream().map(t -> t.getAttribute("textContent")).toList(),
                card
            );
        }

        public String getStatus() {
            return card.findElement(By.cssSelector("[style='grid-area: status;']")).getText();
        }
    }

    static Pattern mangaIdPattern = Pattern.compile("title/([\\d\\w-]+)");

    public static String getMangaId(String url) throws Exception {
        var matcher = mangaIdPattern.matcher(url);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new RuntimeException();
        }
    }

    public static String openMangaPage(String id, WebDriver driver) {

        var og = driver.getWindowHandle();

        driver.switchTo().newWindow(WindowType.TAB);
        driver.get(MANGADEX + "/title/" + id);

        return og;
    }

    public static Map<String, List<String>> getMangaMetadata(String id, WebDriver driver) {

        var og = openMangaPage(id, driver);

        driver.manage().window().maximize();

        var wait = new FluentWait<>(driver)
            .pollingEvery(Duration.ofMillis(300))
            .withTimeout(Duration.ofSeconds(8));


        wait.until(ExpectedConditions.urlContains("/title/"));

        var fields = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("[id='" + id + "'] > div:not(.hidden)")));

        Map<String, List<String>> metadata = new HashMap<>();

        for (WebElement field : fields) {
            var props = field.findElements(By.xpath("./*"));

            var propName = props.get(0).getText();

            if (propName.equals("Author") || propName.equals("Artist")) propName += "s";

            if (!MetadataFields.contains(propName)) continue;

            if (propName.equals("Alternative Titles")) {
                metadata.put(
                    propName, 
                    props.stream().skip(1).map(t -> t.getText()).toList()
                );
            } else {
                metadata.put(
                    propName,
                    props.get(1).findElements(By.className("tag")).stream().map(t -> t.getText()).toList()
                );
            }
        }

        var publication = driver.findElement(By.xpath("//span[contains(., 'Publication')]"));
        var sypnosis = driver.findElement(By.cssSelector("[style='grid-area: synopsis;']"));

        metadata.put("Status", List.of(publication.getAttribute("textContent").substring(12).stripLeading()));

        metadata.put("Sypnosis", List.of(sypnosis.getText()));

        driver.close();

        driver.switchTo().window(og);

        return metadata;
    }

    public static Map<String, String> getQueryParams(String url) {
        try {
            URI uri = new URI(url);
            String query = uri.getQuery();

            if (query == null || query.isEmpty()) {
                return Collections.emptyMap();
            }

            return Arrays.stream(query.split("&"))
                .map(param -> param.split("=", 2))
                .collect(Collectors.toMap(
                    p -> URLDecoder.decode(p[0], StandardCharsets.UTF_8),
                    p -> p.length > 1
                        ? URLDecoder.decode(p[1], StandardCharsets.UTF_8)
                        : ""
                )
            );
        } catch (Exception e) {
            throw new RuntimeException("Invalid URL: " + url, e);
        }
    }

}