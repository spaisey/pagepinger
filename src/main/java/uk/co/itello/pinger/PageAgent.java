package uk.co.itello.pinger;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

@Service
public class PageAgent implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(PageAgent.class);
    private final Executor executor;
    private final RestTemplate restTemplate;
    private final URI configUrl;
    private final ExternalIp externalIp;
    private int loadCount;

    public PageAgent(Executor executor, RestTemplate restTemplate, ExternalIp externalIp,
                     @Value("${config.url}") String configUrl,
                     @Value("${tenant}") String tenant) {
        this.executor = executor;
        this.restTemplate = restTemplate;
        this.externalIp = externalIp;
        this.configUrl = fromHttpUrl(configUrl).path(tenant).build().toUri();
        LOG.info("Created PageAgent");
    }

    @PostConstruct
    public void submitRun() {
        executor.execute(this);
    }

    @Override
    public void run() {
        try {
            doRun();
        } finally {
            if (loadCount <  1) {
                sleep();
                submitRun();
            }
        }
    }

    private void doRun() {
        LOG.info("Starting PageAgent run");

        Config config = retrieveGlastoConfig();
        if (config == null) {
            LOG.info("No config available as yet");
            return;
        }

        if ("notset".equals(config.getUrl()) || config.getUrl() == null || config.getUrl().trim().length() == 0) {
            LOG.info("URL not set yet...");
            return;
        } else {
            LOG.debug("Received config: {}", config);
        }

        WebDriver driver = createDriver();
        try {
            driver.get(config.getUrl());
        } catch(WebDriverException e) {
            LOG.warn("Failed to load page", e);
            driver.quit();
            return;
        }

        boolean pageLoaded = config.getSearchCriteria()
                .stream()
                .anyMatch(criteria -> search(driver, criteria));

        if (pageLoaded) {
            LOG.info("Sending : {}", externalIp.getIp());
            loadCount++;
            restTemplate.postForObject(configUrl + "/notify", externalIp, Void.class);
        } else {
            driver.quit();
        }

        LOG.info("Completed PageAgent run");
    }

    private WebDriver createDriver() {
        ChromeDriver driver = new ChromeDriver();
        driver.manage().window().setSize(new Dimension(1024,768));
        driver.manage().timeouts().pageLoadTimeout(5, TimeUnit.MINUTES);
        driver.manage().timeouts().setScriptTimeout(2, TimeUnit.MINUTES);

        return driver;
    }

    private boolean search(WebDriver driver, String text) {
        LOG.info("Loading WebDriver");
        try {
            return driver.getPageSource().toLowerCase().contains(text);
        } catch (Exception e) {
            LOG.warn("Failed to find text on page: {}", text);
        }
        return false;
    }

    private void sleep()  {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Config retrieveGlastoConfig() {
        LOG.info("Attempting to retrieve config from {}", configUrl + "/config");
        try {
            return restTemplate.getForEntity(configUrl + "/config", Config.class).getBody();
        } catch (Exception e) {
            LOG.error("Failed to retrieve config: {}", e.getMessage());
        }

        return null;
    }

    public void reset() {
        loadCount = 0;
        submitRun();
    }
}
