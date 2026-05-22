const appInsights = require("applicationinsights");

const connectionString = process.env.APPLICATIONINSIGHTS_CONNECTION_STRING;

if (connectionString) {
    try {
        appInsights.setup(connectionString).start();
    } catch (e) {
        console.error("Failed to start Application Insights", e);
    }
}

const telemetryClient = appInsights.defaultClient;

// ---------------------------------------------------------------------------
// trackCustomEvent
//
// Thin wrapper around App Insights' trackEvent that no-ops when telemetry is
// not configured (e.g. local development without a connection string) and
// swallows any error so that telemetry issues never break the request flow.
// ---------------------------------------------------------------------------
function trackCustomEvent(msg) {
    if (!telemetryClient) return;
    try {
        telemetryClient.trackEvent(msg);
    } catch (e) {
        console.error("custom event tracking failed", e);
    }
}

// ---------------------------------------------------------------------------
// Performance instrumentation.
//
// Disabled by default. Activate with PERF_LOG=1 (or =true / =yes).
// When disabled the helpers degrade to no-ops.
//
// All timings are in milliseconds with sub-ms precision (process.hrtime.bigint).
// Each phase is published twice on App Insights:
//   - one PDF_ENGINE_NODE_PERF_PHASE event per phase (for percentiles per phase)
//   - one PDF_ENGINE_NODE_PERF_TOTAL event per request, with the full breakdown
//     in `measurements` (so KQL queries can correlate phases on the same row).
// ---------------------------------------------------------------------------
const PERF_LOG = /^(1|true|yes)$/i.test(String(process.env.PERF_LOG || ""));

function nowNs() { return process.hrtime.bigint(); }
function nsToMs(ns) { return Number(ns) / 1e6; }

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
    // Mirror to console for local development / docker logs when telemetry
    // is not wired up.
    if (!telemetryClient) {
        console.log(`PERF | ${name} ${JSON.stringify({ ...properties, ...measurements })}`);
    }
}

// Emit a single perf phase outside of a tracker (e.g. ad-hoc spans inside
// helper functions). No-op when PERF_LOG is disabled.
function emitPerfPhase(phase, durationMs, extraProps = {}, extraMeasurements = {}) {
    if (!PERF_LOG) return;
    emitPerfEvent("PDF_ENGINE_NODE_PERF_PHASE", {
        phase,
        ...extraProps
    }, {
        duration_ms: durationMs,
        ...extraMeasurements
    });
}

// Build a per-request tracker. Returns no-op stubs when PERF_LOG is disabled
// so callers don't need to guard each call site.
function createPerfTracker(reqId) {
    if (!PERF_LOG) {
        return {
            reqId,
            measure: async (_name, fn) => fn(),
            start: () => () => {},
            flush: () => {}
        };
    }

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
            const s = nowNs();
            try {
                return await fn();
            } finally {
                recordPhase(name, nsToMs(nowNs() - s));
            }
        },
        // Manual span (when measure() doesn't fit, e.g. interleaved code).
        start(name) {
            const s = nowNs();
            return () => recordPhase(name, nsToMs(nowNs() - s));
        },
        flush(extra) {
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

module.exports = {
    telemetryClient,
    trackCustomEvent,
    createPerfTracker,
    emitPerfPhase,
    PERF_LOG,
    nowNs,
    nsToMs
};

