package mbt.counter;

public class Counter {

	private int value = 0;

	public void inc() {
		value++;
	}

	public void set(int value) {
		this.value = value;
	}

	public int value() {
		return value;
	}

	public void clear() {
		value = 0;
	}
}
