var QRCode = require("qrcode-svg");
const { v4: uuidv4 } = require("uuid");
const path = require('path');

function genQrCode(data, saveDir) {
  const filename = path.join(saveDir,uuidv4()+".svg");
  var qrcode = new QRCode({
    content: data,
    padding: 0,
    width: 256,
    height: 256,
    color: "#000000",
    background: "#ffffff",
    ecl: "M",
    join: true,
  });
  qrcode.save(filename, function (error) {
    if (error) throw error;
  });
  return filename;
}

module.exports = genQrCode;
