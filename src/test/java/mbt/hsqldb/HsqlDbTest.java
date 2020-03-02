package mbt.hsqldb;

import java.sql.*;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.*;

class HsqlDbTest {
	final static String driverClassName = "org.hsqldb.jdbc.JDBCDriver";
	final static String url = "jdbc:hsqldb:mem:testdb;DB_CLOSE_DELAY=-1";
	final static String username = "sa";
	final static String password = "";

	private Connection connection;

	@BeforeContainer
	static void initDatabase() throws Exception {
		Class.forName(driverClassName);
		Connection connection = DriverManager.getConnection(url, username, password);
		try {
			executeDDL(
					connection,
					"create table users(name varchar(256))"
			);
		} finally {
			connection.close();
		}
	}

	@BeforeProperty
	void initConnection() throws SQLException {
		connection = DriverManager.getConnection(url, username, password);
	}

	@AfterProperty
	void closeConnection() throws SQLException {
		connection.close();
	}

	@Example
	void createUsers() throws Exception {
		executeDDL(
				connection,
				"insert into users values('Kent Beck')",
				"insert into users values('Johannes Link')"
		);
		readAndPrintAllUsers();
	}

	@Example
	void createMoreUsers() throws Exception {
		executeDDL(
				connection,
				"insert into users values('Frank Dude')",
				"insert into users values('Hayah Kalaan')"
		);
		readAndPrintAllUsers();
	}

	private void readAndPrintAllUsers() throws SQLException {
		Statement statement = connection.createStatement();
		ResultSet resultSet = statement.executeQuery("select * from users");

		while (resultSet.next()) {
			System.out.println(resultSet.getString("name"));
		}
	}

	private static void executeDDL(Connection con, String... lines) {
		try {
			// enable transaction
			con.setAutoCommit(false);

			Statement statement = con.createStatement();

			// for every DDL statement, execute it
			for (String sql : lines) {
				statement.executeUpdate(sql);
			}

			statement.close();
			con.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
