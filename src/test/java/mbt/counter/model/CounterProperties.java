package mbt.counter.model;

import mbt.counter.*;
import net.jqwik.api.*;
import net.jqwik.api.Tuple.*;
import net.jqwik.api.stateful.*;
import net.jqwik.api.statistics.Statistics;

class CounterProperties {

	@Property
	void checkCounter(@ForAll("counterActions") ActionSequence<Tuple2<Counter, CounterModel>> actions) {
		actions.run(Tuple.of(new Counter(), new CounterModel()));
	}

	@Provide
	Arbitrary<ActionSequence<Tuple2<Counter, CounterModel>>> counterActions() {
		Arbitrary<Action<Tuple2<Counter, CounterModel>>> standardActions = Arbitraries.of(
				new CountUpAction(),
				new CountDownAction()
		);
		Arbitrary<Action<Tuple2<Counter, CounterModel>>> raiseAction =
				Arbitraries
						.integers()
						.between(1, 99)
						.shrinkTowards(99)
						.map(RaiseValueAction::new);

		return Arbitraries.sequences(
				Arbitraries.frequencyOf(
						Tuple.of(5, standardActions),
						Tuple.of(1, raiseAction)
				)
		);
	}

}
