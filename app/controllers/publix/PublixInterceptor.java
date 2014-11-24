package controllers.publix;

import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;

import com.fasterxml.jackson.core.JsonProcessingException;

import exceptions.PublixException;

/**
 * Interceptor for Publix: it intercepts requests for JATOS' public API and
 * forwards them to one of the implementations of the API. Right now two
 * implementations exists: MTPublix for studies originating from MTurk and
 * MAPublix for studies and components started from within JATOS' UI.
 * 
 * TODO: Move @Transactional out of controller and get rid of synchronisation
 * and JPA transaction handling
 * 
 * @author Kristian Lange
 */
public class PublixInterceptor extends Controller implements IPublix {

	private IPublix maPublix = new MAPublix();
	private IPublix mtPublix = new MTPublix();

	private static Object lock = new Object();

	@Override
	@Transactional
	public Result startStudy(Long studyId) throws PublixException {
		synchronized (lock) {
			Result result;
			if (isFromJatos()) {
				result = maPublix.startStudy(studyId);
			} else {
				result = mtPublix.startStudy(studyId);
			}
			JPA.em().flush();
			JPA.em().getTransaction().commit();
			JPA.em().getTransaction().begin();
			return result;
		}
	}

	@Override
	@Transactional
	public Promise<Result> startComponent(Long studyId, Long componentId)
			throws PublixException {
		synchronized (lock) {
			Promise<Result> result;
			if (isFromJatos()) {
				result = maPublix.startComponent(studyId, componentId);
			} else {
				result = mtPublix.startComponent(studyId, componentId);
			}
			JPA.em().flush();
			JPA.em().getTransaction().commit();
			JPA.em().getTransaction().begin();
			return result;
		}
	}

	@Override
	@Transactional
	public Promise<Result> startComponentByPosition(Long studyId,
			Integer position) throws PublixException {
		// This method calls startComponent(). Therefore no synchronisation
		// and JPA transaction handling
		Promise<Result> result;
		if (isFromJatos()) {
			result = maPublix.startComponentByPosition(studyId, position);
		} else {
			result = mtPublix.startComponentByPosition(studyId, position);
		}
		return result;
	}

	@Override
	@Transactional
	public Result startNextComponent(Long studyId) throws PublixException {
		synchronized (lock) {
			Result result;
			if (isFromJatos()) {
				result = maPublix.startNextComponent(studyId);
			} else {
				result = mtPublix.startNextComponent(studyId);
			}
			JPA.em().flush();
			JPA.em().getTransaction().commit();
			JPA.em().getTransaction().begin();
			return result;
		}
	}

	@Override
	@Transactional
	public Result getStudyData(Long studyId) throws PublixException,
			JsonProcessingException {
		synchronized (lock) {
			Result result;
			if (isFromJatos()) {
				result = maPublix.getStudyData(studyId);
			} else {
				result = mtPublix.getStudyData(studyId);
			}
			JPA.em().flush();
			JPA.em().getTransaction().commit();
			JPA.em().getTransaction().begin();
			return result;
		}
	}

	@Override
	@Transactional
	public Result getComponentData(Long studyId, Long componentId)
			throws PublixException, JsonProcessingException {
		synchronized (lock) {
			Result result;
			if (isFromJatos()) {
				result = maPublix.getComponentData(studyId, componentId);
			} else {
				result = mtPublix.getComponentData(studyId, componentId);
			}
			JPA.em().flush();
			JPA.em().getTransaction().commit();
			JPA.em().getTransaction().begin();
			return result;
		}
	}

	@Override
	@Transactional
	public Result submitResultData(Long studyId, Long componentId)
			throws PublixException {
		synchronized (lock) {
			Result result;
			if (isFromJatos()) {
				result = maPublix.submitResultData(studyId, componentId);
			} else {
				result = mtPublix.submitResultData(studyId, componentId);
			}
			JPA.em().flush();
			JPA.em().getTransaction().commit();
			JPA.em().getTransaction().begin();
			return result;
		}
	}

	@Override
	@Transactional
	public Result finishComponent(Long studyId, Long componentId,
			Boolean successful, String errorMsg) throws PublixException {
		synchronized (lock) {
			Result result;
			if (isFromJatos()) {
				result = maPublix.finishComponent(studyId, componentId,
						successful, errorMsg);
			} else {
				result = mtPublix.finishComponent(studyId, componentId,
						successful, errorMsg);
			}
			JPA.em().flush();
			JPA.em().getTransaction().commit();
			JPA.em().getTransaction().begin();
			return result;
		}
	}
	
	@Override
	@Transactional
	public Result abortStudy(Long studyId, String message)
			throws PublixException {
		synchronized (lock) {
			Result result;
			if (isFromJatos()) {
				result = maPublix.abortStudy(studyId, message);
			} else {
				result = mtPublix.abortStudy(studyId, message);
			}
			JPA.em().flush();
			JPA.em().getTransaction().commit();
			JPA.em().getTransaction().begin();
			return result;
		}
	}

	@Override
	@Transactional
	public Result finishStudy(Long studyId, Boolean successful, String errorMsg)
			throws PublixException {
		synchronized (lock) {
			Result result;
			if (isFromJatos()) {
				result = maPublix.finishStudy(studyId, successful, errorMsg);
			} else {
				result = mtPublix.finishStudy(studyId, successful, errorMsg);
			}
			JPA.em().flush();
			JPA.em().getTransaction().commit();
			JPA.em().getTransaction().begin();
			return result;
		}
	}

	@Override
	public Result logError(Long studyId, Long componentId) {
		if (isFromJatos()) {
			return maPublix.logError(studyId, componentId);
		} else {
			return mtPublix.logError(studyId, componentId);
		}
	}

	@Override
	public Result teapot() {
		if (isFromJatos()) {
			return maPublix.teapot();
		} else {
			return mtPublix.teapot();
		}
	}

	/**
	 * Check if this request originates from within JATOS.
	 */
	private boolean isFromJatos() {
		if (session(MAPublix.JATOS_SHOW) != null) {
			return true;
		}
		return false;
	}

}
