const puppeteer = require('puppeteer');
const registerHelpers = require("handlebars-helpers");

let browser;

const getBrowserSession = async () => {
  if (browser) return browser;

  registerHelpers();

  browser = await puppeteer.launch({
    headless: true,
    args: ["--headless","--no-sandbox"]
  });

  return browser;
};

module.exports = getBrowserSession;