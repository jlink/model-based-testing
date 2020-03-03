package mbt.tecoc;

import net.jqwik.api.Tuple.*;

import static org.assertj.core.api.Assertions.*;

class CreatePostAction extends AbstractPersistenceAction {

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

	@Override
	public String toString() {
		return String.format("create-post[userIndex=%d, %s, %s]", userIndex, title, body);
	}
}
