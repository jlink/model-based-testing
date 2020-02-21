package mbt.tecoc;

import java.sql.*;
import java.util.*;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.*;

import static org.assertj.core.api.Assertions.*;

class TecocPersistenceTests {

	final static String driverClassName = "org.hsqldb.jdbc.JDBCDriver";
	final static String url = "jdbc:hsqldb:mem:tecoc;sql.syntax_pgs=true";
	final static String username = "sa";
	final static String password = "";

	private TecocPersistence persistence;

	@BeforeContainer
	static void initInMemoryDatabaseDriver() throws Exception {
		Class.forName(driverClassName);
	}

	@BeforeProperty
	void initPersistence() throws SQLException {
		Connection connection = DriverManager.getConnection(url, username, password);
		persistence = new TecocPersistence(connection);
		persistence.initialize();
	}

	@AfterProperty
	void closePersistence() throws SQLException {
		persistence.close();
	}

	@Example
	void newPersistenceIsEmpty() {
		assertThat(persistence.findAllUsers()).isEmpty();
	}

	@Example
	void createNewUser() {
		User newUser = new User("Johannes", "jl@johanneslink.net");
		int newId = persistence.createUser(newUser);
		assertThat(newId).isNotEqualTo(0);

		assertThat(persistence.findAllUsers()).hasSize(1);

		Optional<User> readUser = persistence.readUser(newId);
		assertThat(readUser).isPresent();
		readUser.ifPresent(user -> {
			assertThat(user.getName()).isEqualTo("Johannes");
			assertThat(user.getEmail()).isEqualTo("jl@johanneslink.net");
		});
	}

	@Example
	void deleteUser() {
		User newUser = new User("Johannes", "jl@johanneslink.net");
		int userId = persistence.createUser(newUser);

		assertThat(persistence.deleteUser(userId)).isTrue();
		assertThat(persistence.readUser(userId)).isNotPresent();
	}

}
