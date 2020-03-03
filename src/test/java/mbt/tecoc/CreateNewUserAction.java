package mbt.tecoc;

import net.jqwik.api.Tuple.*;

import static org.assertj.core.api.Assertions.*;

class CreateNewUserAction extends AbstractPersistenceAction {

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

	@Override
	public String toString() {
		return String.format("create-new-user[%s, %s]", userName, userEmail);
	}
}
