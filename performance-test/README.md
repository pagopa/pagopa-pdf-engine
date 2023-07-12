# K6 tests

This is a set of [k6](https://k6.io) tests.

To invoke k6 tests use `run_performance_test.sh` script.


## How to run 🚀

Use this command to launch the tests:

``` shell
sh run_performance_test.sh <local|dev|uat> <load|stress|spike|...> <script-filename> <DB-name> <subkey> <generate-zipped> <template-file-name>
```
> **Note** \
> <generate-zipped> is a boolean field. It is optional (true/false, default is false) \
> <template-file-name> is the name of the zip file that contains the PDF template. It is optional (you must specify only the file name without the extension)