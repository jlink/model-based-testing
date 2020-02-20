# Model-based Testing

When you're doing 
[property-based testing](https://hypothesis.works/articles/what-is-property-based-testing/) (PBT) 
the biggest challenge is to find 
good properties, which is arguably much harder than coming up with examples.
Given PBT's 20-year history quite a few patterns and strategies have been discovered
that can guide you towards useful and effective properties. A detailed view into
[John Hughes's](https://twitter.com/rjmh) good practices is his 2019 paper 
[_How to Specify it!_](https://www.dropbox.com/s/tx2b84kae4bw1p4/paper.pdf).
Or, if you prefer Java code over Haskell code, read my 
[line by line translation](https://johanneslink.net/how-to-specify-it/)
of his contents.

One of the approaches that he uses is [model-based properties](https://johanneslink.net/how-to-specify-it/#45-model-based-properties). This kind of properties is interesting
in cases when your subject under test is not a pure function, i.e. all inputs
determine unambiguously all output and effects, but some kind of object or process
in which you have to deal with stateful behaviour.

In this article I want to address how to test stateful objects and applications with
properties; and _model-based properties_ playing an important role there.
The final motivation for this write-down came from 
[Jacob Stanley's article](https://jacobstanley.io/how-to-use-hedgehog-to-test-a-real-world-large-scale-stateful-app/). Therein he describes 
"How to use Hedgehog to test a real world, large scale, stateful app".

### How to do PBT in Java

I've already written a 
[whole blog series](https://blog.johanneslink.net/2018/03/24/property-based-testing-in-java-introduction/)
on that topic, that's why I'm not covering the basics here. 
Reading [this article in Oracle's Java Magazine](https://blogs.oracle.com/javamagazine/know-for-sure-with-property-based-testing)
will also give you an overview. 

## Stateful Testing

So what's the problem with properties and state? PBT started its life in Haskell,
programming language that has _pure functions_ built into its heart. It's therefore
not a coincidence that most of the examples that the PBT newbie sees in talks and
tutorials is about applying PBT to functions. When you search the web for 
"patterns and property-based testing" you often find articles 
[like this](https://blog.ssanj.net/posts/2016-06-26-property-based-testing-patterns.html).
None of the patterns describes there lends itself well to stateful systems.

Digging a bit further, though, the idea of using properties to explore the realm
of [finite state machines](https://blog.johanneslink.net/2018/09/06/stateful-testing/#finite-state-machines) shows up. In the end, most - if not all - stateful objects can be considered to be
state machines with the following characteristics:

- There is a finite list of possible states and the object is in always in a single, defined state.
- For every state there is a finite and possible empty list of transitions that bring the state machine into its next state.
- Transitions are often triggered by actions (aka events).
- Actions can have pre-conditions (Is it currently applicable?) 
  and postconditions (What must be true after an action finishes successfully?).
  
With the perspective of a property-based tester the following approach looks viable:

- Generate a random sequence of actions. 
  - Use an action's precondition as a generation constraint.
  - Specify the action's postconditions.
- Apply this sequence to a state machine starting in one of its initial states.
- For any (allowed) action, run it and check that its postconditions 
  and all of the object's invariants hold.

Let's see how this idea can be used in actual code.

### Testing a Counter 

A rather simple stateful object is a counter with the capabilities 
to count up and to count:

```java
public class Counter {
	public void countUp() {...}
	public void countDown() {...}
	public int value() {...}
}
```

At first glance you might think its finite set of state has only one element, 
e.g. called `counting`. When exploring the counting domain with your experts, though,
you learn that a counter must never go below zero and also has a maximum
value of `100`. So you come up with the following state transition table:

|Current State |Action    |Next State|
|--------------|----------|----------|
|at zero       |count up  |counting  |
|at zero       |count down|at zero   |
|counting (<99)|count up  |counting  |
|counting (=99)|count up  |at max    |
|counting (=1) |count down|at zero   |
|counting (>1) |count down|counting  |
|at max        |count up  |at max    |
|at max        |count down|counting  |

Despite there being just two state changing actions, there are quite
a few different states and transitions to consider. Using good old example tests
would require at least 8 scenarios to make sure that all the corner cases
are being covered. Instead we want to build on 
[jqwik's support for stateful testing](https://jqwik.net/docs/current/user-guide.html#stateful-testing).

#### Specifying the Actions

The first step is usually to specify the actions. 
We choose counting up as the first to tackle:

```java
import net.jqwik.api.stateful.*;
import static org.assertj.core.api.Assertions.*;

class CountUpAction implements Action<Counter> {
	@Override
	public Counter run(Counter counter) {
		int previousValue = counter.value();
		counter.countUp();
		assertThat(counter.value()).isEqualTo(previousValue + 1);
		return counter;
	}
}
``` 

_jqwik's_ `Action` abstraction combines the execution of an action and checking
postcondition into a single `run(..)` method. In this case we are using
AssertJ to check that the counter's value increases. But actually that's not
always the case as our state transition table shows. We now have the choice
to either create branches in our assertion logic:

```java
if (previousValue < 100) {
		assertThat(counter.value()).isEqualTo(previousValue + 1);
} else {
		assertThat(counter.value()).isEqualTo(previousValue);
}
```

or to split the action in two cases - and that's what I prefer:

```java
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

class CountUpAtMaxAction implements Action<Counter> {
	@Override
	public boolean precondition(Counter state) {
		return state.value() == 100;
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
```

Let's do the same thing for counting down:

```java
class CountDownAction implements Action<Counter> {
	@Override
	public boolean precondition(Counter state) {
		return state.value() > 0;
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

class CountDownAtZeroAction implements Action<Counter> {
	@Override
	public boolean precondition(Counter state) {
		return state.value() == 0;
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
```

#### Running the Property

We are left with combining the actions into an executable property:

```java
class CounterProperties {
	@Property
	void checkCounter(@ForAll("counterActions") ActionSequence<Counter> actions) {
		actions.run(new Counter());
	}
	@Provide
	Arbitrary<ActionSequence<Counter>> counterActions() {
		return Arbitraries.sequences(
				Arbitraries.of(
						new CountUpAction(),
						new CountUpAtMaxAction(),
						new CountDownAction(),
						new CountDownAtZeroAction()
				));
	}
}
```

Running this property will immediately reveal a bug in our implementation:

```
org.opentest4j.AssertionFailedError: 
    Property [CounterProperties:checkCounter] failed with sample [ActionSequence: [count down at zero]]
```

Here's the faulty implementation:

```java
public class Counter {
	private int value = 0;
	public void countUp() {
		value++;
	}
	public void countDown() {
		value--;
	}
	public int value() {
		return value;
	}
	@Override
	public String toString() {
		return String.format("Counter[%d]", value);
	}
}
```

And here's the simple fix:

```java
public class Counter {
    ...
	public void countDown() {
		if (value > 0) {
			value--;
		}
	}
}
```

#### Tuning the Action Generators

The optimistic tester might now expect that our property would now detect 
the other obvious thing we missed: Not checking the max value of `100`.
But actually it does not!

The problem we are running into is that revealing the bug would require an
action sequence of 100 more "count-ups" than "count-downs". Without heavy tweaking
of both sequence size and random choice of actions such a sequence will 
never be generated by jqwik. 

How would we learn that our property has
this blind spot? The statistics feature comes to our rescue.
It's good practice to check that generators will cover the cases the we already
know to be important.

```java
@Property
void checkCounter(@ForAll("counterActions") ActionSequence<Counter> actions) {
	actions.withInvariant(counter -> {
		String classifier = counter.value() == 0 ? "at zero"
				: counter.value() == 100 ? "at max" : "in between";
		Statistics.collect(classifier);
	}).run(new Counter());

	Statistics.coverage(checker -> {
		checker.check("at zero").count(c -> c > 1);
		checker.check("in between").count(c -> c > 1);
		checker.check("at max").count(c -> c > 1);
	});
}
```

To collect statistics for the state of our counter we had to use 
`withInvariant(..)` in an unusual way - but we are hackers, aren't we?
The property is now failing with

```
org.opentest4j.AssertionFailedError: Count of 0 does not fulfill condition
```

and the stack trace leads us to the `check("at max")` line so that we know
which case is not hit by the generator. What can we do?

One trick in our bag is introducing another action that will make the probability
of hitting the missing state much more likely. In this case we could, for example,
randomly raise the counter by a value between `0` and `99`:

```java
class RaiseValueAction implements Action<Counter> {
	private int raiseBy;

	public RaiseValueAction(int raiseBy) {
		this.raiseBy = raiseBy;
	}

	@Override
	public boolean precondition(Counter state) {
		return state.value() + raiseBy < 100;
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
```

We also have to modify our sequences generator to include this action.
I have chosen a ratio of 5 to 1 between the standard actions and the new
raising action:

```java
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
```

Now we are getting the statistics and failure message we want:

```
[CounterProperties:checkCounter] (1907) statistics = 
    in between (1795) : 94.13 %
    at max     ( 109) :  5.72 %
    at zero    (   3) :  0.16 %

CounterProperties:checkCounter = 
    org.opentest4j.AssertionFailedError: Run failed after following actions:
        raise by 98
        count up
        count up
        count up at max
      final currentModel: Counter[101]}

Expecting:
 <101>
to be equal to:
 <100>
but was not.
```

The fix is now trivial and the happy ending is filling our PBT soul with joy.

## Model-based Testing

One can argue that the `Counter` example is too simple in the sense that the postconditions,
which are essential for checking an implementation's validity, could be stated
without much knowledge abut previous actions and complicated internal state.
In cases where state-dependency is more intrigued we'll have to search for additional
tools to specify the expected outcome of an action. 
_Model-based testing_ is coming to our rescue...

_TBD_

## Example: TeCoC - The Contrived CRM

_TBD_