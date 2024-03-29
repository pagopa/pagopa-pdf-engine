const multer = require('multer');
const express = require('express');
const {getBrowserSession} = require('./utils/browserManager');
const { info, shutdown, generatePdf } = require('./handlers')


var app = express();

const storage = multer.memoryStorage()
const upload = multer({ storage: storage })

const browser = getBrowserSession();

var server = app.listen( process.env.PORT || 3000, function(){
  console.log('Listening on port ' + server.address().port);
});

app.use(function(err, req, res, next) {
  res.status(err.status || 500);
  res.json({'errors': {
    message: err.message,
    error: {}
  }});
});

app.get('/info', info);

app.get('/shutdown', function(req, res) { shutdown(req, res, server)});

app.post('/generate-pdf', upload.single('template'), generatePdf);

module.exports = server;