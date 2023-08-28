const {chromium} = require('playwright');

let browser;

const getPage = async () => {
  if (browser) return browser;

  browser = await chromium.launch({
    headless: true,
    //args: ["--enable-gpu","--use-gl=egl","--use-angle=vulkan"]
    args: ["--headless","--no-sandbox","--use-angle=gl","--enable-gpu","--use-gl=gl"]
  });

  return browser;
};

module.exports = getPage;