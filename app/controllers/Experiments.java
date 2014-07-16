package controllers;

import java.util.List;
import java.util.Map;

import models.MAComponent;
import models.MAExperiment;
import models.MAUser;
import play.data.DynamicForm;
import play.data.Form;
import play.db.jpa.Transactional;
import play.mvc.Result;
import play.mvc.Security;

public class Experiments extends MAController {

	public static final String USER = "user";
	public static final String TITLE = "title";
	public static final String DESCRIPTION = "description";
	public static final String AN_EXPERIMENT_SHOULD_HAVE_AT_LEAST_ONE_MEMBER = "An experiment should have at least one member.";

	@Transactional
	@Security.Authenticated(Secured.class)
	public static Result index(Long experimentId) {
		MAExperiment experiment = MAExperiment.findById(experimentId);
		MAUser user = MAUser.findByEmail(session(MAController.COOKIE_EMAIL));
		List<MAExperiment> experimentList = MAExperiment.findAll();
		Result result = checkStandard(experiment, experimentId, user,
				experimentList);
		if (result != null) {
			return result;
		}

		return ok(views.html.admin.experiment.index.render(experimentList,
				user, null, experiment));
	}

	@Transactional
	@Security.Authenticated(Secured.class)
	public static Result create() {
		List<MAExperiment> experimentList = MAExperiment.findAll();
		MAUser user = MAUser.findByEmail(session(MAController.COOKIE_EMAIL));
		return ok(views.html.admin.experiment.create.render(experimentList,
				user, Form.form(MAExperiment.class)));
	}

	@Transactional
	@Security.Authenticated(Secured.class)
	public static Result submit() {
		Form<MAExperiment> form = Form.form(MAExperiment.class)
				.bindFromRequest();
		MAUser user = MAUser.findByEmail(session(MAController.COOKIE_EMAIL));
		if (form.hasErrors()) {
			List<MAExperiment> experimentList = MAExperiment.findAll();
			return badRequest(views.html.admin.experiment.create.render(
					experimentList, user, form));
		} else {
			MAExperiment experiment = form.get();
			experiment.addMember(user);
			experiment.persist();
			return redirect(routes.Experiments.index(experiment.id));
		}
	}

	@Transactional
	@Security.Authenticated(Secured.class)
	public static Result update(Long experimentId) {
		MAExperiment experiment = MAExperiment.findById(experimentId);
		MAUser user = MAUser.findByEmail(session(MAController.COOKIE_EMAIL));
		List<MAExperiment> experimentList = MAExperiment.findAll();
		Result result = checkStandard(experiment, experimentId, user,
				experimentList);
		if (result != null) {
			return result;
		}

		Form<MAExperiment> form = Form.form(MAExperiment.class)
				.fill(experiment);
		return ok(views.html.admin.experiment.update.render(experimentList,
				experiment, user, form));
	}

	@Transactional
	@Security.Authenticated(Secured.class)
	public static Result submitUpdated(Long experimentId) {
		MAExperiment experiment = MAExperiment.findById(experimentId);
		MAUser user = MAUser.findByEmail(session(MAController.COOKIE_EMAIL));
		List<MAExperiment> experimentList = MAExperiment.findAll();
		Result result = checkStandard(experiment, experimentId, user,
				experimentList);
		if (result != null) {
			return result;
		}

		Form<MAExperiment> form = Form.form(MAExperiment.class)
				.bindFromRequest();
		if (form.hasErrors()) {
			return badRequest(views.html.admin.experiment.update.render(
					experimentList, experiment, user, form));
		}

		// Update experiment in DB
		DynamicForm requestData = Form.form().bindFromRequest();
		String title = requestData.get(TITLE);
		String description = requestData.get(DESCRIPTION);
		experiment.update(title, description);
		experiment.merge();
		return redirect(routes.Experiments.index(experimentId));
	}

	@Transactional
	@Security.Authenticated(Secured.class)
	public static Result remove(Long experimentId) {
		MAExperiment experiment = MAExperiment.findById(experimentId);
		MAUser user = MAUser.findByEmail(session(MAController.COOKIE_EMAIL));
		List<MAExperiment> experimentList = MAExperiment.findAll();
		Result result = checkStandard(experiment, experimentId, user,
				experimentList);
		if (result != null) {
			return result;
		}

		experiment.remove();
		return redirect(routes.Admin.index());
	}

