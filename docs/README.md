Property-based testing (PBT) is often presented in the context of pure functions and stateless systems.
Tackling stateful systems with PBT requires some additional thinking. 
This article starts with introducing properties for state-machine-like objects 
and afterwards dives into the intricacies of writing properties for a database-centric persistence implementation.
All that is shown using Java and [_jqwik_](https://jqwik.net); 
many ideas can be transferred to other languages and libraries, though. 

<!-- use `doctoc --maxlevel 4 README.md` to recreate the TOC -->
<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
## Table of Contents  

- [Introduction](#introduction)
  - [How to do PBT in Java](#how-to-do-pbt-in-java)
    - [How jqwik Works](#how-jqwik-works)
- [Stateful Testing](#stateful-testing)
  - [Testing a Counter](#testing-a-counter)
    - [Specifying the Actions](#specifying-the-actions)
    - [Running the Property](#running-the-property)
    - [Tuning the Action Generators](#tuning-the-action-generators)
- [Model-based Testing](#model-based-testing)
  - [A Counter Model](#a-counter-model)
- [Example: TeCoC - The Contrived CMS](#example-tecoc---the-contrived-cms)
  - [TecocPersistence Implementation](#tecocpersistence-implementation)
    - [Database Schema](#database-schema)
    - [JDBC Code](#jdbc-code)
  - [The Property](#the-property)
  - [Initial Model](#initial-model)
  - [Action: Create New User](#action-create-new-user)
  - [Action: Create a Post](#action-create-a-post)
  - [A bit of clean up](#a-bit-of-clean-up)
  - [Action: Delete a User](#action-delete-a-user)
  - [Extended Statistics](#extended-statistics)
  - [Asserting Invariants](#asserting-invariants)
- [Summary](#summary)
- [Feedback](#feedback)
- [Sharing, Code and License](#sharing-code-and-license)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Introduction

When you're doing 
[property-based testing](https://hypothesis.works/articles/what-is-property-based-testing/) 
the biggest challenge is to find 
good properties, which is arguably much harder than coming up with example-based tests.
Given PBT's 20-year history quite a few patterns and strategies have been discovered
that can guide you towards useful and effective properties. A detailed view into
[John Hughes's](https://twitter.com/rjmh) good practices is his recent paper 
[_How to Specify it!_](https://www.dropbox.com/s/tx2b84kae4bw1p4/paper.pdf).
Or, if you prefer Java over Haskell code, read my 
[line by line translation](https://johanneslink.net/how-to-specify-it/)
of his contents.

One of the approaches he presents are [model-based properties](https://johanneslink.net/how-to-specify-it/#45-model-based-properties). This kind of properties is interesting
in cases when your subject under test is not a pure function (all inputs
determine unambiguously all output and effects), but some kind of object or process
in which you have to deal with stateful behaviour.

In this article I want to address how to test stateful objects and applications with
properties; _model-based properties_ play an important role there.
The final motivation for this write-down came from 
[Jacob Stanley's article](https://jacobstanley.io/how-to-use-hedgehog-to-test-a-real-world-large-scale-stateful-app/), in which he describes 
"How to use Hedgehog to test a real world, large scale, stateful app".
I confess to have stolen more than one idea from him.

### How to do PBT in Java

I've already written a 
[whole blog series](https://blog.johanneslink.net/2018/03/24/property-based-testing-in-java-introduction/)
on that topic, that's why I'm not covering the basics here. 
Reading [this article in Oracle's Java Magazine](https://blogs.oracle.com/javamagazine/know-for-sure-with-property-based-testing)
will also give you an overview. 

#### How jqwik Works

Ok, you're too ~~lazy~~ busy to read one of the articles above. 
Here's the bare minimum of [jqwik](https://jqwik.net)'s syntax that you should understand 
when you want to follow along. 
[Skip](#stateful-testing) without regret if you already know the fundamentals.

```java
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
```

Above you see a working and runnable but completely meaningless property. 
You can start it like any JUnit 5 platform test using Gradle, Maven or your favourite IDE.
The resulting console output will look similar to:

```
anyString:  靐乺 廛䷛ 㡡㝮屠 
anEvenInt: 176926354
anyString:  孯⇳餖᭞ၖ
anEvenInt: -568892912
anyString: 谏崢 麊⃠ 栦 ꆬ  ㄵᥞꓨṯ跚პ疽퇺꨹ 冊뀃쁾䌬ص猽 툲
anEvenInt: -2147483648
anyString:  俕 ⍍畽     䂍㷼   ⬀∄髨 ⋏ ប  Ჱ 
anEvenInt: 10
...
``` 

The library will generate values for all parameters annotated with `@ForAll`.
It will either use the parameter's type - `@ForAll String anyString` - to invoke  one of its various default generators. 
Or it will refer to a `@Provider` method specified by the annotations `value()`
attribute: `@ForAll("evenNumbers") int anEvenInt` refers to the method named
`evenNumbers`. Generators, which have the monadic type `Arbitrary`, are usually
described as a _filter_, _map_ or _combination_ of other generators. Most built-in
generators can be accessed from a static method on class `Arbitraries`. 
Anything else [you'd like to know](https://jqwik.net/docs/current/user-guide.html)?


## Stateful Testing

So what's the problem with properties and state? PBT started its life in Haskell,
a programming language that has _pure functions_ coded into its DNA. It's therefore
not a coincidence that most of the examples that the PBT newbie sees in talks and
tutorials are about applying PBT to functions. When you search the web for 
"patterns and property-based testing" you often find articles 
[like this](https://blog.ssanj.net/posts/2016-06-26-property-based-testing-patterns.html).
None of the patterns described there lends itself well to stateful systems.

Digging a bit further, though, the idea of using properties to explore the realm
of [finite state machines](https://blog.johanneslink.net/2018/09/06/stateful-testing/#finite-state-machines) 
shows up. In the end, most - if not all - stateful objects can be considered 
to be state machines of a sort, which gives you the following characteristics:

- There is a _finite_ set of possible states and the object is 
  always in a single, defined state.
- For every state there is a finite and possible empty list of 
  transitions that bring the state machine into its next state.
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
a few different states and transitions to consider. The typical goal of any testing
approach is to cover at least all transitions. Using good old example tests
this requires at least 8 scenarios. We want to tackle the challenge with PBT 
and build on 
[jqwik's support for stateful testing](https://jqwik.net/docs/current/user-guide.html#stateful-testing).

#### Specifying the Actions

The first step is usually to specify the actions. 
We choose _counting up_ as the first one to tackle:

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
[AssertJ](https://assertj.github.io/doc/) to check that the counter's value increases. 
But actually that's not always the case as our state transition table shows. 
We now have the choice to either create branches in our assertion logic:

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
  public boolean precondition(Counter counter) {
    return counter.value() < 100;
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
  public boolean precondition(Counter counter) {
    return counter.value() == 100;
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
  public boolean precondition(Counter counter) {
    return counter.value() > 0;
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
  public boolean precondition(Counter counter) {
    return counter.value() == 0;
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

`ActionSequenc<T>` is a built-in type supported by the generator function
`Arbitraries.sequences(...)` which takes care of generating only possible
sequences of actions.
Running the property above will immediately reveal a bug in our implementation:

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

The optimistic tester might expect that our property would now detect 
the other obvious thing we missed: Not checking the max value of `100`.
But to our distress it does not!

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
```

To collect statistics about our counter's states and state transitions we can use 
`peek(..)` which gives us access to the internal state after each successful
execution of an action's `run(..)` method. Checking the count or percentage
of collected statistical values is done in a lambda parameter to 
`Statistics.coverage(..)`.
With those checks in place the property is now failing:

```
org.opentest4j.AssertionFailedError: Count of 0 for ["at max"] does not fulfill condition
```

What can we do to have this case covered as well? 
One trick in our bag is to introduce another action that will make the probability
of hitting the missing state much more likely. We could, for example,
randomly raise the counter by a value between `0` and `99`:

```java
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
```

We also have to modify our sequences generator to include this action.
I have chosen a ratio of 5 to 1 between standard actions and the new
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
          .shrinkTowards(99) // This improves shrinking
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
        raise by 99
        count up
        count up at max
      final currentModel: Counter[101]}

Expecting:
 <101>
to be equal to:
 <100>
but was not.
```

The fix is trivial and the happy ending is filling our souls with joy.

## Model-based Testing

One can argue that the `Counter` example is too simple; 
"simple" in the sense that all postconditions,
which are essential for checking an implementation's validity, could be stated
without much knowledge about previous actions and complicated internal state.
In cases where state-dependency is more involved we will have to find additional
tools in order to specify the expected outcome of an action with sufficient precision. 

As the term _Model-based Testing_ implies we can use a _model_ of our
subject under test (SUT). The model is mainly used to solve 
the ["oracle problem of test case generation"](http://www0.cs.ucl.ac.uk/staff/m.harman/tse-oracle.pdf): 
How do we know the expected behaviour of a SUT without duplicating the system's logic in the test itself?
The answer to this question differs from situation to situation. 
Using an explicit model makes sense when the publicly exposed behaviour is
rather simple but the technical intricacies of its implementation bring risk.

Here's a [quote from Oskar Wickström on Twitter](https://twitter.com/owickstrom/status/1229997807710429184):

> Model-based properties tend to be very precise and mirror the SUT. 
> I've found they make most sense when your system has more non-functional 
> complexity, rather than inherent problem domain complexity.

In other words: If implementing a model is (almost) as complicated as the target
implementation it might not be worth it. Since you'll be using the
model as a reference you must have trust in its correctness. If it requires
the same testing and implementation effort as the SUT itself, nothing is gained.

### A Counter Model

Creating a model for `Counter` is not difficult but it will have - more or less - 
the same code as the original model:

```java
public class CounterModel {
  private int value = 0;
  void up() {
    if (value == 100) {
      return;
    }
    value++;
  }
  void down() {
    if (value == 0) {
      return;
    }
    value--;
  }
  int getValue() {
    return value;
  }
}
```

I tried to vary a bit on naming and branching logic but you can see that
model-based testing does not make a lot of sense here - except for learning
how to use a model in stateful properties. Let's therefore use it in a revised 
count-up action:

```java
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
}
```

Three things are worth noticing here:

- When using a model-based approach most actions look alike: 
  1. Perform the action on SUT and model,
  2. Then assert that SUT and model have the same state.

- Since an action needs access to both SUT and model we need 
  to make the two available in the action.
  Using jqwik's `Tuple` type is one option; we could also use any other container 
  or even hold an instance of the SUT within the model.

- A trustworthy model frees us from some case-based analysis: We don't need
  to differentiate between a normal count-up action and a count-up action with
  maximum value anymore; the model has this difference already integrated.
  
The remaining two actions must be changed similarly; the property can stay
the same except for the type changes and the creation of the initial state. 
Without all the coverage checking it looks like this:

```java
@Property
void checkCounter(@ForAll("counterActions") ActionSequence<Tuple2<Counter, CounterModel>> actions) {
  actions.run(Tuple.of(new Counter(), new CounterModel()));
}
```

Now that we have seen the _mechanics_ of model-based testing it's high time to
apply the new knowledge on a more complex problem.


## Example: TeCoC - The Contrived CMS

The app we are going to tackle is a small _content management system_ handling
_users_ and their _posts_. I've stolen the example domain from 
[the article mentioned above](https://jacobstanley.io/how-to-use-hedgehog-to-test-a-real-world-large-scale-stateful-app/)
which itself states that the database schema 
[was borrowed here](https://github.com/saurabhnanda/hedgehog-db-testing/blob/master/src/Main.hs#L48-L49).

The stateful object we want to scrutinize using model-base testing is the system's
persistence layer. It has a straightforward interface:

```java
public class TecocPersistence implements AutoCloseable {

  public TecocPersistence(Connection connection) { ... }

  // Make sure the schema exists in the database
  public void initialize() { ... }

  // Clear all data: For testing only
  public void reset() { ... }

  // Close connection to database
  public void close() { ... }

  public int countUsers() { ... }

  public int createUser(User newUser) { ... }

  public Optional<User> readUser(int userId) { ... }

  public boolean deleteUser(int userId) { ... }

  public int countPosts() { ... }

  public int createPost(Post newPost) { ... }

  public Optional<Post> readPost(int postId) { ... }

  public boolean deletePost(int postId) { ... }

}
```

Both 
[`User`](https://github.com/jlink/model-based-testing/blob/master/src/main/java/mbt/tecoc/User.java) 
and [`Post`](https://github.com/jlink/model-based-testing/blob/master/src/main/java/mbt/tecoc/Post.java) 
classes are simple data holders: 

```java
public class User {
  public User(String name, String email) { ... }

  public int getId() { ... }

  public String getName() { ... }

  public String getEmail() { ... }

  public Instant getCreatedAt() { ... }
}
```

```java
public class Post {
  public Post(int userId, String title, String body) { ... }

  public int getId() { ... }

  public int getUserId() { ... }

  public String getTitle() { ... }

  public String getBody() { ... }

  public Instant getCreatedAt() { ... }
}
```

### TecocPersistence Implementation

Most "normal" Java projects would probably use _Spring Data_ or at least some 
_ORM_ mapping library. However, since I did not want
this code base to have any distracting dependencies I exhumed my long buried JDBC
knowledge and came up with a 
[straightforward implementation](https://github.com/jlink/model-based-testing/blob/master/src/main/java/mbt/tecoc/TecocPersistence.java) 
without any magic and simple SQL statements.

#### Database Schema

The schema has two tables:

```
CREATE TABLE IF NOT EXISTS users(
        id SERIAL PRIMARY KEY, 
        name TEXT NOT NULL, 
        email TEXT NOT NULL, 
        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS posts(
        id SERIAL PRIMARY KEY, 
        user_id INTEGER NOT NULL REFERENCES users(id), 
        title TEXT NOT NULL, 
        body TEXT NOT NULL, 
        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

#### JDBC Code

The access to sql connections and statements is encapsulated in order to
get rid of some boiler-plate code duplication. Look at the methods for creating
and reading users to get an impression of how sql-handling code is structured:

```java
public int createUser(User newUser) {
  return usePreparedStatement(
      "INSERT INTO users(name, email) VALUES(?, ?)",
      statement -> {
        statement.setString(1, newUser.getName());
        statement.setString(2, newUser.getEmail());
        int count = statement.executeUpdate();
        if (count > 0) {
          ResultSet generatedKeys = statement.getGeneratedKeys();
          generatedKeys.next();
          return generatedKeys.getInt("id");
        } else {
          return 0;
        }
      }
  );
}

public Optional<User> readUser(int userId) {
  return usePreparedStatement(
      "SELECT * FROM users WHERE id=?",
      statement -> {
        statement.setInt(1, userId);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            return Optional.of(User.fromResultSet(resultSet));
        } else {
          return Optional.empty();
        }
      }
  );
}
```

Nothing more to see here. Except maybe that I test-drove the implementation
with [a few integrated tests](https://github.com/jlink/model-based-testing/blob/master/src/test/java/mbt/tecoc/TecocPersistenceTests.java)
using an in-memory [HSQLDB](http://www.hsqldb.org/) database
in PostgreSQL compatibility mode.

### The Property

As already shown in the `Counter` example stateful properties are rather simple. 
All you have to do is running a generated sequence of actions and see if something goes wrong.

By annotating methods with `@BeforeContainer`, `@BeforeTry` and `@AfterTry`
you can hook into the lifecycle of _jqwik_'s property execution. While the
database driver must be loaded only once, the actual database connection 
will be created freshly for each _try_ - jqwik speak for a single invocation
of a property. The database itself will be `reset` after each try to ensure
that different invocations don't get into each other's way.

```java
class TecocPersistenceProperties {

  final static String driverClassName = "org.hsqldb.jdbc.JDBCDriver";
  final static String url = "jdbc:hsqldb:mem:tecoc;sql.syntax_pgs=true";
  final static String username = "sa";
  final static String password = "";

  private TecocPersistence persistence;

  @BeforeContainer
  static void initInMemoryDatabaseDriver() throws Exception {
    Class.forName(driverClassName);
  }

  @BeforeTry
  void initPersistence() throws SQLException {
    Connection connection = DriverManager.getConnection(url, username, password);
    persistence = new TecocPersistence(connection);
    persistence.initialize();
  }

  @AfterTry
  void closePersistence() throws SQLException {
    persistence.reset();
    persistence.close();
  }

  @Property
  void checkPersistence(@ForAll("persistenceActions") ActionSequence<Tuple2<TecocPersistence, PersistenceModel>> actions) {
    actions.run(Tuple.of(persistence, new PersistenceModel()));
  }

  @Provide
  Arbitrary<ActionSequence<Tuple2<TecocPersistence, PersistenceModel>>> persistenceActions() {
        // Generate persistence actions
        return ...;
  }
}
```

The actual checking logic will be in the model and the generated actions.
Let's start with covering user creation logic.

### Initial Model

What we need for a model-based property is a model that mimics the desired behaviour
for user creation. Under the hood some collection of `User` objects will do,
but we are going to encapsulate it behind an interface similar to what 
`TecocPersistence` reveals to the public:

```java
public class PersistenceModel {
    public void addUser(int userId, User newUser) { ... }
    public int countUsers() { ... }
    public Optional<User> readUser(int userId) { ... }
}
```

The astute reader will notice that I diverged from `TecocPersistence` method signatures
in one place; instead of `createUser` I added the method `addUser(..)` 
that takes `userId` as an additional parameter. 
I did this for two reasons:

1. I relieve the model from having to create IDs of its own
2. The IDs will be around when I need them later in other actions, e.g.
   for creating posts for a specific user.
   
The model implementation can now use a simple list to track known users:

```java
public class PersistenceModel {

  private final List<User> users = new ArrayList<>();

  public void addUser(int userId, User newUser) {
    newUser.setId(userId);
    users.add(newUser);
  }

  public int countUsers() {
    return users.size();
  }

  public Optional<User> readUser(int userId) {
    return users.stream()
          .filter(user -> user.getId() == userId)
          .findFirst();
  }
}
```

Note that the model's implementation is considerably simpler than the real persistence
class. This is a sign that model-based testing may be a good fit here. 
We might however find out later that it's actually too simple and must be refined; 
for example, `createUser(..)` does not fill the user's `createdAt` field.

### Action: Create New User

A new user needs a name and an email address. 

```java
class CreateNewUserAction implements Action<Tuple2<TecocPersistence, PersistenceModel>> {

  private final String userName;
  private final String userEmail;

  CreateNewUserAction(String userName, String userEmail) {
    this.userName = userName;
    this.userEmail = userEmail;
  }

  @Override
  public Tuple2<TecocPersistence, PersistenceModel> run(Tuple2<TecocPersistence, PersistenceModel> state) {
    User newUser = new User(userName, userEmail);
    int newId = state.get1().createUser(newUser);
    assertThat(newId).isNotZero();
    state.get2().addUser(newId, newUser);

    compareReadUser(newId, state);
    compareCounts(state);

    return state;
  }

  private void compareReadUser(int userId, Tuple2<TecocPersistence, PersistenceModel> state) {
    Optional<User> optionalUser = state.get1().readUser(userId);
    Optional<User> optionalUserFromModel = state.get2().readUser(userId);
    assertThat(optionalUser.isPresent()).isEqualTo(optionalUserFromModel.isPresent());
    optionalUser.ifPresent(user -> {
      optionalUserFromModel.ifPresent(userFromModel -> {
        assertThat(user.getName()).isEqualTo(userFromModel.getName());
        assertThat(user.getEmail()).isEqualTo(userFromModel.getEmail());
      });
    });
  }

  private void compareCounts(Tuple2<TecocPersistence, PersistenceModel> state) {
    assertThat(state.get1().countUsers()).isEqualTo(state.get2().countUsers());
  }

  @Override
  public String toString() {
    return String.format("create-new-user[%s, %s]", userName, userEmail);
  }
}
```

After creating the new user in the database it is added to the model for later availability.
The methods `compareReadUser` and `compareCounts` look like candidates for an 
abstract persistence action. Implementing `toString()` in action classes
is a good practice because it makes the understanding of falsified sequence runs easier. 

What's missing for a runnable property is the generation of
`CreateNewUserAction` instances with random values for `userName` and `userEmail`:

```java
class TecocPersistenceProperties...

  @Provide
  Arbitrary<ActionSequence<Tuple2<TecocPersistence, PersistenceModel>>> persistenceActions() {
    return Arbitraries.sequences(createNewUserAction());
  }

  private Arbitrary<Action<Tuple2<TecocPersistence, PersistenceModel>>> createNewUserAction() {
    Arbitrary<String> names = Arbitraries.strings().alpha().ofMinLength(1);
    Arbitrary<String> domains = Arbitraries.of("somemail.com", "mymail.net", "whatever.info");
    return Combinators.combine(names, domains).as((userName, domain) -> {
      String email = userName + "@" + domain;
      return new CreateNewUserAction(userName, email);
    });
  }
``` 

Running the property now does not reveal any bugs. 
With the default number of tries set to `1000` it already takes a few seconds -
despite our using an in-memory database. Be prepared for much longer run times
when you are using file based databases in integrated testing - or to tune down
the number of tries. 

### Action: Create a Post

Creating a post is similar to creating a user except for one detail: 
posts are always related to an existing user, so we have to provide that
user's ID at creation time. Since there is a _foreign-key relation_ in our
[database schema](#database-schema) we don't have the option to just generate
a random user ID. We can, however, randomly pick one of the existing users and take
their ID. The trick we use here is to retrieve a user by its _index_ 
(i.e. position in the list of users); if we use
the random index in a "wrap around" way - i.e. `index % users.size()` - it will
always point to an existing user, given that there is at least one user in the
database, which is a precondition for the create-post action.

Here's the enhanced model:

```java
public class PersistenceModel {

  private final List<User> users = new ArrayList<>();
  private final List<Post> posts = new ArrayList<>();

  public User userByIndex(int index) {
    int wrapAroundIndex = index % (users.size());
    return users.get(wrapAroundIndex);
  }

  public void addPost(int postId, Post newPost) {
    newPost.setId(postId);
    posts.add(newPost);
  }

  public int countPosts() {
    return posts.size();
  }

  public Optional<Post> readPost(int postId) {
    return posts.stream()
          .filter(post -> post.getId() == postId)
          .findFirst();
  }
}
```

With `userByIndex(..)` in place we can now code the create-post action:

```java
class CreatePostAction implements Action<Tuple2<TecocPersistence, PersistenceModel>> {

  private final int userIndex;
  private final String title;
  private final String body;

  CreatePostAction(int userIndex, String title, String body) {
    this.userIndex = userIndex;
    this.title = title;
    this.body = body;
  }

  @Override
  public boolean precondition(Tuple2<TecocPersistence, PersistenceModel> state) {
    return state.get2().countUsers() >= 1;
  }

  @Override
  public Tuple2<TecocPersistence, PersistenceModel> run(Tuple2<TecocPersistence, PersistenceModel> state) {
    int userId = state.get2().userByIndex(userIndex).getId();
    Post newPost = new Post(userId, title, body);
    int newId = state.get1().createPost(newPost);
    assertThat(newId).isNotZero();
    state.get2().addPost(newId, newPost);

    compareReadPost(newId, state);
    compareCounts(state);

    return state;
  }

  private void compareReadPost(int postId, Tuple2<TecocPersistence, PersistenceModel> state) {
    Optional<Post> optionalPost = state.get1().readPost(postId);
    Optional<Post> optionalPostFromModel = state.get2().readPost(postId);
    assertThat(optionalPost.isPresent()).isEqualTo(optionalPostFromModel.isPresent());
    optionalPost.ifPresent(post -> {
      optionalPostFromModel.ifPresent(postFromModel -> {
        assertThat(post.getTitle()).isEqualTo(postFromModel.getTitle());
        assertThat(post.getBody()).isEqualTo(postFromModel.getBody());
      });
    });
  }

  private void compareCounts(Tuple2<TecocPersistence, PersistenceModel> state) {
    assertThat(state.get1().countUsers()).isEqualTo(state.get2().countUsers());
    assertThat(state.get1().countPosts()).isEqualTo(state.get2().countPosts());
  }

  @Override
  public String toString() {
    return String.format("create-post[userIndex=%d, %s, %s]", userIndex, title, body);
  }
}
```

Mind that the precondition is crucial. If we leave it out we will most probably run into
a division-by-zero exception. In the precondition we have the choice to use either
the model or the persistence object itself. I tend to use the model because
it does not require database access.

Let's finally add the new action type to our sequence generation:

```java
class TecocPersistenceProperties...
  @Provide
  Arbitrary<ActionSequence<Tuple2<TecocPersistence, PersistenceModel>>> persistenceActions() {
    return Arbitraries.sequences(
        Arbitraries.oneOf(
            createNewUserAction(),
            createPostAction()
        ));
  }

  private Arbitrary<Action<Tuple2<TecocPersistence, PersistenceModel>>> createPostAction() {
    Arbitrary<Integer> indices = Arbitraries.integers().between(0, 100);
    Arbitrary<String> titles = Arbitraries.strings().alpha().ofMinLength(1);
    Arbitrary<String> bodies = Arbitraries.strings().ofMinLength(1);
    return Combinators.combine(indices, titles, bodies).as(CreatePostAction::new);
  }
```

Everything seems to work fine. No bugs detected.
To make sure that the two possible actions are really applied 
we want to look at the statistics for the different
types of actions. We can only collect that information _after_ a sequence has run 
because the individual actions are created on the fly:

```java
class TecocPersistenceProperties...
  @Property
  void checkPersistence(@ForAll("persistenceActions") ActionSequence<Tuple2<TecocPersistence, PersistenceModel>> actions) {
    actions.run(Tuple.of(persistence, new PersistenceModel()));

    actions.runActions().forEach(action -> Statistics.collect(action.getClass().getSimpleName()));
  }
```

The statistics report looks as expected; the two different kinds of actions are
almost evenly distributed:

```
[TecocPersistenceProperties:checkPersistence] (32000) statistics = 
    CreateNewUserAction (16457) : 51 %
    CreatePostAction    (15543) : 49 %
```

### A bit of clean up

In order to facilitate the coding of new action types I extract an abstract superclass
that provides access to (potentially) shared assertion methods:

```java
abstract class AbstractPersistenceAction implements Action<Tuple2<TecocPersistence, PersistenceModel>> {

  void compareReadUser(int userId, Tuple2<TecocPersistence, PersistenceModel> state) {
    Optional<User> optionalUser = state.get1().readUser(userId);
    Optional<User> optionalUserFromModel = state.get2().readUser(userId);
    assertThat(optionalUser.isPresent()).isEqualTo(optionalUserFromModel.isPresent());
    optionalUser.ifPresent(user -> {
      optionalUserFromModel.ifPresent(userFromModel -> {
        assertThat(user.getName()).isEqualTo(userFromModel.getName());
        assertThat(user.getEmail()).isEqualTo(userFromModel.getEmail());
      });
    });
  }

  void compareReadPost(int postId, Tuple2<TecocPersistence, PersistenceModel> state) {
    Optional<Post> optionalPost = state.get1().readPost(postId);
    Optional<Post> optionalPostFromModel = state.get2().readPost(postId);
    assertThat(optionalPost.isPresent()).isEqualTo(optionalPostFromModel.isPresent());
    optionalPost.ifPresent(post -> {
      optionalPostFromModel.ifPresent(postFromModel -> {
        assertThat(post.getTitle()).isEqualTo(postFromModel.getTitle());
        assertThat(post.getBody()).isEqualTo(postFromModel.getBody());
      });
    });
  }

  void compareCounts(Tuple2<TecocPersistence, PersistenceModel> state) {
    assertThat(state.get1().countUsers()).isEqualTo(state.get2().countUsers());
    assertThat(state.get1().countPosts()).isEqualTo(state.get2().countPosts());
  }
}

class CreateNewUserAction extends AbstractPersistenceAction { ... }

class CreatePostAction extends AbstractPersistenceAction { ... }
```

### Action: Delete a User

So far the provided actions were too simple to reveal any hidden bugs. 
Maybe deleting users will eventually be worth the effort. For doing that
the model needs a little enhancement:

```java
public class PersistenceModel...
  public void removeUser(int userId) {
    users.removeIf(user -> user.getId() == userId);
  }
```

Of course, we create a new action for deleting an existing user:

```java
class DeleteUserAction extends AbstractPersistenceAction {

  private final int userIndex;

  DeleteUserAction(int userIndex) {
    this.userIndex = userIndex;
  }

  @Override
  public boolean precondition(Tuple2<TecocPersistence, PersistenceModel> state) {
    return state.get2().countUsers() >= 1;
  }

  @Override
  public Tuple2<TecocPersistence, PersistenceModel> run(Tuple2<TecocPersistence, PersistenceModel> state) {
    int userId = state.get2().userByIndex(userIndex).getId();
    state.get1().deleteUser(userId);
    state.get2().removeUser(userId);

    compareCounts(state);

    return state;
  }

  @Override
  public String toString() {
    return String.format("delete-user[userIndex=%d]", userIndex);
  }
}
```

And add it to the list of generated actions:

```java
class TecocPersistenceProperties...

  @Provide
  Arbitrary<ActionSequence<Tuple2<TecocPersistence, PersistenceModel>>> persistenceActions() {
    return Arbitraries.sequences(
        Arbitraries.oneOf(
            createNewUserAction(),
            createPostAction(),
            deleteUserAction()
        ));
  }

  private Arbitrary<Action<Tuple2<TecocPersistence, PersistenceModel>>> deleteUserAction() {
    Arbitrary<Integer> indices = Arbitraries.integers().between(0, 100);
    return indices.map(DeleteUserAction::new);
  }
```

Heureka! Rerunning the property fails with the following exception:

```
org.opentest4j.AssertionFailedError: Run failed after following actions:
    create-new-user[A, A@somemail.com]
    create-post[userIndex=0, A,  ]
    delete-user[userIndex=0]
  final currentModel: (mbt.tecoc.TecocPersistence@55f45b92,mbt.tecoc.PersistenceModel@109f5dd8)
java.sql.SQLIntegrityConstraintViolationException: 
    integrity constraint violation: foreign key no action; SYS_FK_10100 table: POSTS
```

This tells us two things:

- We forgot to implement `toString()` in `PersistenceModel`. 
  We should catch up on that at some time; I usually wait till I need the information.
- `TecocPersistence.deleteUser(userId)` does not consider the foreign-key constraint
  on the `posts` table.
  
In order to make the property successful again we have to decide if
a) deleting users with posts should be impossible, or
b) deleting a user should delete all their posts first.
We go with a) which requires to tweak `DeleteUserAction` such
that deletion will not be triggered if the selected user has posts.
This can be done by changing the precondition or by branching
the action's `run` method. 
I strongly prefer the precondition change because it 
requires less logic in our testing code:

```java
class DeleteUserAction...
  @Override
  public boolean precondition(Tuple2<TecocPersistence, PersistenceModel> state) {
    PersistenceModel model = state.get2();
    if (model.countUsers() == 0) {
      return false;
    }
    int userId = model.userByIndex(userIndex).getId();
    return model.hasNoPosts(userId);
  }

class PersistenceModel...
  public boolean hasNoPosts(int userId) {
    return posts.stream().noneMatch(post -> post.getUserId() == userId);
  }
``` 

Everything's back to green now and the statistics show that 
the _delete-user_ action type still gets its fair share:

```
[TecocPersistenceProperties:checkPersistence] (32000) statistics = 
    CreateNewUserAction (14247) : 45 %
    CreatePostAction    (12673) : 40 %
    DeleteUserAction    ( 5080) : 16 %
```

### Extended Statistics

Are you interested in learning more details about the database you end up with?
What about adding the following lines to the `TecocPersistenceProperties.checkPersistence`:

```java
int countUsers = actions.finalModel().get2().countUsers();
String usersClassifier = countUsers <= 10 ? "<= 10" : "> 10";
Statistics.label("users").collect(usersClassifier);

int countPosts = actions.finalModel().get2().countPosts();
String postsClassifier = countPosts <= 10 ? "<= 10"
          : countPosts <= 20 ? "<= 20" : "> 20";
Statistics.label("posts").collect(postsClassifier);
```

Et voilà, two new statistic blocks are reported:

```
[TecocPersistenceProperties:checkPersistence] (1000) users = 
    <= 10 (706) : 71 %
    > 10  (294) : 29 %

[TecocPersistenceProperties:checkPersistence] (1000) posts = 
    <= 20 (749) : 74.90 %
    <= 10 (246) : 24.60 %
    > 20  (  5) :  0.50 %
```

### Asserting Invariants

In stateful systems domain or business rules often come in the form of invariants.
Consider the counter from above that should _invariably_ be between 0 and 100.
One way to assure that an invariant always holds is to embed the invariant 
in the model's behaviour and make sure that the relevant state will be checked 
after each action. That's what we did in [A Counter Model](#a-counter-model).

An alternative approach is to externalize the invariant and check it in the property.
_jqwik_ offers a nice way to do just that:

```java
class TecocPersistenceProperties...
  @Property
  void checkDuplicateEmailsArePrevented(@ForAll("persistenceActions") ActionSequence<Tuple2<TecocPersistence, PersistenceModel>> actions) {
    Invariant<Tuple2<TecocPersistence, PersistenceModel>> noDuplicateEmails =
        tuple -> {
          PersistenceModel model = tuple.get2();
          Map<String, Long> emailCounts =
              model.users().stream().collect(
                  Collectors.groupingBy(
                      User::getEmail, Collectors.counting()
                  )
              );
          emailCounts.forEach((email, count) -> {
            Assertions.assertThat(count)
                  .describedAs("Email: %s has duplicate", email)
                  .isLessThan(2);
          });
        };

    actions.withInvariant("no duplicate emails", noDuplicateEmails)
           .run(Tuple.of(persistence, new PersistenceModel()));
  }

class PersistenceModel...
  public List<User> users() {
    return users;
  }
```

Running this property will sometimes succeed - and sometimes fail with an error:

```
net.jqwik.engine.properties.stateful.InvariantFailedError: Invariant 'no duplicate emails' failed after following actions:
    create-new-user[b, b@mymail.net]
    create-new-user[b, b@mymail.net]
  final currentModel: (mbt.tecoc.TecocPersistence@5df63359,mbt.tecoc.PersistenceModel@53d2d002)
[Email: b@mymail.net has duplicate] 
```

The reason for this flaky falsification outcome is how we chose to generate 
emails for new users:

```java
private Arbitrary<Action<Tuple2<TecocPersistence, PersistenceModel>>> createNewUserAction() {
	Arbitrary<String> names = Arbitraries.strings().alpha().ofMinLength(1);
	Arbitrary<String> domains = Arbitraries.of("somemail.com", "mymail.net", "whatever.info");
	return Combinators.combine(names, domains).as((userName, domain) -> {
		String email = userName + "@" + domain;
		return new CreateNewUserAction(userName, email);
	});
}
```

Since a name can be any String with alphabetic characters of length 1 or more 
and is then combined with one of three domains the chance of generating the same
email at least twice in a 1000 tries is not close enough to 100 percent.
If we had been more inventive with varying email domains we would probably never
see a duplicate email at all!

There is an important lesson to learn here: When you rely on properties to catch
bugs for specific data scenarios you have to make the occurrence of those scenarios
likely. How you can do that differs from case to case. In our scenario you might
inject a few standard user names with higher probability:

```java
private Arbitrary<Action<Tuple2<TecocPersistence, PersistenceModel>>> createNewUserAction() {
  Arbitrary<String> names = Arbitraries.strings().alpha().ofMinLength(1);
  return Combinators.combine(names, emails())
                    .as(CreateNewUserAction::new);
}

private Arbitrary<String> emails() {
  Arbitrary<String> userNames = Arbitraries.oneOf(
      Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(12),
      Arbitraries.of("user1", "user2", "user3")
  );
  Arbitrary<String> domains = Arbitraries.of(
      "somemail.com", "mymail.net", "whatever.info"
  );
  return Combinators.combine(userNames, domains)
                    .as((userName, domain) -> userName + "@" + domain);
}
```

Now that we have discovered the violated domain invariant, how do we fix 
the implementation? The smallest code change I can think of is to just
swallow an attempt to create a new user with an previously known email address.
Adding a unique constraint in the database model and ignoring the resulting
SQL exception would do the trick. 

But hey, _YOU_ would never do such a terrible thing, would you?
Instead, _YOU_ would find out what the expected behaviour really is - maybe
communicate the failing attempt to add a user with a domain-specific exception?
And then change model and implementation synchronously.

## Summary

Property-based testing of stateful systems can get a bit more involved than
using PBT for plain functions and systems without relevant state. 
Sometimes it's easy to use post-conditions for the various actions that
can be applied. In other cases it's much easier to write an explicit model that
duplicates some aspects of the implementation under tests and can thereby 
serve as an easy-to-use oracle for an action's outcome.

## Feedback 

Give feedback, ask questions and tell me about your own PBT experiences 
on [Twitter](https://twitter.com/johanneslink).


## Sharing, Code and License

This article is published under the following license:
[Attribution-ShareAlike 4.0 International](https://creativecommons.org/licenses/by-sa/4.0/)

All code is [available at Github](https://github.com/jlink/model-based-testing) 