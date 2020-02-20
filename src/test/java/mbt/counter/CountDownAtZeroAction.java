package mbt.counter;

import net.jqwik.api.stateful.*;

import static org.assertj.core.api.Assertions.*;

class CountDownAtZeroAction implements Action<Counter> {

	@Override
	public boolean precondition(Counter counter) {
		return counter.value() == 0;
	}

	@Override
	public Counter run(Counter counter) {
		counter.countDown();
		assertThat(counter.value()).isEqualTo(0);
		return counter;
	}

	@Override
	public String toString() {
		return "count down at zero";
	}

}
