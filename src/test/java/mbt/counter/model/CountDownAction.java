package mbt.counter.model;

import mbt.counter.*;
import net.jqwik.api.*;
import net.jqwik.api.Tuple.*;
import net.jqwik.api.stateful.*;

import static org.assertj.core.api.Assertions.*;

class CountDownAction implements Action<Tuple2<Counter, CounterModel>> {

	@Override
	public Tuple2<Counter, CounterModel> run(Tuple2<Counter, CounterModel> tuple) {
		Counter counter = tuple.get1();
		CounterModel counterModel = tuple.get2();
		counter.countDown();
		counterModel.down();
		assertThat(counter.value()).isEqualTo(counterModel.getValue());
		return tuple;
	}

	@Override
	public String toString() {
		return "count down";
	}
}
