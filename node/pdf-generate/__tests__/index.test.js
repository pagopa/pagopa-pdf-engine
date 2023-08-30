const generatePdf = require("../index");

afterEach(() => {
  jest.clearAllMocks();
});

jest.mock('@anzp/azure-function-multipart');

describe("generatePdf", () => {
  it("should return a pdf when passing valid data", async () => {

    var context = {};
    var req = {}

    require('@anzp/azure-function-multipart').setValidData();

    await generatePdf(context, req);

    expect(context.res).toBeDefined();
    expect(context.res.body).toBeDefined();
    expect(context.res.body).toBeInstanceOf(Buffer);


  });
  it("should return an error when passing invalid data", async () => {

    var context = {};
    var req = {}

    require('@anzp/azure-function-multipart').setInvalidData();

    await generatePdf(context, req);

    expect(context.res).toBeDefined();
    expect(context.res.body).toBeDefined();
    expect(context.res.statusCode).toBeDefined();

  });
});