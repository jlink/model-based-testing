package mbt.counter.model;

import mbt.counter.*;
import net.jqwik.api.*;
import net.jqwik.api.stateful.*;

import static org.assertj.core.api.Assertions.*;

class RaiseValueAction implements Action<Tuple.Tuple2<Counter, CounterModel>> {

	private int raiseBy;

	public RaiseValueAction(int raiseBy) {
		this.raiseBy = raiseBy;
	}

	@Override
	public boolean precondition(Tuple.Tuple2<Counter, CounterModel> tuple) {
		return tuple.get2().getValue() + raiseBy < 100;
	}

	@Override
	public Tuple.Tuple2<Counter, CounterModel> run(Tuple.Tuple2<Counter, CounterModel> tuple) {
		Counter counter = tuple.get1();
		CounterModel counterModel = tuple.get2();
		for (int i = 0; i < raiseBy; i++) {
			counter.countUp();
			counterModel.up();
			assertThat(counter.value()).isEqualTo(counterModel.getValue());
		}
		return tuple;
	}

	@Override
	public String toString() {
		return "raise by " + raiseBy;
	}
}
