const bwipjs = require('bwip-js');
const { v4: uuidv4 } = require("uuid");
const path = require('path');
const fs = require('fs');

function genDataMatrix(data, saveDir) {
    const filename = path.join(
    saveDir,uuidv4()+".svg");

    let svg = bwipjs.toSVG({
        bcid: 'datamatrix',
        text: data,
        width: 256,
        height: 256,
        padding: 0,
        scale: 3,
        includetext: false,
        textxalign:  'center',
    });

    fs.writeFileSync(filename, svg);

    return filename;
}

module.exports = genDataMatrix;
