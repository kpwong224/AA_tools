import com.assertthat.selenium_shutterbug.core.Capture;
import com.assertthat.selenium_shutterbug.core.Shutterbug;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class URLScreenshot {
    private static final String SCREEN_SHOT_FOLDER = "screenCap";
    private static final Integer WAIT_TIME = 5000;


    public static void main(String[] args) {

        System.setProperty("webdriver.chrome.driver", "G:\\download\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");

        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--start-maximized");
//        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--window-size=1920,1080");

        WebDriver driver = new ChromeDriver(chromeOptions);

        var accessMappingList = readUrlsFromCsv("urls.csv");

        try {
            for (AccessMapping accessMap : accessMappingList) {
                String url = accessMap.getUrl();
                String fileName = accessMap.getFileName();
                String element = accessMap.getElement();
                String tag = accessMap.getTag();
                String words = accessMap.getWords();

                driver.get(url);
                waitForPageLoad(driver);


                if (tag != null && words != null)
                    clickElementByText(driver, tag, words);
                else if (element != null)
                    clickDivElement(driver, element);


                takeScreenshot(driver, SCREEN_SHOT_FOLDER + "\\" + fileName);
            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }


    private static List<AccessMapping> readUrlsFromCsv(String csvFilePath) {

        List<AccessMapping> accessMappingList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {

                if (line.startsWith("url,")) {
                    continue;
                }
                String[] values = line.split(",");

                AccessMapping accessMapping = new AccessMapping();
                if (values.length == 5) {
                    accessMapping.setUrl(values[0]);
                    accessMapping.setFileName(values[1]);
                    accessMapping.setElement(values[2]);
                    accessMapping.setTag(values[3]);
                    accessMapping.setWords(values[4]);
                } else if (values.length == 3) {
                    accessMapping.setUrl(values[0]);
                    accessMapping.setFileName(values[1]);
                    accessMapping.setElement(values[2]);
                } else {
                    accessMapping.setUrl(values[0]);
                    accessMapping.setFileName(values[1]);
                }
                accessMappingList.add(accessMapping);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return accessMappingList;
    }


    private static void takeScreenshot(WebDriver driver, String filePath) {
        try {

            Files.deleteIfExists(Paths.get(SCREEN_SHOT_FOLDER + "\\" + filePath));
            var screenByteArr = Shutterbug.wait(WAIT_TIME).shootPage(driver, Capture.FULL).getBytes(); //.save(SCREEN_SHOT_FOLDER + "\\");
            FileUtils.writeByteArrayToFile(new File(filePath), screenByteArr);
            log.info("Screenshot saved to " + filePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void waitForPageLoad(WebDriver driver) throws InterruptedException {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        int maxScrollAttempts = 10;
        int scrollAttempts = 0;

        while (scrollAttempts < maxScrollAttempts) {
            js.executeScript("window.scrollBy(0, 200);");
            Thread.sleep(500);
            scrollAttempts++;
        }

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIME));
        wait.until(webDriver -> ((org.openqa.selenium.JavascriptExecutor) webDriver)
                .executeScript("return document.readyState").equals("complete"));
    }

    private static void clickElementByText(WebDriver driver, String tag, String text) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIME));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
            // 使用 XPath 定位包含指定文字的元素
            String xpathExpression = String.format("//%s[contains(text(), '%s')]", tag, text);
            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpathExpression)));

            // 滾動到元素可見位置並向上偏移200像素
            js.executeScript("arguments[0].scrollIntoView(true); window.scrollBy(0, -200);", element);

            // 再次等待元素可點擊
            element = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpathExpression)));

            element.click();
            System.out.println("Clicked on the element containing text: " + text);

        } catch (TimeoutException e) {
            System.err.println("Timeout waiting for element containing text to be clickable: " + text);
        } catch (NoSuchElementException e) {
            System.err.println("No such element containing text found: " + text);
        } catch (Exception e) {
            System.err.println("Error clicking element containing text: " + text);
            e.printStackTrace();
        }
    }


    private static void clickDivElement(WebDriver driver, String divSelector) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIME));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
            WebElement divElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(divSelector)));

            int maxScrollAttempts = 10;
            int scrollAttempts = 0;
            boolean isClickable = false;

            while (scrollAttempts < maxScrollAttempts) {
                try {

                    js.executeScript("arguments[0].scrollIntoView(true); window.scrollBy(0, -200);", divElement);

                    divElement = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(divSelector)));
                    isClickable = true;
                    break;
                } catch (ElementClickInterceptedException | TimeoutException e) {

                    js.executeScript("window.scrollBy(0, 200);");
                    Thread.sleep(500);
                    scrollAttempts++;
                }
            }

            if (isClickable) {
                divElement.click();
                log.info("Clicked on the div element: " + divSelector);
            } else {
                log.error("Failed to click on the element after multiple scroll attempts: " + divSelector);
            }

        } catch (TimeoutException e) {
            log.error("Timeout waiting for element to be clickable: " + divSelector);
        } catch (NoSuchElementException e) {
            log.error("No such element found: " + divSelector);
        } catch (Exception e) {
            log.error("Error clicking element: " + divSelector);
            e.printStackTrace();
        }

    }


}