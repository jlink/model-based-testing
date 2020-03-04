package mbt.counter;

import net.jqwik.api.*;
import net.jqwik.api.stateful.*;
import net.jqwik.api.statistics.Statistics;

class CounterProperties {

	@Property
	void checkCounter(@ForAll("counterActions") ActionSequence<Counter> actions) {
		actions.peek(counter -> {
			String classifier =
					counter.value() == 0 ? "at zero"
							: counter.value() == 100 ? "at max"
									  : "in between";
			Statistics.collect(classifier);
		}).run(new Counter());

		Statistics.coverage(checker -> {
			checker.check("at zero").count(c -> c > 1);
			checker.check("in between").count(c -> c > 1);
			checker.check("at max").count(c -> c > 1);
		});
	}

	@Provide
	Arbitrary<ActionSequence<Counter>> counterActions() {
		Arbitrary<Action<Counter>> standardActions = Arbitraries.of(
				new CountUpAction(),
				new CountUpAtMaxAction(),
				new CountDownAction(),
				new CountDownAtZeroAction()
		);
		Arbitrary<Action<Counter>> raiseAction =
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
