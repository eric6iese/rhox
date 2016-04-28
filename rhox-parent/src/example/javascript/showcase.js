// Require-Bootstrapping
load(java.lang.System.getenv('NASHORN_GLOBALS'));

// Using rhox-maven
var maven = require('rhox-maven');
maven.include('org.apache.commons:commons-lang3:3.4');
maven.include('org.slf4j:slf4j-simple:1.7.21');

var SystemUtils = org.apache.commons.lang3.SystemUtils;
var log = org.slf4j.LoggerFactory.getLogger('');
log.info('You are running on {} with java {}!', SystemUtils.OS_NAME, SystemUtils.JAVA_VERSION);


// Using rhox-native
log.info('The following requires windows and msoffice');
var native = require('rhox-native');
var word = new native.ComObject('Word.Application');
word.Visible=true;
var Document = word.Documents.Add();
var range = Document.Range(0, 0);
range.Text='Hello from Javascript!';
word.Quit();
