package mbt.counter;

import net.jqwik.api.stateful.*;

import static org.assertj.core.api.Assertions.*;

class CountUpAction implements Action<Counter> {

	@Override
	public boolean precondition(Counter state) {
		return state.value() < 100;
	}

	@Override
	public Counter run(Counter counter) {
		int previousValue = counter.value();
		counter.countUp();
		assertThat(counter.value()).isEqualTo(previousValue + 1);
		return counter;
	}

	@Override
	public String toString() {
		return "count up";
	}
}
