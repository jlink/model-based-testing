package mbt.tecoc;

import java.util.*;

import net.jqwik.api.Tuple.*;
import net.jqwik.api.stateful.*;

import static org.assertj.core.api.Assertions.*;

abstract class AbstractPersistenceAction implements Action<Tuple2<TecocPersistence, PersistenceModel>> {

	void compareReadUser(int userId, Tuple2<TecocPersistence, PersistenceModel> state) {
		Optional<User> optionalUser = state.get1().readUser(userId);
		Optional<User> optionalUserFromModel = state.get2().readUser(userId);
		assertThat(optionalUser.isPresent()).isEqualTo(optionalUserFromModel.isPresent());
		optionalUser.ifPresent(user -> {
			optionalUserFromModel.ifPresent(userFromModel -> {
				assertThat(user.getName()).isEqualTo(userFromModel.getName());
				assertThat(user.getEmail()).isEqualTo(userFromModel.getEmail());
			});
		});
	}

	void compareReadPost(int postId, Tuple2<TecocPersistence, PersistenceModel> state) {
		Optional<Post> optionalPost = state.get1().readPost(postId);
		Optional<Post> optionalPostFromModel = state.get2().readPost(postId);
		assertThat(optionalPost.isPresent()).isEqualTo(optionalPostFromModel.isPresent());
		optionalPost.ifPresent(post -> {
			optionalPostFromModel.ifPresent(postFromModel -> {
				assertThat(post.getTitle()).isEqualTo(postFromModel.getTitle());
				assertThat(post.getBody()).isEqualTo(postFromModel.getBody());
			});
		});
	}

	void compareCounts(Tuple2<TecocPersistence, PersistenceModel> state) {
		assertThat(state.get1().countUsers()).isEqualTo(state.get2().countUsers());
		assertThat(state.get1().countPosts()).isEqualTo(state.get2().countPosts());
	}
}
