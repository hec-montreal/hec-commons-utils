package ca.hec.commons.observers;

import org.sakaiproject.event.api.Event;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.event.api.UsageSessionService;
import org.sakaiproject.tool.api.SessionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

public class HecEventsObserver {
	private static final Log log = LogFactory.getLog(HecEventsObserver.class);
	
	EventTrackingService eventTrackingService;
	SessionManager sessionManager;
	UsageSessionService usageSessionService;
	
	public void setEventTrackingService(EventTrackingService ets) {
		eventTrackingService = ets;
	}
	public void setSessionManager(SessionManager sm) {
		sessionManager = sm;
	}
	public void setUsageSessionService(UsageSessionService uss) {
		usageSessionService = uss;
	}
	
	public void init() {
		eventTrackingService.addObserver(new Observer() {
			public void update(Observable o, Object arg) {
				Event e = (Event) arg;
				if (e.getEvent().equals(usageSessionService.EVENT_LOGIN)) {
					// ##### LOGIN ####//
					DateFormat df =
							new SimpleDateFormat("dd/MM/yyyy 'at' HH:mm");
					
					String userEid = sessionManager.getCurrentSession().getUserEid();
					if (userEid != null) {
						log.info("user ["
								+ sessionManager.getCurrentSession().getUserEid()
								+ "] login " + df.format(new Date()));
					}
				} 
			}
		});
	}
}