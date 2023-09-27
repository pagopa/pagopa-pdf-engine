const puppeteer = require('puppeteer');
const registerHelpers = require("handlebars-helpers");

let browser;

const getBrowserSession = async () => {
  if (browser) return browser;

  registerHelpers();

  browser = await puppeteer.launch({
    headless: true,
    args: ["--headless","--no-sandbox", "--font-render-hinting=none"]
  });

  return browser;
};

const closeBrowserSession = async () => {
    if (browser) {
        browser.close();
    }
    return;
}

module.exports = { getBrowserSession, closeBrowserSession };