/**

Inputs:
- Notice:
    Object with instalments sub-array, from 2 to 9 items
    (+9 items are managed with another template)
- withBollettinoPoste
    Boolean value indicating the presence/absence of the "Bollettino Postale"

Outputs:
- Create an n-dimensional array to divide the instalments in pages and rows.

EXAMPLE:

instalments [1,2,3,4,5,6,7]
withBollettinoPoste: false

Output:
[
         page1        page2
row1  [ [1,2,3] ] , [ [4,5] ]
row2                  [6,7]

]

Tip:
Use `navigateMatrix` helper to navigate in this data structure:
- navigateMatrix <pageNumber> <rowNumber>

**/

function paginator(notice, withBollettinoPoste) {
  const instalments = notice.instalments.items;
  const instalmentsNumber = instalments.length;

  const data = withBollettinoPoste ? getPaginationDataPoste(instalmentsNumber) : getPaginationData(instalmentsNumber);

  return paginateAndGroupArray(
    instalments,
    data?.firstPageRows,
    data?.firstPageColumns,
    data?.secondPageRows,
    data?.secondPageColumns
  );
}

function paginateAndGroupArray(array, firstPageRows, firstPageCols, pageRows, pageCols) {
  /* Function to divide a page in rows and columns */
  function splitIntoRowsAndCols(page, numRows, numCols) {
    let rows = [];
    for (let i = 0; i < numRows; i++) {
      let start = i * numCols;
      let end = start + numCols;
      if (start < page.length) {
        rows.push(page.slice(start, end));
      }
    }
    return rows;
  }

  /* Final output:
  - Array of pages, every page is an array of rows
  */
  let paginatedArray = [];

  /* Add the first page with a specific number of rows and columns */
  const firstPageSize = firstPageRows * firstPageCols;
  const firstPage = array.slice(0, firstPageSize);
  paginatedArray.push(splitIntoRowsAndCols(firstPage, firstPageRows, firstPageCols));
  /* Calculate the number of remaining items */
  const remainingItems = array.length - firstPageSize;

  /* Divide the remaining items of the array in pages */
  const pageSize = pageRows * pageCols;
  const totalPages = Math.ceil(remainingItems / pageSize);

  for (let i = 0; i < totalPages; i++) {
    const start = firstPageSize + i * pageSize;
    const end = start + pageSize;
    const page = array.slice(start, end);
    paginatedArray.push(splitIntoRowsAndCols(page, pageRows, pageCols));
  }

  return paginatedArray;
}

function getPaginationDataPoste(instalmentsNumber) {
  let firstPageRows = 0;
  let firstPageColumns = 0;
  let secondPageRows = 0;
  let secondPageColumns = 0;

  switch (instalmentsNumber) {
    case 2:
      firstPageRows = 1;
      firstPageColumns = 2;
      break;
    case 3:
      firstPageRows = 1;
      firstPageColumns = 3;
      break;
    case 4:
      firstPageRows = 1;
      firstPageColumns = 2;
      secondPageRows = 1;
      secondPageColumns = 2;
      break;
    case 5:
      firstPageRows = 1;
      firstPageColumns = 2;
      secondPageRows = 1;
      secondPageColumns = 3;
      break;
    case 6:
      firstPageRows = 1;
      firstPageColumns = 3;
      secondPageRows = 1;
      secondPageColumns = 3;
      break;
    case 7:
      firstPageRows = 1;
      firstPageColumns = 3;
      secondPageRows = 1;
      secondPageColumns = 2;
      break;
    case 8:
      firstPageRows = 1;
      firstPageColumns = 2;
      secondPageRows = 1;
      secondPageColumns = 3;
      break;
    default:
      firstPageRows = 1;
      firstPageColumns = 3;
      secondPageRows = 1;
      secondPageColumns = 3;
  }
  return { firstPageRows, firstPageColumns, secondPageRows, secondPageColumns };
}

function getPaginationData(instalmentsNumber) {
  const firstPageRows = 1;
  let firstPageColumns = 2;
  let secondPageRows = 3;
  let secondPageColumns = 2;

  switch (instalmentsNumber) {
    case 2:
      firstPageColumns = 2;
      secondPageRows = 0;
      break;
    case 3:
      firstPageColumns = 3;
      secondPageRows = 0;
      break;
    case 4:
      firstPageColumns = 2;
      secondPageRows = 1;
      secondPageColumns = 2;
      break;
    case 5:
      firstPageColumns = 2;
      secondPageRows = 1;
      secondPageColumns = 3;
      break;
    case 6:
      firstPageColumns = 2;
      secondPageRows = 2;
      secondPageColumns = 2;
      break;
    case 7:
      firstPageColumns = 3;
      secondPageRows = 2;
      secondPageColumns = 2;
      break;
    case 8:
      firstPageColumns = 2;
      secondPageRows = 2;
      secondPageColumns = 3;
      break;
    default:
      firstPageColumns = 3;
      secondPageRows = 3;
      secondPageColumns = 3;
  }
  return { firstPageRows, firstPageColumns, secondPageRows, secondPageColumns };
}

module.exports = paginator;
