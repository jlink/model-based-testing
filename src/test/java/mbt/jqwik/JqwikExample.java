package mbt.jqwik;

import net.jqwik.api.*;

class JqwikExample {
	@Property
	void stringsAndEvenInts(@ForAll String anyString, @ForAll("evenNumbers") int anEvenInt) {
		System.out.println("anyString: " + anyString);
		System.out.println("anEvenInt: " + anEvenInt);
	}

	@Provide
	Arbitrary<Integer> evenNumbers() {
		return Arbitraries.integers().filter(i -> i % 2 == 0);
	}
}
