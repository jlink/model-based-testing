package mbt.counter;

import net.jqwik.api.stateful.*;

import static org.assertj.core.api.Assertions.*;

class CountDownAction implements Action<Counter> {

	@Override
	public boolean precondition(Counter counter) {
		return counter.value() > 0;
	}

	@Override
	public Counter run(Counter counter) {
		int previousValue = counter.value();
		counter.countDown();
		assertThat(counter.value()).isEqualTo(previousValue - 1);
		return counter;
	}

	@Override
	public String toString() {
		return "count down";
	}

}
