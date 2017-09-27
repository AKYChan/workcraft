// File operations

function load(path) {
	return framework.loadWork(path);
}

function import(path) {
	return framework.loadWork(path);
}

function save(work, path) {
	framework.saveWork(work, path);
}

function export(work, path, format) {
	framework.exportWork(work, path, format);
}


// Command execution

function execResource(name) {
	framework.execJavaScriptResource(name);
}

function execFile(path) {
	framework.execJavaScriptFile(path);
}

function runCommand(work, commandName) {
	framework.runCommand(work, commandName);
}

function executeCommand(work, commandName) {
	return framework.executeCommand(work, commandName);
}


// Configuration

function setConfigVar(key, val) {
	framework.setConfigVar(key, val);
}

function getConfigVar(key) {
	return framework.getConfigVar(key);
}

function saveConfig() {
	framework.saveConfig();
}

function loadConfig() {
	framework.loadConfig();
}


// Text output

function print(msg) {
	java.lang.System.out.println(msg);
}

function eprint(msg) {
	java.lang.System.err.println(msg);
}

function write(text) {
	java.lang.System.out.print(text);
}

function write(text, fileName) {
	dir = framework.getWorkingDirectory();
	file = new java.io.File(dir, fileName);
	fileWriter = new java.io.FileWriter(file);
	bufferedWriter = new java.io.BufferedWriter(fileWriter);
	bufferedWriter.write(text);
	bufferedWriter.close();
}


// GUI and exit

function startGUI() {
	framework.startGUI();
}

function stopGUI() {
	framework.shutdownGUI();
}

function quit() {
	framework.shutdown();
}

function exit() {
	framework.shutdown();
}
