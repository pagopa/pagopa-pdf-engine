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
