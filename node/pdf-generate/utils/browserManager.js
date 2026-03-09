const puppeteer = require('puppeteer');
let handlebars = require("handlebars");
let { readFileSync, readdirSync, existsSync } = require('fs');
const path = require('node:path');
const HandlebarsI18n = require("handlebars-i18n");
const i18next = require("i18next");

const getFiles = source => readdirSync(source, { withFileTypes: true })
    .filter(dirent => !dirent.isDirectory())
    .map(dirent => dirent.name)
const importFile = (filePath, fileName) => readFileSync(`${filePath}/${fileName}`, "utf8");
const requireFile = (filePath, fileName) => require(`${filePath}/${fileName}`);

const partialPath = `./pdf-generate/partials`;
const helpersPath = `./pdf-generate/helpers`;
const localesPath = `./pdf-generate/locales`;
const envFolder = process.env.FOLDERS_TO_LOAD || "receipts";
console.log(envFolder);

const localeFilePath = `${localesPath}/${envFolder}/i18next.json`;
if (existsSync(localeFilePath)) {
    i18next.init({
        resources: JSON.parse(readFileSync(localeFilePath, "utf8")),
        lng: "it"
    });
} else {
    console.warn(`File traduzioni non trovato: ${localeFilePath}`);
}

HandlebarsI18n.init();

let browser;
const getBrowserSession = async () => {
    if (browser) return browser;

    // Register helpers
    const directoryHelperFiles = getFiles(`${helpersPath}/${envFolder}`);
    for (const directoryHelperFile of directoryHelperFiles) {
        const helper = requireFile(`../helpers/${envFolder}`, path.parse(`${helpersPath}/${envFolder}/${directoryHelperFile}`).name);
        handlebars.registerHelper(
            path.parse(`${helpersPath}/${envFolder}/${directoryHelperFile}`).name, helper);
    }

    // Register partials
    const directoryPartialFiles = getFiles(`${partialPath}/${envFolder}`);
    for (const directoryPartialFile of directoryPartialFiles) {
        const partial = importFile(`${partialPath}/${envFolder}`, directoryPartialFile);
        handlebars.registerPartial(
            path.parse(`${partialPath}/${envFolder}/${directoryPartialFile}`).name, partial);
    }

    browser = await puppeteer.launch({
        headless: true,
        args: ["--headless", "--no-sandbox", "--font-render-hinting=none"]
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