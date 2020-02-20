package mbt.counter;

import net.jqwik.api.stateful.*;

import static org.assertj.core.api.Assertions.*;

class CountUpAtMaxAction implements Action<Counter> {

	@Override
	public boolean precondition(Counter counter) {
		return counter.value() == 100;
	}

	@Override
	public Counter run(Counter counter) {
		counter.countUp();
		assertThat(counter.value()).isEqualTo(100);
		return counter;
	}

	@Override
	public String toString() {
		return "count up at max";
	}
}
