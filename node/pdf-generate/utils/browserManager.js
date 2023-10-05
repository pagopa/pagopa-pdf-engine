const puppeteer = require('puppeteer');
const registerHelpers = require("handlebars-helpers");
let handlebars = require("handlebars");
let splitAndSpace = require('../helpers/splitAndSpace');
let not = require('../helpers/not')
let eq = require('../helpers/eq')

let browser;

const getBrowserSession = async () => {
  if (browser) return browser;

  handlebars.registerHelper("not", splitAndSpace);
  handlebars.registerHelper("eq", splitAndSpace);
  handlebars.registerHelper("splitAndSpace", splitAndSpace);

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