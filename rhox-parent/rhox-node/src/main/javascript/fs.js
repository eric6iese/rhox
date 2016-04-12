
var Paths = Java.type('java.nio.file.Paths');
var Files = Java.type('java.nio.file.Files');

function path(filename){
    return Paths.get(filename);
}

// Basic synchronous versions of the node functions
function unlink(file){
  Files.delete(path(file));  
}

function unlink(file){
  Files.delete(path(file));  
}



exports.unlinkSync = unlink;