const i18next = require("i18next");

function languageHandler(i18n_key, options) {
  /* Get language parameters from JSON file */
  const languageInfo = options.data.root.metadata;
  const mainLanguage = languageInfo?.language;
  const secondaryLanguage = languageInfo?.secondaryLanguage;
  const isTrueBilingual = languageInfo?.trueBilingualism;

  /* Get parameters from the helper */
  const {
    classPrimaryLang,
    classSecondaryLang,
    classBilingual,
    classContainer,
    forceSecondaryLang,
    align: textBoxAlignment = "horizontal",
    ...variables
  } = options.hash;

  /* Get localized text using i18n key */
  const mainLocalizedText = i18next.t(i18n_key, {
    lng: mainLanguage ?? "it",
    interpolation: { escapeValue: false },
    ...variables,
  });

  /* If there are no hash parameters, only the main localised text
  will be returned. This could be used to render
  the document title, for example */
  if (!options.hash) {
    return mainLocalizedText;
  }

  /* Create the mainHTML element with default language text */
  const mainHTMLElem = classPrimaryLang
    ? `<p class="${classPrimaryLang}">${mainLocalizedText}</p>`
    : `<p>${mainLocalizedText}</p>`;

  /* Create the secondaryHTML element, using the secondary language */
  if (mainLanguage && secondaryLanguage) {
    const secondaryLocalizedText = i18next.t(i18n_key, {
      lng: languageInfo.secondaryLanguage,
      interpolation: { escapeValue: false },
      ...variables,
    });

    if (forceSecondaryLang) {
      return classPrimaryLang
        ? `<p class="${classPrimaryLang}">${secondaryLocalizedText}</p>`
        : `<p>${secondaryLocalizedText}</p>`;
    }

    const trueBilingualClassname = classBilingual ?? classPrimaryLang; // Use `classPrimaryLang` as default fallback
    const secondaryElementClassname = isTrueBilingual ? trueBilingualClassname : classSecondaryLang;
    const secondaryHTMLElem = textBoxAlignment === "horizontal"
      ? `<p class="${secondaryElementClassname}">· ${secondaryLocalizedText}</p>` // Add a dot separator between the languages
      : `<p class="${secondaryElementClassname}">${secondaryLocalizedText}</p>`;

    const flexDirection = textBoxAlignment === "horizontal" ? "Horizontal" : "Vertical";
    return classContainer
      ? `<div class="${classContainer} bilingualTextBox${flexDirection}">${mainHTMLElem}${secondaryHTMLElem}</div>`
      : `<div class="bilingualTextBox${flexDirection}">${mainHTMLElem}${secondaryHTMLElem}</div>`;
  } else {
    return classContainer ? `<div class="${classContainer}">${mainHTMLElem}</div>` : mainHTMLElem;
  }
}

module.exports = languageHandler;
