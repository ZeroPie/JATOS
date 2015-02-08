package controllers.publix.mt;

import models.StudyModel;
import models.workers.MTSandboxWorker;
import models.workers.MTWorker;
import models.workers.Worker;
import utils.PersistanceUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import controllers.publix.PublixErrorMessages;
import controllers.publix.PublixUtils;
import daos.ComponentDao;
import daos.ComponentResultDao;
import daos.StudyDao;
import daos.StudyResultDao;
import daos.workers.WorkerDao;
import exceptions.ForbiddenPublixException;
import exceptions.PublixException;

/**
 * Special PublixUtils for MTPublix (studies started via MTurk).
 * 
 * @author Kristian Lange
 */
@Singleton
public class MTPublixUtils extends PublixUtils<MTWorker> {

	private MTErrorMessages errorMessages;

	@Inject
	public MTPublixUtils(MTErrorMessages errorMessages,
			PersistanceUtils persistanceUtils, StudyDao studyDao,
			ComponentDao componentDao, ComponentResultDao componentResultDao,
			StudyResultDao studyResultDao, WorkerDao workerDao) {
		super(errorMessages, persistanceUtils, studyDao, componentDao,
				componentResultDao, studyResultDao, workerDao);
		this.errorMessages = errorMessages;
	}

	@Override
	public MTWorker retrieveTypedWorker(String workerIdStr)
			throws PublixException {
		Worker worker = retrieveWorker(workerIdStr);
		if (!(worker instanceof MTWorker)) {
			throw new ForbiddenPublixException(
					errorMessages.workerNotCorrectType(worker.getId()));
		}
		return (MTWorker) worker;
	}

	@Override
	public void checkWorkerAllowedToStartStudy(MTWorker worker, StudyModel study)
			throws ForbiddenPublixException {
		if (!(worker instanceof MTSandboxWorker)
				&& didStudyAlready(worker, study)) {
			throw new ForbiddenPublixException(
					PublixErrorMessages.STUDY_CAN_BE_DONE_ONLY_ONCE);
		}
		checkWorkerAllowedToDoStudy(worker, study);
	}

	@Override
	public void checkWorkerAllowedToDoStudy(MTWorker worker, StudyModel study)
			throws ForbiddenPublixException {
		if (!study.hasAllowedWorker(worker.getWorkerType())) {
			throw new ForbiddenPublixException(
					errorMessages.workerTypeNotAllowed(worker.getUIWorkerType()));
		}
		// Sandbox workers can repeat studies
		if (worker instanceof MTSandboxWorker) {
			return;
		}
		// MTurk workers can't repeat studies
		if (finishedStudyAlready(worker, study)) {
			throw new ForbiddenPublixException(
					PublixErrorMessages.STUDY_CAN_BE_DONE_ONLY_ONCE);
		}
	}

}
