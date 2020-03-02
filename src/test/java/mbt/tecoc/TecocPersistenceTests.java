package mbt.tecoc;

import java.sql.*;
import java.util.*;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.*;

import static org.assertj.core.api.Assertions.*;

@Group
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
		persistence.reset();
		persistence.close();
	}

	@Group
	class Users {
		@Example
		void newPersistenceHasNoUsers() {
			assertThat(persistence.countUsers()).isZero();
		}

		@Example
		void createNewUser() {
			User newUser = new User("Johannes", "jl@johanneslink.net");
			int newId = persistence.createUser(newUser);
			assertThat(newId).isNotEqualTo(0);

			assertThat(persistence.countUsers()).isEqualTo(1);

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

	@Group
	class Posts {

		@Example
		void newPersistenceHasNoPosts() {
			assertThat(persistence.countPosts()).isZero();
		}

		@Example
		void createNewPost() {
			User user = new User("Johannes", "jl@johanneslink.net");
			int userId = persistence.createUser(user);

			Post newPost = new Post(userId, "A Title", "this is a body");
			int newId = persistence.createPost(newPost);
			assertThat(newId).isNotEqualTo(0);

			assertThat(persistence.countPosts()).isEqualTo(1);

			Optional<Post> readPost = persistence.readPost(newId);
			assertThat(readPost).isPresent();
			readPost.ifPresent(post -> {
				assertThat(post.getUserId()).isEqualTo(userId);
				assertThat(post.getTitle()).isEqualTo("A Title");
				assertThat(post.getBody()).isEqualTo("this is a body");
			});
		}

		@Example
		void deletePost() {
			User user = new User("Johannes", "jl@johanneslink.net");
			int userId = persistence.createUser(user);

			Post newPost = new Post(userId, "A Title", "this is a body");
			int postId = persistence.createPost(newPost);

			assertThat(persistence.deletePost(postId)).isTrue();
			assertThat(persistence.readPost(postId)).isNotPresent();
		}

	}
}
