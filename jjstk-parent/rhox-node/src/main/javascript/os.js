var System = Java.type('java.lang.System');

function sys(name){
    return function(){
        return System.getProperty(name);    
    }
}

exports.EOL = sys('line.separator');
exports.arch = sys('os.arch');
exports.type = sys('os.name');
exports.platform = sys('os.platform');
exports.homedir = sys('user.home');
exports.tmpdir = sys('io.tmpdir');