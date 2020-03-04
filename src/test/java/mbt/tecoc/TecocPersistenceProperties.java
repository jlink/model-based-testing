package mbt.tecoc;

import java.sql.*;
import java.util.*;
import java.util.stream.*;

import net.jqwik.api.*;
import net.jqwik.api.Tuple.*;
import net.jqwik.api.lifecycle.*;
import net.jqwik.api.stateful.*;
import net.jqwik.api.statistics.Statistics;
import org.assertj.core.api.*;

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

		int countUsers = actions.finalModel().get2().countUsers();
		String usersClassifier = countUsers <= 10 ? "<= 10" : "> 10";
		Statistics.label("users").collect(usersClassifier);

		int countPosts = actions.finalModel().get2().countPosts();
		String postsClassifier = countPosts <= 10 ? "<= 10"
										 : countPosts <= 20 ? "<= 20" : "> 20";
		Statistics.label("posts").collect(postsClassifier);
	}

	@Property(afterFailure = AfterFailureMode.RANDOM_SEED)
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

	private Arbitrary<Action<Tuple2<TecocPersistence, PersistenceModel>>> createPostAction() {
		Arbitrary<Integer> indices = Arbitraries.integers().between(0, 100);
		Arbitrary<String> titles = Arbitraries.strings().alpha().ofMinLength(1);
		Arbitrary<String> bodies = Arbitraries.strings().ofMinLength(1);
		return Combinators.combine(indices, titles, bodies).as(CreatePostAction::new);
	}

	private Arbitrary<Action<Tuple2<TecocPersistence, PersistenceModel>>> createNewUserAction() {
		Arbitrary<String> names = Arbitraries.strings().alpha().ofMinLength(1);
		return Combinators.combine(names, emails()).as(CreateNewUserAction::new);
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

}
