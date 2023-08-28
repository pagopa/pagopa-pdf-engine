import logging

import azure.functions as func
import os
from pathlib import Path
from playwright.sync_api import sync_playwright
# from ironpdf import *


browser = None

def get_browser():
    global browser
    if browser:
        return browser

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        return browser


def main(req: func.HttpRequest) -> func.HttpResponse:
    transactionID = f"F57E2F8E-25FF-4183-AB7B-4A5EC1A96644-{os.times()[4] * 1e9}"

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)

        browser_context = browser.new_context(accept_downloads=True)
        page = browser_context.new_page()

        html_file = Path('pdf-generate/index.html').resolve()

        page.goto('file:' + str(html_file), wait_until='networkidle')
        page.pdf(
            path=f"tests/pagopa-receipt-{transactionID}.pdf",
            format='A4'
        )

        page.close()


#
#     renderer = ChromePdfRenderer()
#     pdf = renderer.RenderUrlAsPdf('file:' + str(html_file))
#     pdf.SaveAs(f"tests/pagopa-receipt-{transactionID}.pdf")

    return func.HttpResponse(
         "This HTTP triggered function executed successfully. Pass a name in the query string or in the request body for a personalized response.",
         status_code=200
    )
