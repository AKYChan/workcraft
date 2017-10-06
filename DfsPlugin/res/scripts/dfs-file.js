framework.addJavaScriptHelp("exportDfsVerilog", "work, fileName",
    "export the specified DFS 'work' as a Verilog netlist .v 'fileName'");

function exportDfsVerilog(work, fileName) {
    framework.exportWork(work, fileName, 'VERILOG');
}
