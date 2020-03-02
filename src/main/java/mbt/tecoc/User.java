package mbt.tecoc;

import java.sql.*;
import java.time.*;

public class User {

	static User fromResultSet(ResultSet resultSet) {
		try {
			int id = resultSet.getInt("id");
			String userName = resultSet.getString("name");
			String userEmail = resultSet.getString("email");
			Instant userCreatedAt = resultSet.getTimestamp("created_at").toInstant();
			return new User(id, userName, userEmail, userCreatedAt);
		} catch (SQLException sqlException) {
			throw new RuntimeException(sqlException);
		}
	}

	private int id;
	private String name;
	private String email;
	private Instant createdAt;

	public User(String name, String email) {
		this(0, name, email, null);
	}

	private User(int id, String name, String email, Instant createdAt) {
		this.id = id;
		this.name = name;
		this.email = email;
		this.createdAt = createdAt;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	@Override
	public String toString() {
		return String.format("User{id=%d, name='%s', email='%s', createdAt=%s}", id, name, email, createdAt);
	}

	public void setId(int id) {
		this.id = id;
	}
}
