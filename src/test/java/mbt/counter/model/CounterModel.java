package mbt.counter.model;

public class CounterModel {

	private int value = 0;

	void up() {
		if (value == 100) {
			return;
		}
		value++;
	}

	void down() {
		if (value == 0) {
			return;
		}
		value--;
	}

	int getValue() {
		return value;
	}

}
