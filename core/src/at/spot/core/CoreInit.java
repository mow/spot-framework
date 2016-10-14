package at.spot.core;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;

import at.spot.core.data.model.user.User;
import at.spot.core.data.model.user.UserGroup;
import at.spot.core.infrastructure.annotation.logging.Log;
import at.spot.core.infrastructure.service.LoggingService;
import at.spot.core.infrastructure.service.ModelService;
import at.spot.core.infrastructure.service.TypeService;
import at.spot.core.persistence.service.PersistenceService;

/**
 * This is the main entry point for the application. After the application has
 * been initialized, it will call {@link CoreInit#run()}. Then the shell is
 * being loaded.
 */
@Service
public class CoreInit {

	@Autowired
	protected TypeService typeService;

	@Autowired
	protected ModelService modelService;

	@Autowired
	protected LoggingService loggingService;

	@Autowired
	protected PersistenceService persistenceService;

	public static void main(String[] args) throws Exception {
		new ClassPathXmlApplicationContext("spring.xml").close();
	}

	public void run() {
		long start = System.currentTimeMillis();

		try {

			List<User> users = new ArrayList<>();

			UserGroup group = modelService.create(UserGroup.class);
			group.name = "tester group";
			group.uid = "test-group";

			User user1 = modelService.create(User.class);
			User user2 = modelService.create(User.class);

			user1.uid = "user1";
			user2.uid = "user3";

			user1.groups.add(group);
			user2.groups.add(group);

			for (int i = 1; i < 10000; i++) {
				if (i > 0 && i % 50 == 0) {
					long duration = System.currentTimeMillis() - start;

					if (duration >= 1000) {
						loggingService.debug("Created " + i + " users (" + i / (duration / 1000) + " items/s )");
					}
				}

				User user = modelService.create(User.class);
				user.name = "test-" + i;
				user.uid = user.name;

				user.groups.add(group);

				users.add(user);
			}

			// modelService.saveAll(user1, user2);
			//
			// // user1 = modelService.get(User.class, user1.pk);
			// user2 = modelService.get(User.class, user2.pk);
			//
			// user1.groups.get(0).uid = "abc";
			//
			// System.out.println(user2.groups.get(0).uid);
			//
			// modelService.save(user1);
			//
			// System.out.println(user2.groups.get(0).uid);

			// modelService.refresh(user2);

			// System.out.println(user2.groups.get(0).uid);

			modelService.saveAll(users);

		} catch (Exception e) {
			loggingService.exception(e.getMessage());
		}

		// try {
		// User loadedUser = modelService.get(User.class, 0l);
		// loggingService.info("loaded user again");
		// } catch (ModelNotFoundException e) {
		// loggingService.exception(e.getMessage());
		// }

		persistenceService.saveDataStorage();

	}

	/*
	 * STARTUP FUNCTIONALITY
	 */

	public void initSpring() {

	}

	@Log(message = "Starting spOt core ...")
	@PostConstruct
	public void init() throws Exception {
		setupTypeInfrastrucutre();

		run();
	}

	@Log(message = "Setting up type registry ...")
	protected void setupTypeInfrastrucutre() {
		typeService.registerTypes();
		persistenceService.initDataStorage();
	}
}
