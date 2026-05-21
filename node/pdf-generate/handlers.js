const puppeteer = require('puppeteer');
const path = require('path');
const fs = require('fs');
const readFileSync = require('fs').readFileSync
const rmSync = require('fs').rmSync
const os = require('os');
const {getBrowserSession, closeBrowserSession} = require('./utils/browserManager');
const buildResponseBody = require('./utils/buildUtils');
const multer = require('multer');
const express = require('express');
let handlebars = require("handlebars");
const packageJson = require("../package.json");
var AdmZip = require("adm-zip");
const fse = require('fs-extra');
const telemetryClient = require('./utils/telemetry');
const crypto = require('crypto');

// ---------------------------------------------------------------------------
// Lightweight performance instrumentation.
//
// Activate with PERF_LOG=1 (or =true). When disabled the helper functions
// reduce to no-ops, so leaving the calls in production is safe.
// All timings are in milliseconds with sub-ms precision (process.hrtime.bigint).
// Output format (single line, easy to grep / parse):
//   PERF | reqId=<id> phase=<name> ms=<value>
//   PERF_TOTAL | reqId=<id> total_ms=<value> phases=<json>
// ---------------------------------------------------------------------------
const PERF_LOG = /^(1|true|yes)$/i.test(String(process.env.PERF_LOG || true));

function nowNs() { return process.hrtime.bigint(); }
function nsToMs(ns) { return Number(ns) / 1e6; }


// Emit a perf event through Application Insights (trackCustomEvent) and,
// as a fallback for local runs without telemetry, also to stdout.
// Kept centralized so the payload shape stays consistent across all phases.
function emitPerfEvent(name, properties, measurements) {
    trackCustomEvent({
        name: "PDF_ENGINE_NODE",
        properties: {
            type: "PDF_ENGINE_NODE_PERF",
            title: name,
            ...properties
        },
        measurements
    });
    // Mirror to console for local development / docker logs.
    if (!telemetryClient) {
        console.log(`PERF | ${name} ${JSON.stringify({ ...properties, ...measurements })}`);
    }
}

function createPerfTracker(reqId) {
    const phases = {};
    const startTotal = nowNs();
    const recordPhase = (phaseName, ms) => {
        phases[phaseName] = (phases[phaseName] || 0) + ms;
        emitPerfEvent("PDF_ENGINE_NODE_PERF_PHASE", {
            reqId,
            phase: phaseName
        }, {
            duration_ms: ms
        });
    };
    return {
        reqId,
        // Measure a synchronous or async function call.
        async measure(name, fn) {
            if (!PERF_LOG) return fn();
            const s = nowNs();
            try {
                return await fn();
            } finally {
                recordPhase(name, nsToMs(nowNs() - s));
            }
        },
        // Manual span (when measure() doesn't fit, e.g. interleaved code).
        start(name) {
            if (!PERF_LOG) return () => {};
            const s = nowNs();
            return () => recordPhase(name, nsToMs(nowNs() - s));
        },
        flush(extra) {
            if (!PERF_LOG) return;
            const total = nsToMs(nowNs() - startTotal);
            // Build a measurements object: total_ms + every phase as its own
            // numeric metric (App Insights will index them as customMetrics).
            const measurements = { total_ms: total };
            for (const [k, v] of Object.entries(phases)) {
                measurements[`phase_${k}_ms`] = v;
            }
            if (extra) {
                for (const [k, v] of Object.entries(extra)) {
                    if (typeof v === 'number') measurements[k] = v;
                }
            }
            emitPerfEvent("PDF_ENGINE_NODE_PERF_TOTAL", {
                reqId,
                phases: JSON.stringify(phases),
                extra: extra ? JSON.stringify(extra) : undefined
            }, measurements);
        }
    };
}

// Cache of compiled Handlebars templates keyed by the SHA-1 of the
// template source. The same `template.html` is used over and over again
// across requests, so recompiling it every time is pure overhead.
// A small bounded LRU-like cache keeps memory usage in check.
const TEMPLATE_CACHE_MAX = 32;
const compiledTemplateCache = new Map();

function getCompiledTemplate(source) {
    const key = crypto.createHash('sha1').update(source).digest('hex');
    let tpl = compiledTemplateCache.get(key);
    if (tpl) {
        // refresh LRU position
        compiledTemplateCache.delete(key);
        compiledTemplateCache.set(key, tpl);
        return tpl;
    }
    tpl = handlebars.compile(source);
    compiledTemplateCache.set(key, tpl);
    if (compiledTemplateCache.size > TEMPLATE_CACHE_MAX) {
        const oldest = compiledTemplateCache.keys().next().value;
        compiledTemplateCache.delete(oldest);
    }
    return tpl;
}


