//process.stdin.resume();
//process.stdin.setEncoding('utf8');
//process.stdin.on('data', function(data) {
//    console.log("Received a chunk of charlength " + data.length+ ".  " + data.substring(0, 50) + " ... ");
//    console.log(data.substring(43));
//    console.log("###########################################");
//});

var data = JSON.parse(process.argv[2].substring(43));
var request = require("request");

var options = { method: 'POST',
  url: 'https://hackpen-179409.appspot.com/api/uploadChunk',
  headers: 
   { 'postman-token': '93c38d06-59f7-610f-4e91-aa5f9a751b85',
     'cache-control': 'no-cache',
     'content-type': 'application/json' },
  body: data,
  json: true };

request(options, function (error, response, body) {
  if (error) throw new Error(error);

  console.log(body);
});

//console.log(JSON.stringify(data));
console.log("      ");
