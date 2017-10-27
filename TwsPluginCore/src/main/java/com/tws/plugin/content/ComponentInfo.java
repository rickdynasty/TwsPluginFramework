package com.tws.plugin.content;

public class ComponentInfo {
	public ComponentInfo() {
	}

	public ComponentInfo(String name) {
		this.name = name;
	}

	public ComponentInfo(String name, int type, String packageName, String processName) {
		this.type = type;
		this.name = name;
		this.packageName = packageName;
		this.processName = processName;
	}

	public int type;
	public String name;
	public String packageName;
	public String processName;
}