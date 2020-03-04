package mbt.tecoc;

import java.util.*;

public class PersistenceModel {

	private final List<User> users = new ArrayList<>();
	private final List<Post> posts = new ArrayList<>();

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

	public User userByIndex(int index) {
		int wrapAroundIndex = index % (users.size());
		return users.get(wrapAroundIndex);
	}

	public void addPost(int postId, Post newPost) {
		newPost.setId(postId);
		posts.add(newPost);
	}

	public int countPosts() {
		return posts.size();
	}

	public Optional<Post> readPost(int postId) {
		return posts.stream()
					.filter(post -> post.getId() == postId)
					.findFirst();
	}

	public void removeUser(int userId) {
		users.removeIf(user -> user.getId() == userId);
	}

	public boolean hasNoPosts(int userId) {
		return posts.stream().noneMatch(post -> post.getUserId() == userId);
	}

	public List<User> users() {
		return users;
	}
}
