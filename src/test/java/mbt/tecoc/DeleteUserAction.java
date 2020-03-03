package mbt.tecoc;

import net.jqwik.api.Tuple.*;

class DeleteUserAction extends AbstractPersistenceAction {

	private final int userIndex;

	DeleteUserAction(int userIndex) {
		this.userIndex = userIndex;
	}

	@Override
	public boolean precondition(Tuple2<TecocPersistence, PersistenceModel> state) {
		PersistenceModel model = state.get2();
		if (model.countUsers() == 0) {
			return false;
		}
		int userId = model.userByIndex(userIndex).getId();
		return model.hasNoPosts(userId);
	}

	@Override
	public Tuple2<TecocPersistence, PersistenceModel> run(Tuple2<TecocPersistence, PersistenceModel> state) {
		int userId = state.get2().userByIndex(userIndex).getId();
		state.get1().deleteUser(userId);
		state.get2().removeUser(userId);

		compareCounts(state);

		return state;
	}

	@Override
	public String toString() {
		return String.format("delete-user[userIndex=%d]", userIndex);
	}
}
