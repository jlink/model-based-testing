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
  
Looking at this from as a property-based tester suggest the following approach as viable:

- Generate a random sequence of actions. 
  Use an action's precondition as a generation constraint.
- Apply this sequence to a state machine starting in one of its initial states.
- For any (allowed) action, check that the resulting state is correct 
  and that any other invariants and postconditions hold.

Let's see how this idea can be translated into properties with 
[_jqwik_](https://jqwik.net).

### Testing a Counter 

_TBD_

## Model-based Testing

_TBD_

## Example: TeCoC - The Contrived CRM

_TBD_