package mbt.tecoc;

import java.sql.*;
import java.util.*;

public class TecocPersistence implements AutoCloseable {
	private Connection connection;

	public TecocPersistence(Connection connection) {
		this.connection = connection;
	}

	public void initialize() {
		executeDDL(
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

	private void executeDDL(String... statements) {
		try {
			connection.setAutoCommit(false);

			Statement statement = connection.createStatement();
			for (String sql : statements) {
				statement.executeUpdate(sql);
			}
			statement.close();
			connection.commit();
		} catch (SQLException sqlException) {
			throw new RuntimeException(sqlException);
		}
	}

	public void close() throws SQLException {
		connection.close();
	}

	public List<User> findAllUsers() {
		try {
			ArrayList<User> users = new ArrayList<>();
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery("select * from users");
			while (resultSet.next()) {
				users.add(User.fromResultSet(resultSet));
			}
			return users;
		} catch (SQLException sqlException) {
			throw new RuntimeException(sqlException);
		}
	}

	public int createUser(User newUser) {
		try {
			PreparedStatement statement = connection.prepareStatement(
					"insert into users(name, email) values(?, ?)",
					Statement.RETURN_GENERATED_KEYS
			);
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
		} catch (SQLException sqlException) {
			throw new RuntimeException(sqlException);
		}
	}

	public Optional<User> readUser(int newId) {
		try {
			PreparedStatement statement = connection.prepareStatement(
					"select * from users where id=?"
			);
			statement.setInt(1, newId);
			ResultSet resultSet = statement.executeQuery();
			if (!resultSet.next()) {
				return Optional.empty();
			}
			return Optional.of(User.fromResultSet(resultSet));
		} catch (SQLException sqlException) {
			throw new RuntimeException(sqlException);
		}
	}
}
