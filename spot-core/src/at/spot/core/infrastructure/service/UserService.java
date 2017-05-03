package at.spot.core.infrastructure.service;

import java.util.List;
import java.util.Set;

import at.spot.core.infrastructure.exception.DuplicateUserException;
import at.spot.core.model.user.User;
import at.spot.core.model.user.UserGroup;

public interface UserService<U extends User, G extends UserGroup> {

	/**
	 * Creates an empty {@link User} object, only setting the given userId.
	 * 
	 * @param userId
	 * @return the newly created {@link User}.
	 * @throws DuplicateUserException
	 */
	U createUser(Class<U> type, String userId) throws DuplicateUserException;

	/**
	 * @param uid
	 *            the user's uid
	 * @return the given user or null, if the user is not found.
	 */
	U getUser(String uid);

	/**
	 * @return all available {@link User}s (even subtypes).
	 */
	List<U> getAllUsers();

	/**
	 * @return all available {@link UserGroup}s (even subtypes).
	 */
	List<G> getAllUserGroups();

	/**
	 * Get user group with the given id.
	 * 
	 * @param uid
	 * @return the {@link UserGroup}
	 */
	G getUserGroup(String uid);

	Set<G> getAllGroupsOfUser(String uid);

	boolean isUserInGroup(String userUid, String groupUid);

	/**
	 * Returns the current user in the session.
	 * 
	 * @return
	 */
	U getCurrentUser();

	/**
	 * Sets the given user as the current session user.
	 * 
	 * @param user
	 */
	void setCurrentUser(U user);
}
