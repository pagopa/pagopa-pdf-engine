const puppeteer = require('puppeteer');

let browser;

const getPage = async () => {
  if (browser) return browser;

  browser = await puppeteer.launch({
    headless: true,
  });

  return browser;
};

module.exports = getPage;