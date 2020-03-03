package mbt.tecoc;

import java.sql.*;
import java.time.*;

public class Post {

	static Post fromResultSet(ResultSet resultSet) {
		try {
			int id = resultSet.getInt("id");
			int userId = resultSet.getInt("user_id");
			String title = resultSet.getString("title");
			String body = resultSet.getString("body");
			Instant userCreatedAt = resultSet.getTimestamp("created_at").toInstant();
			return new Post(id, userId, title, body, userCreatedAt);
		} catch (SQLException sqlException) {
			throw new RuntimeException(sqlException);
		}
	}

	private int id;
	private int userId;
	private String title;
	private String body;
	private Instant createdAt;

	public Post(int userId, String title, String body) {
		this(0, userId, title, body, null);
	}

	private Post(int id, int userId, String title, String body, Instant createdAt) {
		this.id = id;
		this.userId = userId;
		this.title = title;
		this.body = body;
		this.createdAt = createdAt;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getUserId() {
		return userId;
	}

	public String getTitle() {
		return title;
	}

	public String getBody() {
		return body;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	@Override
	public String toString() {
		return String.format(
				"Post{id=%d, userId=%d, title='%s', body='%s', createdAt=%s}",
				id,
				userId,
				title,
				body,
				createdAt
		);
	}
}
