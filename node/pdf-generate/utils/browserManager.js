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

let page;

const getPage = async () => {
  if (page) return page;

  const browser = await puppeteer.launch({
    headless: true,
  });

  page = await browser.newPage();

  return page;
};

module.exports = {getBrowserSession, getPage};