package mbt.counter.model;

import mbt.counter.*;
import net.jqwik.api.Tuple.*;
import net.jqwik.api.stateful.*;

import static org.assertj.core.api.Assertions.*;

class CountUpAction implements Action<Tuple2<Counter, CounterModel>> {
	@Override
	public Tuple2<Counter, CounterModel> run(Tuple2<Counter, CounterModel> tuple) {
		Counter counter = tuple.get1();
		CounterModel counterModel = tuple.get2();
		counter.countUp();
		counterModel.up();
		assertThat(counter.value()).isEqualTo(counterModel.getValue());
		return tuple;
	}


	@Override
	public String toString() {
		return "count up";
	}
}
