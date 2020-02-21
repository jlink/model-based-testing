package mbt.tecoc;

import java.sql.*;
import java.util.*;

public class TecocPersistence implements AutoCloseable {

	private interface WithConnection<T> {
		T run(Connection connection) throws SQLException;
	}

	private interface WithStatement<T, S extends Statement> {
		T run(S statement) throws SQLException;
	}

	private Connection connection;

	public TecocPersistence(Connection connection) {
		this.connection = connection;
		try {
			this.connection.setAutoCommit(false);
		} catch (SQLException sqlException) {
			throw new RuntimeException(sqlException);
		}
	}

	public void initialize() {
		executeStatements(
				"CREATE TABLE IF NOT EXISTS users(" +
						"id SERIAL PRIMARY KEY, " +
						"name TEXT NOT NULL, " +
						"email TEXT NOT NULL, " +
						"created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP" +
						");",
				"CREATE TABLE IF NOT EXISTS posts(" +
						"id SERIAL PRIMARY KEY, " +
						"user_id INTEGER NOT NULL REFERENCES users(id), " +
						"title TEXT NOT NULL, " +
						"body TEXT NOT NULL, " +
						"created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP" +
						");"
		);
	}

	public void close() throws SQLException {
		connection.close();
	}

	public List<User> findAllUsers() {
		return useStatement(statement -> {
			ArrayList<User> users = new ArrayList<>();
			ResultSet resultSet = statement.executeQuery("select * from users");
			while (resultSet.next()) {
				users.add(User.fromResultSet(resultSet));
			}
			return users;
		});
	}

	public int createUser(User newUser) {
		return usePreparedStatement(
				"insert into users(name, email) values(?, ?)",
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
				"select * from users where id=?",
				statement -> {
					statement.setInt(1, userId);
					ResultSet resultSet = statement.executeQuery();
					if (!resultSet.next()) {
						return Optional.empty();
					}
					return Optional.of(User.fromResultSet(resultSet));
				}
		);
	}

	public boolean deleteUser(int userId) {
		return usePreparedStatement(
				"delete from users where id=?",
				statement -> {
					statement.setInt(1, userId);
					int count = statement.executeUpdate();
					return count > 0;
				}
		);
	}

	private <T> T useConnection(WithConnection<T> sqlCode) {
		try {
			T result = sqlCode.run(connection);
			connection.commit();
			return result;
		} catch (SQLException sqlException) {
			throw new RuntimeException(sqlException);
		}
	}

	private <T> T useStatement(WithStatement<T, Statement> sqlCode) {
		return useConnection(c -> {
			try (Statement statement = c.createStatement()) {
				return sqlCode.run(statement);
			}
		});
	}

	private <T> T usePreparedStatement(String sql, WithStatement<T, PreparedStatement> sqlCode) {
		return useConnection(c -> {
			try (PreparedStatement statement = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
				return sqlCode.run(statement);
			}
		});
	}

	private void executeStatements(String... statements) {
		useConnection(c -> {
			try (Statement statement = connection.createStatement()) {
				for (String sql : statements) {
					statement.executeUpdate(sql);
				}
			}
			return null;
		});
	}

}
