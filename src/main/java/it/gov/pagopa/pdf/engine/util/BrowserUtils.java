package it.gov.pagopa.pdf.engine.util;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

import java.io.File;

public class BrowserUtils {

    private static BrowserUtils instance = null;
    private Browser browser;

    private BrowserUtils() {
        browser = Playwright.create().chromium()
                .launch(new BrowserType.LaunchOptions().setHeadless(true));
        browser.newContext();
    }

    public static BrowserUtils getInstance() {
        if (instance == null) {
            instance = new BrowserUtils();
        }
        return instance;
    }

    public Browser getBrowser() {
        return browser;
    }
}
