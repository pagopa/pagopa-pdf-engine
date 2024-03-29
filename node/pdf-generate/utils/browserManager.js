const puppeteer = require('puppeteer');
let handlebars = require("handlebars");
let splitAndSpace = require('../helpers/splitAndSpace');
let not = require('../helpers/not')
let eq = require('../helpers/eq')
let lowercase = require('../helpers/lowercase');

let browser;

const getBrowserSession = async () => {
  if (browser) return browser;

  handlebars.registerHelper("not", not);
  handlebars.registerHelper("eq", eq);
  handlebars.registerHelper("splitAndSpace", splitAndSpace);
  handlebars.registerHelper("lowercase", lowercase);

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