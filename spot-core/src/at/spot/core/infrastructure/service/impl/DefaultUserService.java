package at.spot.core.infrastructure.service.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import at.spot.core.infrastructure.exception.DuplicateUserException;
import at.spot.core.infrastructure.exception.ModelSaveException;
import at.spot.core.infrastructure.exception.ModelValidationException;
import at.spot.core.infrastructure.service.ModelService;
import at.spot.core.infrastructure.service.SessionService;
import at.spot.core.infrastructure.service.UserService;
import at.spot.core.infrastructure.support.Session;
import at.spot.core.model.ItemTypeConstants;
import at.spot.core.model.user.PrincipalGroup;
import at.spot.core.model.user.User;
import at.spot.core.model.user.UserGroup;
import at.spot.core.persistence.exception.ModelNotUniqueException;

@Service
public class DefaultUserService<U extends User, G extends UserGroup> extends AbstractService
		implements UserService<U, G> {

	@Autowired
	protected ModelService modelService;

	@Autowired
	protected SessionService sessionService;

	@Override
	public U createUser(final Class<U> type, final String userId) throws DuplicateUserException {
		final U user = modelService.create(type);
		user.setId(userId);

		try {
			modelService.save(user);
		} catch (ModelSaveException | ModelNotUniqueException | ModelValidationException e) {
			throw new DuplicateUserException();
		}

		return user;
	}

	@Override
	public U getUser(final String uid) {
		final Map<String, Comparable<?>> params = new HashMap<>();
		params.put("uid", uid);

		return modelService.get(getUserType(), params);
	}

	@Override
	public G getUserGroup(final String uid) {
		final Map<String, Comparable<?>> params = new HashMap<>();
		params.put("uid", uid);

		return modelService.get(getUserGroupType(), params);
	}

	@Override
	public boolean isUserInGroup(final String userUid, final String groupUid) {
		return false;
	}

	@Override
	public Set<G> getAllGroupsOfUser(final String uid) {
		final Set<G> groups = new HashSet<>();

		for (final PrincipalGroup g : getUser(uid).getGroups()) {
			if (g instanceof UserGroup) {
				groups.add((G) g);
			}
		}

		return groups;
	}

	@Override
	public List<U> getAllUsers() {
		return modelService.getAll(getUserType());
	}

	@Override
	public List<G> getAllUserGroups() {
		return modelService.getAll(getUserGroupType());
	}

	protected Class<U> getUserType() {
		final Class<U> userType = (Class<U>) getApplicationContext().getBean(ItemTypeConstants.USER).getClass();
		return userType;
	}

	protected Class<G> getUserGroupType() {
		final Class<G> userGroupType = (Class<G>) getApplicationContext().getBean(ItemTypeConstants.USER_GROUP)
				.getClass();

		return userGroupType;
	}

	@Override
	public void setCurrentUser(U user) {
		Session session = sessionService.getCurrentSession();

		if (user != null) {
			// session.setAttribute(CoreConstants.SESSION_KEY_CURRENT_USER,
			// user);
			session.user(user);
		} else {
			loggingService.warn("Cannot set a null user as current session user.");
		}
	}

	@Override
	public U getCurrentUser() {
		Session session = sessionService.getCurrentSession();

		if (session != null) {
			// Object currentUserAttr =
			// session.getAttribute(CoreConstants.SESSION_KEY_CURRENT_USER);

			// if (currentUserAttr != null && currentUserAttr instanceof User) {
			// return (U) currentUserAttr;
			// }
			return (U) session.user();
		} else {
			loggingService.warn("No session is set up.");
		}

		return null;
	}
}
