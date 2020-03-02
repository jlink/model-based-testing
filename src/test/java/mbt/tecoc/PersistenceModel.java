package mbt.tecoc;

import java.util.*;

public class PersistenceModel {

	private final Map<Integer, User> users = new HashMap<>();

	public void addUser(int userId, User newUser) {
		newUser.setId(userId);
		users.put(userId, newUser);
	}

	public int countUsers() {
		return users.size();
	}

	public Optional<User> readUser(int userId) {
		return Optional.ofNullable(users.get(userId));
	}

}
