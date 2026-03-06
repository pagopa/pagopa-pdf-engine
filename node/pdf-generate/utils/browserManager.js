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
const requireFile = (filePath, fileName) => require(`${filePath}/${fileName}`);

const partialPath = `./pdf-generate/partials`;
const helpersPath = `./pdf-generate/helpers`;

let browser;

const HandlebarsI18n = require("handlebars-i18n");

const i18next = require("i18next");
i18next.init({
    resources: JSON.parse(readFileSync('./pdf-generate/assets/i18next.json', 'utf8')),
    lng: "it"
});

HandlebarsI18n.init();

const getBrowserSession = async () => {
  if (browser) return browser;

  const envFolders = process.env.FOLDERS_TO_LOAD || "receipts";
  let includedFolders = [];
  if (envFolders) {
          includedFolders = envFolders.split(',')
          .map(folder => folder.trim())
          .filter(folder => folder.length > 0);
  }

  const includeAll = includedFolders.length === 0;

  console.log(includedFolders);

  // Register helpers
 const allHelperDirectories = getDirectories(helpersPath);
    const helperDirectoriesToLoad = includeAll
        ? allHelperDirectories
        : allHelperDirectories.filter(dir => includedFolders.includes(dir)); // Filtro
  for (directoryHelper of helperDirectoriesToLoad) {
    const directoryHelperFiles = getFiles(`${helpersPath}/${directoryHelper}`);
    for (directoryHelperFile of directoryHelperFiles) {
        const helper = requireFile(`../helpers/${directoryHelper}`, path.parse(`${helpersPath}/${directoryHelper}/${directoryHelperFile}`).name);
        handlebars.registerHelper(
            path.parse(`${helpersPath}/${directoryHelper}/${directoryHelperFile}`).name , helper);
    }
  }

  // Register partials
  const allPartialDirectories = getDirectories(partialPath);
    const partialDirectoriesToLoad = includeAll
        ? allPartialDirectories
        : allPartialDirectories.filter(dir => includedFolders.includes(dir)); // Filtro
  for (directoryPartial of partialDirectoriesToLoad) {
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