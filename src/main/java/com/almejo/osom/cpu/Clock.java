package com.almejo.osom.cpu;

public class Clock {

	private int m;
	private int t;

	public void update(int m, int t) {
		this.m += m;
		this.t += t;
	}

	public int getT() {
		return t;
	}

	@Override
	public String toString() {
		return "[m: " + m + ", t: " + t + "]";
	}
}
