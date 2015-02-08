package controllers.publix.closed_standalone;

import models.ComponentModel;
import models.StudyModel;
import models.workers.ClosedStandaloneWorker;
import play.Logger;
import play.mvc.Result;
import utils.JsonUtils;
import utils.PersistanceUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import controllers.publix.IPublix;
import controllers.publix.Publix;
import daos.ComponentResultDao;
import daos.StudyResultDao;
import exceptions.PublixException;

/**
 * Implementation of JATOS' public API for closed standalone study runs
 * (standalone runs with invitation and pre-created worker).
 * 
 * @author Kristian Lange
 */
@Singleton
public class ClosedStandalonePublix extends Publix<ClosedStandaloneWorker>
		implements IPublix {

	public static final String CLOSEDSTANDALONE_WORKER_ID = "closedStandaloneWorkerId";

	private static final String CLASS_NAME = ClosedStandalonePublix.class
			.getSimpleName();

	private final ClosedStandalonePublixUtils publixUtils;

	@Inject
	public ClosedStandalonePublix(ClosedStandaloneErrorMessages errorMessages,
			ClosedStandalonePublixUtils publixUtils,
			PersistanceUtils persistanceUtils,
			ComponentResultDao componentResultDao, JsonUtils jsonUtils,
			StudyResultDao studyResultDao) {
		super(publixUtils, persistanceUtils, componentResultDao, jsonUtils,
				studyResultDao);
		this.publixUtils = publixUtils;
	}

	@Override
	public Result startStudy(Long studyId) throws PublixException {
		String workerIdStr = getQueryString(CLOSEDSTANDALONE_WORKER_ID);
		Logger.info(CLASS_NAME + ".startStudy: studyId " + studyId + ", "
				+ CLOSEDSTANDALONE_WORKER_ID + " " + workerIdStr);
		StudyModel study = publixUtils.retrieveStudy(studyId);

		ClosedStandaloneWorker worker = publixUtils
				.retrieveTypedWorker(workerIdStr);
		publixUtils.checkWorkerAllowedToStartStudy(worker, study);
		session(WORKER_ID, workerIdStr);

		publixUtils.finishAllPriorStudyResults(worker, study);
		persistanceUtils.createStudyResult(study, worker);

		ComponentModel firstComponent = publixUtils
				.retrieveFirstActiveComponent(study);
		return redirect(controllers.publix.routes.PublixInterceptor
				.startComponent(studyId, firstComponent.getId()));
	}

}
