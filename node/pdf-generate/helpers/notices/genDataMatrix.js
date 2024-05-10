const bitgener = require('bitgener');
const { v4: uuidv4 } = require("uuid");
const path = require('path');

function genDataMatrix(data, saveDir) {
    const filename = path.join(saveDir,uuidv4()+".svg");
    bitgener({
      data: data,
      type: 'datamatrix',
      output: filename,
      encoding: 'utf8',
      rectangular: false,
      padding: 0,
      width: 256,
      height: 256,
      original2DSize: false,
      color: 'black',
      opacity: 1,
      bgColor: '#F7931A',
      bgOpacity: 0,
      hri: {
        show: false
      }
    }).then((ret) => {
        console.log(ret);
    });

    return filename;
}

module.exports = genDataMatrix;