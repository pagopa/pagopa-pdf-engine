const {chromium} = require('playwright');

let browser;

const getPage = async () => {
  if (browser) return browser;

  browser = await chromium.launch({
    headless: true
  });

  return browser;
};

module.exports = getPage;