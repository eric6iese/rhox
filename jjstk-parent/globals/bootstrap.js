/**
 * Global (non-module) definitions for jjs-scripting.
 * This script must be called once by all rhox-scripts in order to get all
 * global types and functions like console or require().
 * @author Eric Giese
 */
load(__DIR__ + 'jvm-npm.js');