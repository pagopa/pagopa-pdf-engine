const fs = require('fs');
const path = require('path');
const Handlebars = require('handlebars');
const paginate = require('../helpers/receipts/paginate');
const genBarcode = require('../helpers/receipts/genBarcode');

describe('receipt pagination', () => {
    test.each([
        [1, [1]],
        [2, [2]],
        [3, [2, 1]],
        [4, [2, 2]],
        [5, [2, 2, 1]],
        [6, [2, 3, 1]],
        [7, [2, 3, 2]],
        [12, [2, 3, 3, 3, 1]],
    ])('%i items are preserved in pages of sizes %j', (count, sizes) => {
        const items = Array.from({ length: count }, (_, id) => ({ id }));
        const pages = paginate(items, { hash: { first: 2, next: 3, last: 2 } });

        expect(pages.map(page => page.length)).toEqual(sizes);
        expect(pages.flat()).toEqual(items);
        expect(pages[pages.length - 1].length).toBeGreaterThan(0);
    });

    test.each([0, -1, 1.5, undefined])('rejects invalid capacity %s', capacity => {
        expect(() => paginate([], {
            hash: { first: capacity, next: 3, last: 2 },
        })).toThrow('must be positive integers');
    });
});

describe('receipt partials', () => {
    const handlebars = Handlebars.create();
    const helpersPath = path.join(__dirname, '../helpers/receipts');
    for (const file of fs.readdirSync(helpersPath)) {
        if (file.endsWith('.js')) {
            handlebars.registerHelper(path.parse(file).name, require(path.join(helpersPath, file)));
        }
    }

    const partial = name => handlebars.compile(fs.readFileSync(
        path.join(__dirname, '../partials/receipts', `${name}.hbs`), 'utf8'
    ));

    test('renders two barcodes and preserves leading zeros in the creditor code', () => {
        const html = partial('singleCartItem')({
            payee: { name: 'Test creditor', taxCode: '00123456789' },
            refNumber: { type: 'codiceAvviso', value: '301234567890123416' },
            amount: '1,00',
        });

        expect(html.match(/src="data:image\/svg\+xml;base64,/g)).toHaveLength(2);
        expect(html).toContain('00123456789');
        expect(html).toContain(Handlebars.escapeExpression(genBarcode('00123456789')));
        expect(genBarcode('00123456789')).not.toEqual(genBarcode('123456789'));
    });

    test('hides repeated headers and renders only the real header as h1', () => {
        expect(partial('header')({})).toContain('<h1');
        const repeated = partial('header')({ isRepeated: true });
        expect(repeated).toContain('aria-hidden="true"');
        expect(repeated).not.toContain('<h1');
    });

    test('shows page indicators only for multipage receipts', () => {
        expect(partial('footer')({ pageCount: 1 })).not.toContain('class="page-indicator"');
        expect(partial('footer')({ pageCount: 3 }))
            .toContain('class="page-indicator" aria-hidden="true">3</p>');
    });
});
