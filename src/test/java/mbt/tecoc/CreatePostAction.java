package mbt.tecoc;

import java.util.*;

import net.jqwik.api.Tuple.*;
import net.jqwik.api.stateful.*;

import static org.assertj.core.api.Assertions.*;

class CreatePostAction implements Action<Tuple2<TecocPersistence, PersistenceModel>> {

	private final int userIndex;
	private final String title;
	private final String body;

	CreatePostAction(int userIndex, String title, String body) {
		this.userIndex = userIndex;
		this.title = title;
		this.body = body;
	}

	@Override
	public boolean precondition(Tuple2<TecocPersistence, PersistenceModel> state) {
		return state.get2().countUsers() >= 1;
	}

	@Override
	public Tuple2<TecocPersistence, PersistenceModel> run(Tuple2<TecocPersistence, PersistenceModel> state) {
		int userId = state.get2().userByIndex(userIndex).getId();
		Post newPost = new Post(userId, title, body);
		int newId = state.get1().createPost(newPost);
		assertThat(newId).isNotZero();
		state.get2().addPost(newId, newPost);

		compareReadPost(newId, state);
		compareCounts(state);

		return state;
	}

	private void compareReadPost(int userId, Tuple2<TecocPersistence, PersistenceModel> state) {
		Optional<Post> optionalPost = state.get1().readPost(userId);
		Optional<Post> optionalPostFromModel = state.get2().readPost(userId);
		assertThat(optionalPost.isPresent()).isEqualTo(optionalPostFromModel.isPresent());
		optionalPost.ifPresent(post -> {
			optionalPostFromModel.ifPresent(postFromModel -> {
				assertThat(post.getTitle()).isEqualTo(postFromModel.getTitle());
				assertThat(post.getBody()).isEqualTo(postFromModel.getBody());
			});
		});
	}

	private void compareCounts(Tuple2<TecocPersistence, PersistenceModel> state) {
		assertThat(state.get1().countUsers()).isEqualTo(state.get2().countUsers());
		assertThat(state.get1().countPosts()).isEqualTo(state.get2().countPosts());
	}

	@Override
	public String toString() {
		return String.format("create-post[userIndex=%d, %s, %s]", userIndex, title, body);
	}
}
