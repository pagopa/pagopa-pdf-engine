const InfoHandler = require("../index");

afterEach(() => {
  jest.clearAllMocks();
});

describe("InfoHandler", () => {
  it("should return a success", async () => {

    var context = {};
    var req = {};

    await InfoHandler(context, req);

    expect(context.res).toBeDefined();
  });
});