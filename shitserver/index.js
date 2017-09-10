process.stdin.resume();
process.stdin.setEncoding('utf8');
process.stdin.on('data', function(data) {
//    console.log("Received a chunk of charlength " + data.length+ ".  " + data.substring(0, 50) + " ... ");
    console.log(data.substring(43));
    console.log("###########################################");
});