const info = async function (req, res, next) {

    console.log(`INFO : name ${packageJson.name} version ${packageJson.version}`);

    res.send({
        name: packageJson.name,
        version: packageJson.version
    });

}

const shutdown = async function (req, res, server) {
    res.send("Shutdown");
    process.exit(0);
}

function trackCustomEvent(msg) {
    if (!telemetryClient) return;
    try {
        telemetryClient.trackEvent(msg);
    } catch (e) {
        console.error("custom event tracking failed", e);
    }
}

const generatePdf = async function (req, res, next) {

    const reqId = (req.headers && (req.headers['x-request-id'] || req.headers['x-correlation-id']))
        || crypto.randomBytes(6).toString('hex');
    const perf = createPerfTracker(reqId);

    trackCustomEvent({
        name: "PDF_ENGINE_NODE",
        properties: {
            "type": "PDF_ENGINE_NODE_LOG",
            "title": "Generate PDF NodeJS Function Invoked",
        }
    });

    var workingDir;
    var page;

    let timestampLog = `${Date.now()}`;
    let zipEntryCount = 0;
    let waitRenderIterations = 0;
    let pdfBytes = 0;

    console.time(timestampLog);
    console.info(`Starting generate pdf nodejs function reqId=${reqId}`);

    try {

        try {
            const endMkdir = perf.start('mkdtemp');
            workingDir = fs.mkdtempSync(path.join(os.tmpdir(), 'pdfenginetmp-'));
            endMkdir();
        } catch (err) {
            trackCustomEvent({
                name: "PDF_ENGINE_NODE",
                properties: {
                    "type": "PDF_ENGINE_NODE_ERROR",
                    "title": "An error occurred on processing the request",
                    "details": "PDFE_908 - Error creating working directory",
                    "cause": err.toString()
                }
            });
            res.status(500);
            res.body(buildResponseBody(500, 'PDFE_908', "An error occurred on processing the request"));
            return;
        }

        console.log(req.file)

        var zip = new AdmZip(req.file.buffer);
        var zipEntries = zip.getEntries();

        // NOTE: previously fse.outputFile was fired in a fire-and-forget
        // fashion inside the loop, which (a) made it impossible to know
        // when the files were really on disk and (b) hid the real cost of
        // this phase from any timing logs. We now await every write so the
        // perf measurement is meaningful and the page.goto below cannot
        // race the writes.
        const endZip = perf.start('zip_extract');
        const writes = [];
        for (const zipEntry of zipEntries) {
            if (!zipEntry.entryName.includes("._") && !zipEntry.isDirectory) {
                zipEntryCount++;
                writes.push(
                    fse.outputFile(path.join(workingDir, zipEntry.entryName), zipEntry.getData())
                        .catch(err => {
                            trackCustomEvent({
                                name: "PDF_ENGINE_NODE",
                                properties: {
                                    "type": "PDF_ENGINE_NODE_ERROR",
                                    "title": "outputFile error",
                                    "details": "An error occurred writing file " + zipEntry.entryName,
                                    "cause": err.toString()
                                }
                            });
                            console.error(err);
                        })
                );
            }
        }
        await Promise.all(writes);
        endZip();

        const endBrowser = perf.start('browser_session');
        const browser = await getBrowserSession();
        endBrowser();
        const endNewPage = perf.start('new_page');
        page = await browser.newPage();
        endNewPage();

        let data = req.body.data;
        let title = req.body.title;
        let renderMode = req.body.renderMode || 'handlebar';

        if (title == undefined) {
            title = "Documento PDF PagoPA";
        }

        if (data == undefined) {
            trackCustomEvent({
                name: "PDF_ENGINE_NODE",
                properties: {
                    "type": "PDF_ENGINE_NODE_ERROR",
                    "title": "Invalid request",
                    "details": "PDFE_898 - Missing data parameter",
                    "cause": "data parameter is undefined"
                }
            });
            res.status(400);
            res.json(buildResponseBody(400, 'PDFE_898', "Invalid request"));

            return;
        }

        try {

            const jsonData = JSON.parse(data);
            const endRead = perf.start('read_template');
            let templateFile = readFileSync(path.join(workingDir, "template.html")).toString();
            endRead();
            const endCompile = perf.start('handlebars_compile');
            let template = getCompiledTemplate(templateFile);
            endCompile();
            jsonData.tempPath = workingDir;
            const endRender = perf.start('handlebars_render');
            let html = template(jsonData);
            endRender();
            const endWrite = perf.start('write_compiled_html');
            fs.writeFileSync(path.join(workingDir, "compiledTemplate.html"), html);
            endWrite();

        } catch (err) {
            trackCustomEvent({
                name: "PDF_ENGINE_NODE",
                properties: {
                    "type": "PDF_ENGINE_NODE_ERROR",
                    "title": "Error compiling the HTML template",
                    "details": "PDFE_901 - An error occurred compiling the HTML template",
                    "cause": err.toString()
                }
            });
            console.error(err)
            res.status(500);
            res.json(buildResponseBody(400, 'PDFE_901', "Error compiling the HTML template"));

            return;
        }

        try {
            const endGoto = perf.start('page_goto');
            await page.goto('file:' + path.join(workingDir, "compiledTemplate.html"), {
                waitUntil: ['load', 'domcontentloaded']
            });
            endGoto();
            // path, can be relative or absolute path
            //await page.addStyleTag({path: path.join(workingDir, "style.css")});
            const endWait = perf.start('wait_for_render');
            waitRenderIterations = await waitForRender(page);
            endWait();
            const endPdf = perf.start('page_pdf');
            await page.pdf({
                path: path.join(workingDir, "pagopa-receipt.pdf"),
                title: title,
                format: 'A4',
                landscape: false,
                printBackground: true,
            });
            endPdf();
        } catch (err) {
            trackCustomEvent({
                name: "PDF_ENGINE_NODE",
                properties: {
                    "type": "PDF_ENGINE_NODE_ERROR",
                    "title": "Error generating the PDF document",
                    "details": "PDFE_902 - An error occurred generating the PDF document",
                    "cause": err.toString()
                }
            });
            console.error(err);
            res.status(500);
            res.json(buildResponseBody(500, 'PDFE_902', "Error generating the PDF document"));

            return;
        }

        trackCustomEvent({
            name: "PDF_ENGINE_NODE",
            properties: {
                "type": "PDF_ENGINE_NODE_LOG",
                "title": "PDF generation process completed",
                "details": "PDF " + title + " generated successfully"
            }
        });
        let content = readFileSync(path.join(workingDir, "pagopa-receipt.pdf"));
        pdfBytes = content.length;
        res.setHeader('content-type', 'application/pdf');
        const endSend = perf.start('res_send');
        res.send(content);
        endSend();

    } catch (err) {
        trackCustomEvent({
            name: "PDF_ENGINE_NODE",
            properties: {
                "type": "PDF_ENGINE_NODE_ERROR",
                "title": "Error generating the PDF document",
                "details": "PDFE_902 - An error occurred generating the PDF document",
                "cause": err.toString()
            }
        });
        console.error(err);
        res.status(500);
        res.json(buildResponseBody(500, 'PDFE_902', "Error generating the PDF document"));
    } finally {
        if (page) {
            const endClose = perf.start('page_close');
            await page.close();
            endClose();
        }

        if (workingDir) {
            const endRm = perf.start('rm_workdir');
            rmSync(workingDir, {recursive: true, force: true});
            endRm();
        }

        console.timeEnd(timestampLog);
        perf.flush({
            zipEntries: zipEntryCount,
            waitRenderIterations,
            pdfBytes
        });
    }

}

