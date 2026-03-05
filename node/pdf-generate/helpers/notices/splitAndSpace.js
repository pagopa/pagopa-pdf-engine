function splitAndSpace(str, options) {
  return str.match(/(.{2,4})/gy).join(" ");
}

module.exports = splitAndSpace;
