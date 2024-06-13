function getOnlinePaymentMethodContent(website, app) {
  if (website && app) {
    return "sul sito o app dell'ente, ";
  } else if (website && !app) {
    return "sul sito dell'ente, ";
  } else if (!website && app) {
    return "sull'app dell'ente, ";
  } else {
    return null;
  }
}

module.exports = getOnlinePaymentMethodContent;
