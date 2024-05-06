const puppeteer = require('puppeteer');
let handlebars = require("handlebars");
let { readFileSync, readdirSync } = require('fs');
const path = require('node:path');

const getDirectories = source => readdirSync(source, { withFileTypes: true })
.filter(dirent => dirent.isDirectory())
    .map(dirent => dirent.name)
const getFiles = source => readdirSync(source, { withFileTypes: true })
.filter(dirent => !dirent.isDirectory())
    .map(dirent => dirent.name)
const importFile = (filePath, fileName) => readFileSync(`${filePath}/${fileName}`, "utf8");
const partialPath = `./pdf-generate/helpers`;
const helpersPath = `./pdf-generate/helpers`;


let browser;

const getBrowserSession = async () => {
  if (browser) return browser;

  // Register helpers

  const helperDirectories = getDirectories(helpersPath);
  for (directoryHelper of helperDirectories) {
    const directoryHelperFiles = getFiles(`${helpersPath}/${directoryHelper}`);
    for (directoryHelperFile of directoryHelperFiles) {
        const helper = importFile(`${helpersPath}/${directoryHelper}`, directoryHelperFile);
        handlebars.registerHelper(
            path.parse(`${helpersPath}/${directoryHelper}/${directoryHelperFile}`).name , helper);
    }
  }

  // Register partials
  const partialDirectories = getDirectories(partialPath);
  for (directoryPartial of partialDirectories) {
    const directoryPartialFiles = getFiles(`${partialPath}/${directoryPartial}`);
    for (directoryPartialFile of directoryPartialFiles) {
        const partial = importFile(`${partialPath}/${directoryPartial}`, directoryPartialFile);
        handlebars.registerPartial(
            path.parse(`${partialPath}/${directoryPartial}/${directoryPartialFile}`).name , partial);
    }
  }

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