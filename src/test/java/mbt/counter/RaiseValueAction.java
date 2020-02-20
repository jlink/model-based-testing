package mbt.counter;

import net.jqwik.api.stateful.*;

import static org.assertj.core.api.Assertions.*;

class RaiseValueAction implements Action<Counter> {

	private int raiseBy;

	public RaiseValueAction(int raiseBy) {
		this.raiseBy = raiseBy;
	}

	@Override
	public boolean precondition(Counter counter) {
		return counter.value() + raiseBy < 100;
	}

	@Override
	public Counter run(Counter counter) {
		int previousValue = counter.value();
		for (int i = 0; i < raiseBy; i++) {
			counter.countUp();
		}
		assertThat(counter.value()).isEqualTo(previousValue + raiseBy);
		return counter;
	}

	@Override
	public String toString() {
		return "raise by " + raiseBy;
	}
}
