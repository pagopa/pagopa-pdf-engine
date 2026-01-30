/* Inputs:

matrix: a n-dimensional array
row: optional parameter. the index of the row
column: optional parameter the index of the column

Output:
3 options because row and column are optionals:
- matrix
- matrix[row]
- matrix[row][column]



Example:
to retrieve the first row (index 0) second column (index 1)
    -> navigateMatrix matrix 0 1

to retrieve the third row (index 2)
    -> navigateMatrix matrix 2

*/

function navigateMatrix(matrix, row, column) {
  let ret = matrix;
  if (row !== null && row !== undefined && !isNaN(row)) {
    ret = ret[row];
  }
  if (column !== null && column !== undefined && !isNaN(column)) {
    ret = ret[column];
  }
  return ret;
}

module.exports = navigateMatrix;
