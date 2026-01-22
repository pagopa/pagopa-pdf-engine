/**
 * Returns a CSS class name for dynamic payee name sizing based on string length.
 * @param {string} str - The payee name
 * @returns {string} CSS class: 'smallest' (≥65 chars), 'small' (≥40), or 'default'
 */
function getDynamicPayeeSizeClassName(str) {
  const length = str.length;
  const payeeNameBaseClass = "dynamic-payee-name";
  
  switch (true) {
    case length >= 65:
      return `${payeeNameBaseClass} smallest`;
    case length >= 40:
      return `${payeeNameBaseClass} small`;
    default:
      return `${payeeNameBaseClass} default`;
  } 
}

module.exports = getDynamicPayeeSizeClassName;
