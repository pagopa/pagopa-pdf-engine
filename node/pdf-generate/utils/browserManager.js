const puppeteer = require('puppeteer');
const registerHelpers = require("handlebars-helpers");
let handlebars = require("handlebars");
let splitAndSpace = require('../helpers/splitAndSpace');
let not = require('../helpers/not')
let eq = require('../helpers/eq')
let lowercase = require('../helpers/lowercase');
let genQrCode = require('../helpers/genQrCode');
let genDataMatrix = require('../helpers/genDataMatrix');

let browser;

const getBrowserSession = async () => {
  if (browser) return browser;

  handlebars.registerHelper("not", not);
  handlebars.registerHelper("eq", eq);
  handlebars.registerHelper("splitAndSpace", splitAndSpace);
  handlebars.registerHelper("lowercase", lowercase);
  handlebars.registerHelper("genQrCode", genQrCode);
  handlebars.registerHelper("genDataMatrix", genDataMatrix);

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