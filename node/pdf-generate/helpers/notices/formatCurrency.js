function formatCurrency(amountInCents, options) {
    if (!Number.isInteger(amountInCents)) {
        throw new Error(`formatCurrency: expected integer eurocents, received ${JSON.stringify(amountInCents)}`);
    }
    const amount = amountInCents / 100;
    const metadata = options?.data?.root?.metadata;
    const isTrueBilingual = metadata?.trueBilingualism;
    const language = metadata?.language ?? "it";

    const locale = isTrueBilingual ? "it" : language;

    const { numberOnly } = options.hash ?? {};

    if (numberOnly) {
        return new Intl.NumberFormat(locale, {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
        }).format(amount);
    }

    // U+202F (narrow no-break space) is defined in Titillium Sans Pro with only 45/1000 units
    // advance width (~1-2px at h1 size), making it effectively invisible. Normalize to U+0020.
    const result = new Intl.NumberFormat(locale, {
        style: "currency",
        currency: "EUR",
    }).format(amount);
    return result.replace(/ /g, " ");
}

module.exports = formatCurrency;
