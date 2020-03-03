package mbt.tecoc;

import java.sql.*;

import net.jqwik.api.*;
import net.jqwik.api.Tuple.*;
import net.jqwik.api.lifecycle.*;
import net.jqwik.api.stateful.*;
import net.jqwik.api.statistics.Statistics;

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

		actions.runActions().forEach(action -> Statistics.collect(action.getClass().getSimpleName()));
	}

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

	private Arbitrary<Action<Tuple2<TecocPersistence, PersistenceModel>>> createNewUserAction() {
		Arbitrary<String> names = Arbitraries.strings().alpha().ofMinLength(1);
		Arbitrary<String> domains = Arbitraries.of("somemail.com", "mymail.net", "whatever.info");
		return Combinators.combine(names, domains).as((userName, domain) -> {
			String email = userName + "@" + domain;
			return new CreateNewUserAction(userName, email);
		});
	}

}
