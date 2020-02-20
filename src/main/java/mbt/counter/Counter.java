package mbt.counter;

public class Counter {

	private int value = 0;

	public void countUp() {
		if (value < 100) {
			value++;
		}
	}

	public void countDown() {
		if (value > 0) {
			value--;
		}
	}

	public int value() {
		return value;
	}

	@Override
	public String toString() {
		return String.format("Counter[%d]", value);
	}
}
