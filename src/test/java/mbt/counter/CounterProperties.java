package mbt.counter;

import net.jqwik.api.*;
import org.assertj.core.api.*;

class CounterProperties {

	@Property
	void fail() {
		Assertions.fail("initial failing example");
	}
}