	@Transactional
	@Security.Authenticated(Secured.class)
	public static Result updateMembers(Long experimentId) {
		MAExperiment experiment = MAExperiment.findById(experimentId);
		MAUser user = MAUser.findByEmail(session(MAController.COOKIE_EMAIL));
		List<MAExperiment> experimentList = MAExperiment.findAll();
		Result result = checkStandard(experiment, experimentId, user,
				experimentList);
		if (result != null) {
			return result;
		}

		List<MAUser> userList = MAUser.findAll();
		return ok(views.html.admin.experiment.updateMembers.render(
				experimentList, experiment, userList, user, null));
	}

	@Transactional
	@Security.Authenticated(Secured.class)
	public static Result submitUpdatedMembers(Long experimentId) {
		MAExperiment experiment = MAExperiment.findById(experimentId);
		MAUser loggedInUser = MAUser
				.findByEmail(session(MAController.COOKIE_EMAIL));
		List<MAExperiment> experimentList = MAExperiment.findAll();
		Result result = checkStandard(experiment, experimentId, loggedInUser,
				experimentList);
		if (result != null) {
			return result;
		}

		Map<String, String[]> formMap = request().body().asFormUrlEncoded();
		String[] checkedUsers = formMap.get(USER);
		if (checkedUsers == null || checkedUsers.length < 1) {
			String errorMsg = AN_EXPERIMENT_SHOULD_HAVE_AT_LEAST_ONE_MEMBER;
			List<MAUser> userList = MAUser.findAll();
			return badRequest(views.html.admin.experiment.updateMembers.render(
					experimentList, experiment, userList, loggedInUser,
					errorMsg));
		}
		experiment.memberList.clear();
		for (String email : checkedUsers) {
			MAUser user = MAUser.findByEmail(email);
			if (user != null) {
				experiment.addMember(user);
				experiment.merge();
			}
		}

		return redirect(routes.Experiments.index(experimentId));
	}

	/**
	 * Ajax POST request to change the oder of components within an experiment.
	 */
	@Transactional
	@Security.Authenticated(Secured.class)
	public static Result changeComponentOrder(Long experimentId,
			Long componentId, String direction) {
		MAExperiment experiment = MAExperiment.findById(experimentId);
		MAUser loggedInUser = MAUser
				.findByEmail(session(MAController.COOKIE_EMAIL));
		if (experiment == null) {
			return badRequest(experimentNotExist(experimentId));
		}
		if (!experiment.hasMember(loggedInUser)) {
			return forbidden(notMember(loggedInUser.name, loggedInUser.email,
					experiment.id, experiment.title));
		}

		MAComponent component = MAComponent.findById(componentId);
		if (component == null) {
			return badRequest(componentNotExist(componentId));
		}
		if (!experiment.hasComponent(component)) {
			badRequest(componentNotBelongToExperiment(experimentId, componentId));
		}

		if (direction.equals("up")) {
			experiment.componentOrderMinusOne(component);
		}
		if (direction.equals("down")) {
			experiment.componentOrderPlusOne(component);
		}
		experiment.refresh();

		return ok();
	}

	@Transactional
	@Security.Authenticated(Secured.class)
	public static Result showMTurkSourceCode(Long experimentId) {
		MAExperiment experiment = MAExperiment.findById(experimentId);
		MAUser user = MAUser.findByEmail(session(MAController.COOKIE_EMAIL));
		List<MAExperiment> experimentList = MAExperiment.findAll();
		Result result = checkStandard(experiment, experimentId, user,
				experimentList);
		if (result != null) {
			return result;
		}

		String hostname = request().host();
		return ok(views.html.admin.experiment.mTurkSourceCode.render(
				experimentList, user, null, experiment, hostname));
	}

	private static Result checkStandard(MAExperiment experiment,
			Long experimentId, MAUser user, List<MAExperiment> experimentList) {
		if (experiment == null) {
			return badRequestExperimentNotExist(experimentId, user,
					experimentList);
		}
		if (!experiment.hasMember(user)) {
			return forbiddenNotMember(user, experiment, experimentList);
		}
		return null;
	}

}
