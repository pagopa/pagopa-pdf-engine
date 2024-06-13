function getRowIndex(index, pageNumber, paginator) {
  let n = 0;
  for (let page = 0; page <= pageNumber; page++) {
    let i = page === pageNumber ? index : paginator[page].length;
    for (let row = 0; row < i; row++) {
      n += paginator[page][row].length;
    }
  }

  return n;
}

module.exports = getRowIndex;
