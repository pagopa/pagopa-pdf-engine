function getOfflinePaymentMethodContent(location) {
  if (location) {
    return `${location}, `;
  } else {
    return null;
  }
}

module.exports = getOfflinePaymentMethodContent;
