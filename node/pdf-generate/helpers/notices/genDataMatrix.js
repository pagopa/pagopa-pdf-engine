const bwipjs = require('bwip-js');
const fs = require('fs');

function genDataMatrix(data, path) {
    const trimmedData = data.replaceAll(' ', '');

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

    const filePath = path + "/" + trimmedData+".svg";
    fs.writeFileSync(filePath, svg);

    return filePath;

}

module.exports = genDataMatrix;