const puppeteer = require('puppeteer');

let browser;

const getPage = async () => {
  if (browser) return browser;

  browser = await puppeteer.launch({
    headless: true,
    //args: ["--enable-gpu","--use-gl=egl","--use-angle=vulkan"]
    args: ["--headless","--no-sandbox"]
  });

  return browser;
};

module.exports = getPage;