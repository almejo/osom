package com.almejo.osom.cpu;

import lombok.Getter;

@Getter
class Clock {

	private int m;
	private int t;

	public void update(int m, int t) {
		this.m += m;
		this.t += t;
	}

	@Override
	public String toString() {
		return "[m: " + m + ", t: " + t + "]";
	}
}
