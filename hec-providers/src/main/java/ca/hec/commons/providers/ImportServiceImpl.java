package ca.hec.commons.providers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.hec.tenjin.api.model.syllabus.Syllabus;

import ca.hec.tenjin.api.ImportService;

public class ImportServiceImpl implements ImportService {
	private Log log = LogFactory.getLog(ImportServiceImpl.class);
	
	public Syllabus importSyllabusFromSite(String siteId) {
		
		ca.hec.tenjin.api.model.syllabus.Syllabus syllabus = new Syllabus();
		syllabus.setTitle(siteId);
		return syllabus;
		
	}
}
