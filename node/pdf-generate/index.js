const multer = require('multer');
const express = require('express');
const { info, generatePdf } = require('./handlers')


var app = express();

const storage = multer.memoryStorage()
const upload = multer({ storage: storage })

app.use(function(err, req, res, next) {
  res.status(err.status || 500);
  res.json({'errors': {
    message: err.message,
    error: {}
  }});
});

app.get('/info', info);

app.post('/pdf-generate', upload.none(), generatePdf);

var server = app.listen( process.env.PORT || 3000, function(){
  console.log('Listening on port ' + server.address().port);
});

module.exports = server;