package mbt.tecoc;

import java.util.*;

import net.jqwik.api.Tuple.*;
import net.jqwik.api.stateful.*;

import static org.assertj.core.api.Assertions.*;

class CreateNewUserAction implements Action<Tuple2<TecocPersistence, PersistenceModel>> {

	private final String userName;
	private final String userEmail;

	CreateNewUserAction(String userName, String userEmail) {
		this.userName = userName;
		this.userEmail = userEmail;
	}

	@Override
	public Tuple2<TecocPersistence, PersistenceModel> run(Tuple2<TecocPersistence, PersistenceModel> state) {
		User newUser = new User(userName, userEmail);
		int newId = state.get1().createUser(newUser);
		assertThat(newId).isNotZero();
		state.get2().addUser(newId, newUser);

		compareReadUser(newId, state);
		compareCounts(state);

		return state;
	}

	private void compareReadUser(int userId, Tuple2<TecocPersistence, PersistenceModel> state) {
		Optional<User> user = state.get1().readUser(userId);
		Optional<User> userFromModel = state.get2().readUser(userId);
		assertThat(user.isPresent()).isEqualTo(userFromModel.isPresent());
	}

	private void compareCounts(Tuple2<TecocPersistence, PersistenceModel> state) {
		assertThat(state.get1().countUsers()).isEqualTo(state.get2().countUsers());
	}

}