const waitForRender = async (page, timeout = 30000) => {
    const checkInterval = Number(process.env.CHECK_SIZE_INTERVAL) || 100;
    const maxChecks = timeout / checkInterval;
    let lastSize = 0;
    let checkCounts = 1;
    let countStableSizeIterations = 0;
    const minStableSizeIterations = Number(process.env.MIN_STABLE_SIZE_ITERATIONS) || 3;

    // Wait for fonts to be ready up-front
    const fontsStart = PERF_LOG ? nowNs() : null;
    try {
        await page.evaluate(() => document.fonts?.ready);
    } catch (_) { /* ignore: page may have already been torn down */ }
    if (PERF_LOG) {
        emitPerfEvent("PDF_ENGINE_NODE_PERF_PHASE", {
            phase: "fonts_ready"
        }, {
            duration_ms: nsToMs(nowNs() - fontsStart)
        });
    }

    let iterations = 0;
    while (checkCounts++ <= maxChecks) {
        iterations++;
        const iterStart = PERF_LOG ? nowNs() : null;
        const currentSize = await page.evaluate(() => {
            const body = document.body;
            if (!body) return 0;
            return body.innerHTML.length * 31 + body.scrollHeight;
        });
        if (PERF_LOG && iterations <= 3) {
            // log only the first few to avoid noise
            emitPerfEvent("PDF_ENGINE_NODE_PERF_PHASE", {
                phase: `wait_iter_${iterations}`
            }, {
                duration_ms: nsToMs(nowNs() - iterStart),
                size: currentSize
            });
        }

        if (lastSize !== 0 && currentSize === lastSize)
            countStableSizeIterations++;
        else
            countStableSizeIterations = 0;

        if (countStableSizeIterations >= minStableSizeIterations) {
            break;
        }

        lastSize = currentSize;
        await new Promise(r => setTimeout(r, checkInterval));
    }
    return iterations;
};

module.exports = {info, generatePdf, shutdown};