package mbt.tecoc;

import java.util.*;

public class PersistenceModel {

	private final List<User> users = new ArrayList<>();

	public void addUser(int userId, User newUser) {
		newUser.setId(userId);
		users.add(newUser);
	}

	public int countUsers() {
		return users.size();
	}

	public Optional<User> readUser(int userId) {
		return users.stream()
					.filter(user -> user.getId() == userId)
					.findFirst();
	}

}
