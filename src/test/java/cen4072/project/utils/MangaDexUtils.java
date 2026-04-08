package cen4072.project.utils;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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

    public record SearchResult(String title, String description, WebElement card) {
        public SearchResult(WebElement card) {
            this(
                card.findElement(By.className("title")).getText(),
                card.findElement(By.className("description")).getText(),
                card
            );
        }

        public String GetId() throws Exception {
            return getMangaId(card.findElement(By.cssSelector(":scope > a")).getAttribute("href"));
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

        var fields = wait.until(ExpectedConditions.presenceOfNestedElementsLocatedBy(By.id(id), By.cssSelector(":scope > div:not(.hidden)")));

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

        metadata.put("Status", List.of(publication.getText().substring(12).stripLeading().toLowerCase()));

        metadata.put("Sypnosis", List.of(sypnosis.getText()));

        driver.close();

        driver.switchTo().window(og);

        return metadata;
    }

}