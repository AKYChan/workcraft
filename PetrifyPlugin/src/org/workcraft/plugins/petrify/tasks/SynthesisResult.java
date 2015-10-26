package org.workcraft.plugins.petrify.tasks;

public class SynthesisResult {
	private String equations;
	private String verilog;
	private String log;
	private String stdout;
	private String stderr;

	public SynthesisResult(String equations, String verilog, String log, String stdout, String stderr) {
		this.equations = equations;
		this.verilog = verilog;
		this.log = log;
		this.stdout = stdout;
		this.stderr = stderr;
	}

	public String getEquation() {
		return this.equations;
	}

	public String getVerilog() {
		return this.verilog;
	}

	public String getLog() {
		return this.log;
	}

	public String getStdout() {
		return this.stdout;
	}

	public String getStderr() {
		return this.stderr;
	}
}
