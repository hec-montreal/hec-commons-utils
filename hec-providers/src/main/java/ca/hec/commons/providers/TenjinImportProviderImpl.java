package ca.hec.commons.providers;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Collections;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

import ca.hec.tenjin.api.model.syllabus.AbstractSyllabusElement;
import ca.hec.tenjin.api.model.syllabus.Syllabus;
import ca.hec.tenjin.api.model.syllabus.SyllabusEvaluationElement;
import ca.hec.tenjin.api.model.syllabus.SyllabusExamElement;
import ca.hec.tenjin.api.model.syllabus.SyllabusLectureElement;
import ca.hec.tenjin.api.model.syllabus.SyllabusTutorialElement;
import ca.hec.tenjin.api.model.syllabus.SyllabusCitationElement;
import ca.hec.tenjin.api.model.syllabus.SyllabusCompositeElement;
import ca.hec.tenjin.api.model.syllabus.SyllabusContactInfoElement;
import ca.hec.tenjin.api.model.syllabus.SyllabusDocumentElement;
import ca.hec.tenjin.api.model.syllabus.SyllabusHyperlinkElement;
import ca.hec.tenjin.api.model.syllabus.SyllabusSakaiToolElement;
import ca.hec.tenjin.api.model.syllabus.SyllabusTextElement;
import ca.hec.tenjin.api.model.syllabus.SyllabusRubricElement;
import lombok.Setter;
import ca.hec.tenjin.api.provider.TenjinImportProvider;
import ca.hec.tenjin.api.TemplateService;
import ca.hec.tenjin.api.exception.DeniedAccessException;

import org.sakaiquebec.opensyllabus.common.api.OsylSiteService;
import org.sakaiquebec.opensyllabus.common.model.COModeledServer;
import org.sakaiquebec.opensyllabus.shared.model.COContentResourceProxy;
import org.sakaiquebec.opensyllabus.shared.model.COElementAbstract;
import org.sakaiquebec.opensyllabus.shared.model.COModelInterface;
import org.sakaiquebec.opensyllabus.shared.model.COPropertiesType;
import org.sakaiquebec.opensyllabus.shared.model.COUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.citation.api.Citation;
import org.sakaiproject.citation.api.CitationCollection;
import org.sakaiproject.citation.api.CitationService;
import org.sakaiproject.content.api.ContentCollection;
import org.sakaiproject.content.api.ContentCollectionEdit;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.content.api.ContentResourceEdit;
import org.sakaiproject.content.api.ResourceType;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.event.api.NotificationService;
import org.sakaiproject.exception.IdInvalidException;
import org.sakaiproject.exception.IdLengthException;
import org.sakaiproject.exception.IdUniquenessException;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.exception.InUseException;
import org.sakaiproject.exception.InconsistentException;
import org.sakaiproject.exception.OverQuotaException;

public class TenjinImportProviderImpl implements TenjinImportProvider {
	private static Logger log = LoggerFactory.getLogger(TenjinImportProviderImpl.class);
	
	@Setter
	ContentHostingService contentService;
	@Setter
	CitationService citationService;
	@Setter
	OsylSiteService osylSiteService;
	@Setter
	TemplateService templateService;
	
	// Maps for rubric keyword => title 
    private static final Map<String, String> rubricMap_fr;
    private static final Map<String, String> rubricMap_en;
    private static final Map<String, String> rubricMap_es;
    
    private static final Map<String, String> citationTypes;
    private static final Map<String, String> documentTypes;
    private static final Map<String, String> hyperlinkTypes;
    private static final Map<String, String> contactTitles;
    
    static {
        Map<String, String> aMap = new HashMap<String, String>();
        aMap.put("description", "Description");
        aMap.put("objectives", "Objectifs");
        aMap.put("learningstrat", "Approche p�dagogique");
        aMap.put("coordinators", "Coordonnateur");
        aMap.put("lecturers", "Enseignant(s)");
        aMap.put("teachingassistants", "Stagiaire(s) d'enseignement");
        aMap.put("speakers", "Conf�rencier(s)");
        aMap.put("secretaries", "Secr�taire(s)");
        aMap.put("bibliographicres", "Ressources bibliographiques");
        aMap.put("misresources", "Ressources g�n�rales");
        aMap.put("complbibres", "Ressources bibliographiques compl�mentaires");
        aMap.put("case", "Cas");
        aMap.put("tools", "Outils");
        aMap.put("pastexams", "Anciens examens");
        aMap.put("evalcriteria", "Crit�res d'�valuation");
        aMap.put("evalpreparation", "Pr�paration � l'�valuation");
        aMap.put("evalsubproc", "Modalit�s de remise et p�nalit�s");
        aMap.put("actResBefore", "Activit�s/Ressources avant la s�ance");
        aMap.put("actResDuring", "Activit�s/Ressources pendant la s�ance");
        aMap.put("actResAfter", "Activit�s/Ressources apr�s la s�ance");
        aMap.put("citationListName", "R�f�rences bibliographiques du cours");
        rubricMap_fr = Collections.unmodifiableMap(aMap);
        
        Map<String, String> bMap = new HashMap<String, String>();
        bMap.put("description", "Description");
        bMap.put("objectives", "Objectives");
        bMap.put("learningstrat", "Learning Strategy");
        bMap.put("coordinators", "Coordinator");
        bMap.put("lecturers", "Lecturer(s)");
        bMap.put("teachingassistants", "Teaching Assistant(s)");
        bMap.put("speakers", "Speaker(s)");
        bMap.put("secretaries", "Secretary(ies)");
        bMap.put("bibliographicres", "Bibliographic Resources");
        bMap.put("misresources", "Miscellaneous Resources");
        bMap.put("complbibres", "Complementary Bibliographical Resources");
        bMap.put("case", "Case");
        bMap.put("tools", "Tools");
        bMap.put("pastexams", "Past Exams");
        bMap.put("evalcriteria", "Evaluation Criteria");
        bMap.put("evalpreparation", "Preperation to Evaluation");
        bMap.put("evalsubproc", "Submission Procedures and Penalties");
        bMap.put("actResBefore", "Activities/Resources before session");
        bMap.put("actResDuring", "Activities/Resources during session");
        bMap.put("actResAfter", "Activities/Resources after session");
        bMap.put("citationListName", "Course citation list");
        rubricMap_en = Collections.unmodifiableMap(bMap);

        Map<String, String> cMap = new HashMap<String, String>();
        cMap.put("description", "Descripci�n");
        cMap.put("objectives", "Objetivos");
        cMap.put("learningstrat", "Estrategia de aprendizaje");
        cMap.put("coordinators", "Coordinador");
        cMap.put("lecturers", "Profesor(es)");
        cMap.put("teachingassistants", "Profesor(es) ayudant(es)");
        cMap.put("speakers", "Conferencista(s)");
        cMap.put("secretaries", "Secretaria(s)");
        cMap.put("bibliographicres", "Referencias bibliogr�ficas");
        cMap.put("misresources", "Recursos generales");
        cMap.put("complbibres", "Recursos bibliogr�ficos complementarios");
        cMap.put("case", "Caso");
        cMap.put("tools", "Herramientas");
        cMap.put("pastexams", "Ex�menes anteriores");
        cMap.put("evalcriteria", "Criterios de evaluaci�n");
        cMap.put("evalpreparation", "Preparaci�n para la evaluaci�n");
        cMap.put("evalsubproc", "Procedimientos de presentaci�n y penalidades");
        cMap.put("actResBefore", "Actividades y/o Recursos antes de la sesi�n");
        cMap.put("actResDuring", "Actividades y/o Recursos durante de la sesi�n");
        cMap.put("actResAfter", "Actividades y/o Recursos despu�s de la sesi�n");
        cMap.put("citationListName", "Curso referencias");
        rubricMap_es = Collections.unmodifiableMap(cMap);

        // citation type keywords from opensyllabus
        Map<String, String> dMap = new HashMap<String, String>();
        dMap.put("article", "REFERENCE_TYPE_ARTICLE");
        dMap.put("news_article", "REFERENCE_TYPE_ARTICLE_NEWS");
        dMap.put("scientific_article", "REFERENCE_TYPE_ARTICLE");
        dMap.put("professional_article", "REFERENCE_TYPE_ARTICLE");
        dMap.put("unpublished_article", "REFERENCE_TYPE_ARTICLE");
        dMap.put("slides", "REFERENCE_TYPE_SLIDES");
        dMap.put("exercise", "REFERENCE_TYPE_EXERCISE");
        dMap.put("solution", "REFERENCE_TYPE_SOLUTION");
        dMap.put("case", "REFERENCE_TYPE_CASE");
        dMap.put("course_package", "REFERENCE_TYPE_OTHER");
        dMap.put("report", "REFERENCE_TYPE_REPORT");
        dMap.put("consultant_report", "REFERENCE_TYPE_REPORT");
        dMap.put("annual_report", "REFERENCE_TYPE_REPORT");
        dMap.put("governement_report", "REFERENCE_TYPE_REPORT");
        dMap.put("international_organization_report", "REFERENCE_TYPE_REPORT");
        dMap.put("data", "REFERENCE_TYPE_DATA");
        dMap.put("document", "REFERENCE_TYPE_OTHER");
        dMap.put("pedagogical_document", "REFERENCE_TYPE_PEDAGOGIC");
        dMap.put("book", "REFERENCE_TYPE_BOOK");
        dMap.put("book_chapter", "REFERENCE_TYPE_BOOK_CHAPTER");
        dMap.put("survey", "REFERENCE_TYPE_POLL");
        dMap.put("past_exam", "REFERENCE_TYPE_OLD_EXAM");
        dMap.put("website", "REFERENCE_TYPE_WEBSITE");
        dMap.put("image", "REFERENCE_TYPE_OTHER");
        dMap.put("graphic", "REFERENCE_TYPE_OTHER");
        dMap.put("audio", "REFERENCE_TYPE_AUDIO");
        dMap.put("video", "REFERENCE_TYPE_VIDEO");
        dMap.put("simulation", "REFERENCE_TYPE_SIMULATION");
        dMap.put("game", "REFERENCE_TYPE_SIMULATION");
        dMap.put("software", "REFERENCE_TYPE_SOFTWARE");
        dMap.put("other", "REFERENCE_TYPE_OTHER");
        dMap.put("noType", "REFERENCE_TYPE_OTHER");
        citationTypes = Collections.unmodifiableMap(dMap);
        
        Map<String, String> eMap = new HashMap<String, String>();
//      eMap.put("article", "");
//      eMap.put("news_article", "");
//      eMap.put("scientific_article", "");
//      eMap.put("professional_article", "");
//      eMap.put("unpublished_article", "");
//      eMap.put("slides", "");
//      eMap.put("exercise", "");
//      eMap.put("solution", "");
//      eMap.put("case", "");
//      eMap.put("course_package", "");
        eMap.put("report", "DOCUMENT_TYPE_REPORT");
        eMap.put("consultant_report", "DOCUMENT_TYPE_REPORT");
        eMap.put("annual_report", "DOCUMENT_TYPE_REPORT");
        eMap.put("governement_report", "DOCUMENT_TYPE_REPORT");
        eMap.put("international_organization_report", "DOCUMENT_TYPE_REPORT");
//      eMap.put("data", "");
//      eMap.put("document", "");
//      eMap.put("pedagogical_document", "");
//      eMap.put("book", "");
//      eMap.put("book_chapter", "");
//      eMap.put("survey", "");
//      eMap.put("past_exam", "");
//      eMap.put("website", "");
//      eMap.put("image", "");
//      eMap.put("graphic", "");
//      eMap.put("audio", "");
//      eMap.put("video", "");
//      eMap.put("simulation", "");
//      eMap.put("game", "");
//      eMap.put("software", "");
//      eMap.put("other", "");
//      eMap.put("noType", "");
      documentTypes = Collections.unmodifiableMap(eMap);
      
      Map<String, String> fMap = new HashMap<String, String>();
      fMap.put("article", "HYPERLINK_TYPE_ARTICLE");
      fMap.put("news_article", "HYPERLINK_TYPE_ARTICLE");
      fMap.put("scientific_article", "HYPERLINK_TYPE_ARTICLE");
      fMap.put("professional_article", "HYPERLINK_TYPE_ARTICLE");
      fMap.put("unpublished_article", "HYPERLINK_TYPE_ARTICLE");
//    eMap.put("slides", "");
//    eMap.put("exercise", "");
//    eMap.put("solution", "");
//    eMap.put("case", "");
//    eMap.put("course_package", "");
      fMap.put("report", "HYPERLINK_TYPE_REPORT");
      fMap.put("consultant_report", "HYPERLINK_TYPE_REPORT");
      fMap.put("annual_report", "HYPERLINK_TYPE_REPORT");
      fMap.put("governement_report", "HYPERLINK_TYPE_REPORT");
      fMap.put("international_organization_report", "HYPERLINK_TYPE_REPORT");
//    eMap.put("data", "");
//    eMap.put("document", "");
//    eMap.put("pedagogical_document", "");
//    eMap.put("book", "");
//    eMap.put("book_chapter", "");
//    eMap.put("survey", "");
      fMap.put("past_exam", "HYPERLINK_TYPE_ARTICLE");
//    eMap.put("website", "");
//    eMap.put("image", "");
//    eMap.put("graphic", "");
//    eMap.put("audio", "");
//    eMap.put("video", "");
      fMap.put("simulation", "HYPERLINK_TYPE_SIMULATION");
      fMap.put("game", "HYPERLINK_TYPE_SIMULATION");
//    eMap.put("software", "");
//    eMap.put("other", "");
//    eMap.put("noType", "");
      hyperlinkTypes = Collections.unmodifiableMap(fMap);
      
      Map<String, String> gMap = new HashMap<String, String>();
      gMap.put("Adjunct Professor", "FORM_CONTACT_TITLE_ADJUNCTPROFESSOR");
      gMap.put("Affiliated Professor", "FORM_CONTACT_TITLE_AFFILIATEDPROFESSOR");
      gMap.put("Assistant Professor", "FORM_CONTACT_TITLE_ASSISTANTPROFESSOR");
      gMap.put("Associate Professor", "FORM_CONTACT_TITLE_ASSOCIATEPROFESSOR");
      gMap.put("Full-time Faculty Lecturer", "FORM_CONTACT_TITLE_FULLTIMEFACULTYLECTURER");
      gMap.put("Full-time Lecturer", "FORM_CONTACT_TITLE_FULLTIMELECTURER");
      gMap.put("Guest Professor", "FORM_CONTACT_TITLE_GUESTPROFESSOR");
      gMap.put("Honorary Professor", "FORM_CONTACT_TITLE_HONORARYPROFESSOR");
      gMap.put("Part-time Faculty Lecturer", "FORM_CONTACT_TITLE_PARTTIMEFACULTYLECTURER");
      gMap.put("Part-time Lecturer", "FORM_CONTACT_TITLE_PARTTIMELECTURER");
      gMap.put("Professor", "FORM_CONTACT_TITLE_PROFESSOR");
      gMap.put("Secretary", "FORM_CONTACT_TITLE_SECRETARY");
      gMap.put("Student", "FORM_CONTACT_TITLE_STUDENT");
      gMap.put("Trainee", "FORM_CONTACT_TITLE_TRAINEE");
      contactTitles = Collections.unmodifiableMap(gMap);
    }	
	
    @Override
	public Syllabus importSyllabusFromSite(String siteId, String destinationSiteId) throws DeniedAccessException {
		
		COModeledServer osylCO = null;
		try {
			osylCO = osylSiteService.getCourseOutlineForTenjinImport(siteId);
		} catch (PermissionException pe) {
			throw new DeniedAccessException();
		} catch (Exception e) {
			log.error("Could not retrieve specified OpenSyllabus course outline");
			return null;
		}
		
		if (osylCO == null) 
			return null;

		String osylLang = osylCO.getModeledContent().getProperty("language");
		String lang = "fr_CA";
		if (osylLang.equals("ENG")) {
			lang = "en_US";
		} else if (osylLang.equals("ES")) {
			lang = "es";
		}
		
		HashMap<String, HashMap<String, Object>> templateRules = null;
		try {
			// hardcoded template, need the rules to set template structure ids
			templateRules = templateService.getTemplateRules(1L, lang);
		} catch (IdUnusedException e) {
			// should propogate?
			log.error("Could not retrieve template");
			return null;
		}

		if (templateRules == null) // TODO:  should this be an exception?
			return null;
		
		Syllabus syllabus = templateService.getEmptySyllabusFromTemplate(1L, lang);
		
		// keep track of already copied resources
		Map<String, String> copiedResources = new HashMap<String, String>();
		CitationCollection destinationCitationList = null;
		
		for (COModelInterface e : osylCO.getModeledContent().getChildrens()) {
			SyllabusCompositeElement copyTo = null;
			
			if (e.getType().equals("OverviewStruct")) {
				copyTo = (SyllabusCompositeElement) syllabus.getElements().get(0);
			} else if (e.getType().equals("StaffStruct")) {
				copyTo = (SyllabusCompositeElement) syllabus.getElements().get(1);
			} else if (e.getType().equals("LearningMaterialStruct")) {
				copyTo = (SyllabusCompositeElement) syllabus.getElements().get(2);
			} else if (e.getType().equals("AssessmentStruct")) {
				copyTo = (SyllabusCompositeElement) syllabus.getElements().get(3);
			} else if (e.getType().equals("PedagogicalStruct")) {
				copyTo = (SyllabusCompositeElement) syllabus.getElements().get(4);
			}

			if (copyTo != null)
				recursiveCopyToTenjinSyllabus(copyTo, e, lang, templateRules, destinationSiteId, destinationCitationList, copiedResources);			
		}
	
		// set title
		if (lang.equals("fr_CA"))
			syllabus.setTitle("Commun");
		else if (lang.equals("en_US"))
			syllabus.setTitle("Common");
		else if (lang.equals("es"))
			syllabus.setTitle("Com�n");
		
		syllabus.setCommon(true);
		
		return syllabus;
		
	}
	
	private void recursiveCopyToTenjinSyllabus(SyllabusCompositeElement elem, COModelInterface comi, String lang, HashMap<String, HashMap<String, Object>> templateRules, String destinationSiteId, CitationCollection destinationCitationList, Map<String, String> copiedResources) {
		SyllabusCompositeElement compositeElement = null;
		
		if (comi instanceof COContentResourceProxy) {
			COContentResourceProxy cocrp = (COContentResourceProxy) comi;
			
			String rubricKey = null;
			Iterator<String> i = cocrp.getRubrics().keySet().iterator(); 
			while (i.hasNext()) {
				String key = (String) i.next();
				
				// for some reason the rubric keyword is it's type
				rubricKey = cocrp.getRubrics().get(key).getType();
				
				log.debug("Rubric: " + rubricKey);
			}
			
			if (rubricKey.equals("undefined")) {
				// don't add elements in undefined rubric
				return;
			}
			
			// get the title for the rubric for the correct language
			String rubricTitle = null;
			if (lang.equals("fr_CA")) {
				rubricTitle = rubricMap_fr.getOrDefault(rubricKey, rubricKey);
			} else if (lang.equals("en_US")) {
				rubricTitle = rubricMap_en.getOrDefault(rubricKey, rubricKey);				
			} else if (lang.equals("es")) {
				rubricTitle = rubricMap_es.getOrDefault(rubricKey, rubricKey);				
			}
			
			AbstractSyllabusElement tenjinElement = convertToTenjinElement(cocrp, destinationSiteId, copiedResources, lang);
			if (tenjinElement == null)
				return;
			
			// find the desired rubric
			boolean added = false;
			for (AbstractSyllabusElement r : elem.getElements()) {
				if (r.isComposite() && r.getTitle().equals(rubricTitle)) {
					
					SyllabusCompositeElement parentRubric = (SyllabusCompositeElement)r;
					tenjinElement.setTemplateStructureId(getTemplateStructureIdForElement(parentRubric, tenjinElement, templateRules));
					parentRubric.getElements().add(tenjinElement);
					added = true;
				}
			}
			
			// If the rubric doesn't exist, create it
			if (!added) {
				// if we didn't find the rubric, create one
				SyllabusRubricElement newRubric = new SyllabusRubricElement();
				newRubric.setTitle(rubricTitle);
				
				newRubric.setElements(new ArrayList<AbstractSyllabusElement>());
				newRubric.setAttributes(new HashMap<String, String>());

				// we are generating a common syllabus
				newRubric.setCommon(true);
				
				// elements are not public by default
				newRubric.setPublicElement(false);
				
				// composite elements cannot be hidden or important
				newRubric.setHidden(false);
				newRubric.setImportant(false);
				
				newRubric.setCreatedDate(new Date());
				// TODO created by current user.
				//ret.setCreatedBy()
				
				// Get template structure ids for new rubric and element
				newRubric.setTemplateStructureId(getTemplateStructureIdForElement(elem, newRubric, templateRules));
				tenjinElement.setTemplateStructureId(getTemplateStructureIdForElement(newRubric, tenjinElement, templateRules));

				newRubric.getElements().add(tenjinElement);
				elem.getElements().add(newRubric);
			}
			

		} else if (comi instanceof COUnit) {
			COUnit cou = (COUnit) comi;
			compositeElement = convertToTenjinCompositeElement(cou);

			if (compositeElement != null) {
				compositeElement.setTemplateStructureId(getTemplateStructureIdForElement(elem, compositeElement, templateRules));
				elem.getElements().add(compositeElement);
			}
		}
		
		if (comi instanceof COElementAbstract) {
			COElementAbstract abstractElement = (COElementAbstract) comi;
			
			for (Object child : abstractElement.getChildrens()) {
				
				if (compositeElement != null) {
					recursiveCopyToTenjinSyllabus(compositeElement, (COModelInterface)child, lang, templateRules, destinationSiteId, destinationCitationList, copiedResources);
				} else {
					recursiveCopyToTenjinSyllabus(elem, (COModelInterface)child, lang, templateRules, destinationSiteId, destinationCitationList, copiedResources);
				}
			}
		}
	}
	
	private Long getTemplateStructureIdForElement(SyllabusCompositeElement parentElement, AbstractSyllabusElement newElement, HashMap<String, HashMap<String, Object>> templateRules) {
		HashMap<String, Object> rulesForParent = null;
		
		if (parentElement.getTemplateStructureId() != null && 
				templateRules.containsKey(parentElement.getTemplateStructureId().toString()))
			rulesForParent = templateRules.get(parentElement.getTemplateStructureId().toString());
			
		if (rulesForParent != null && rulesForParent.containsKey("elements")) {									
			List<Object> elementsForParent = (List<Object>)rulesForParent.get("elements");
			for (Object o : elementsForParent) {
				HashMap<String, Object> map = (HashMap<String, Object>)o;
				String ruleType = (String)map.get("type");
				String ruleLabel = (String)map.get("label");
				
				// if the new element is a rubric, find template rule with the same title
				// or just match types
				if ((ruleType.equals("rubric") && ruleLabel.equals(newElement.getTitle())) || 
						!ruleType.equals("rubric") && ruleType.equals(newElement.getType())) {

					return (Long)map.get("id");
				}
			}
		}
		
		String error = "templateStructureId is null for child " + newElement.getType();
		if (newElement.getType().equals("rubric"))
			error += ":"+newElement.getTitle();
		error += " of " + parentElement.getTitle();
		log.error(error);

		return null;
	}

	private SyllabusCompositeElement convertToTenjinCompositeElement(COUnit element) {
		SyllabusCompositeElement ret = null;
		
		if (element.getType().equals("AssessmentUnit")) {
			// TODO : exam/eval attributes
			HashMap<String, String> attributes = new HashMap<String, String>();
			
			if (element.getProperty("assessmentType") == null) {
				// a new evaluation in OpenSyllabus can have no type, until one is selected
				return null;
			}
			
			if (element.getProperty("assessmentType").equals("intra_exam") || 
					element.getProperty("assessmentType").equals("final_exam")) {
				
				ret = new SyllabusExamElement();
				attributes.put("examWeight", element.getProperty("weight"));
				if (element.getProperties().containsKey("location")) {
					attributes.put("examAtHome", element.getProperty("location").contains("home")?"true":"false");
					attributes.put("examInClass", element.getProperty("location").contains("inClass")?"true":"false");
				}
				if (element.getProperties().containsKey("modality")) {
					attributes.put("examOral", element.getProperty("modality").contains("oral")?"true":"false");
					attributes.put("examWritten", element.getProperty("modality").contains("written")?"true":"false");
				}
				if (element.getProperties().containsKey("submition_type")) {
					attributes.put("examPaper", element.getProperty("submition_type").contains("paper")?"true":"false");
					attributes.put("examElectronic", element.getProperty("submition_type").contains("elect")?"true":"false");
				}
				attributes.put("examMode", element.getProperty("mode"));
				
			} else if (element.getProperty("assessmentType").equals("quiz") ||
					element.getProperty("assessmentType").equals("session_work") ||
					element.getProperty("assessmentType").equals("participation") ||
					element.getProperty("assessmentType").equals("other")) {
				
				ret = new SyllabusEvaluationElement();
				attributes.put("evaluationWeight", element.getProperty("weight"));
				if (element.getProperties().containsKey("location")) {
					attributes.put("evaluationAtHome", element.getProperty("location").contains("home")?"true":"false");
					attributes.put("evaluationInClass", element.getProperty("location").contains("inClass")?"true":"false");
				}
				if (element.getProperties().containsKey("modality")) {
					attributes.put("evaluationOral", element.getProperty("modality").contains("oral")?"true":"false");
					attributes.put("evaluationWritten", element.getProperty("modality").contains("written")?"true":"false");
				}
				if (element.getProperties().containsKey("submition_type")) {
					attributes.put("evaluationPaper", element.getProperty("submition_type").contains("paper")?"true":"false");
					attributes.put("evaluationElectronic", element.getProperty("submition_type").contains("elect")?"true":"false");
				}
				attributes.put("evaluationMode", element.getProperty("mode"));
				attributes.put("evaluationDate", element.getProperty("date-start"));
			}
			ret.setAttributes(attributes);
			
		} else if (element.getType().equals("Lecture")) {			
			ret = new SyllabusLectureElement();
		} else if (element.getType().equals("WorkSession")) {
			ret = new SyllabusTutorialElement();
		}

		if (ret != null) {
			ret.setTitle(element.getLabel());
			ret.setElements(new ArrayList<AbstractSyllabusElement>());

			// we are generating a common syllabus
			ret.setCommon(true);
			
			// elements are not public by default
			ret.setPublicElement(false);
			
			// composite elements cannot be hidden or important
			ret.setHidden(false);
			ret.setImportant(false);
			
			ret.setCreatedDate(new Date());

			log.debug(ret.toString());
		}
		
		return ret;
	}
	
	private AbstractSyllabusElement convertToTenjinElement(COContentResourceProxy element, 
			String destinationSiteId, Map<String, String> copiedResources, String locale) {
		
		AbstractSyllabusElement ret = null;
		
		COModelInterface resource = element.getResource();

		HashMap<String, String> attributes = new HashMap<String, String>();

		if (resource.getType().equals("Text")) {
			
			String text = resource.getProperty("text");

			ret = new SyllabusTextElement();
			ret.setDescription(text);
			
		} else if (resource.getType().equals("URL")) {			
			// label, type, url, comment
			String uri = resource.getProperty(
				    COPropertiesType.IDENTIFIER,
				    COPropertiesType.IDENTIFIER_TYPE_URI);
			String type = resource.getProperty("asmResourceType");
			String title = element.getProperty("label");
			String description = element.getProperty("comment");
			
			ret = new SyllabusHyperlinkElement();
			ret.setTitle(title);
			ret.setDescription(description);
			
			attributes.put("hyperlinkUrl", uri);

			if (hyperlinkTypes.containsKey(type))
				attributes.put("hyperlinkType", hyperlinkTypes.get(type));
			
		} else if (resource.getType().equals("Document")) {
			String uri = resource.getProperty(
				    COPropertiesType.IDENTIFIER,
				    COPropertiesType.IDENTIFIER_TYPE_URI);
			String type = resource.getProperty("asmResourceType");
			String title = element.getProperty("label");
			String description = element.getProperty("comment");
						
			ret = new SyllabusDocumentElement();
			ret.setTitle(title);
			ret.setDescription(description);
			
			String newUri = null;
			if(!copiedResources.containsKey(uri)) {
				try {
					newUri = copyResource(uri, destinationSiteId);
					copiedResources.put(uri, newUri);
				} catch (PermissionException | IdUnusedException | TypeException | InUseException | OverQuotaException
						| IdUsedException | ServerOverloadException | InconsistentException | IdLengthException
						| IdUniquenessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				newUri = copiedResources.get(uri);
			}
			
			attributes.put("documentId", newUri);

			if (documentTypes.containsKey(type))
				attributes.put("documentType", documentTypes.get(type));
			
		} else if (resource.getType().equals("BiblioResource")) {
			
			String uri = resource.getProperty(
					COPropertiesType.IDENTIFIER,
				    COPropertiesType.IDENTIFIER_TYPE_URI);
			String type = resource.getProperty("resourceType");
			String title = element.getProperty("label");
			String description = element.getProperty("comment");

			String newCitationId = null;
			if (!copiedResources.containsKey(uri)) {
				try {
					newCitationId = copyCitation(uri, destinationSiteId, locale);
					copiedResources.put(uri, newCitationId);
				} catch (PermissionException | IdUnusedException | TypeException | InUseException | OverQuotaException
						| IdUsedException | ServerOverloadException | InconsistentException | IdLengthException
						| IdUniquenessException | IdInvalidException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			} else {
				newCitationId = copiedResources.get(uri);
			}

			// if citation is not in it's original list anymore, it is not added.
			if (newCitationId != null) {
				ret = new SyllabusCitationElement();
				ret.setTitle(title);
				ret.setDescription(description);
				attributes.put("citationId", newCitationId);
			}
			
			if (citationTypes.containsKey(type))
				attributes.put("citationType", citationTypes.get(type));
			
		} else if (resource.getType().equals("Entity")) {
			
			String uri = resource.getProperty(
					COPropertiesType.IDENTIFIER,
				    COPropertiesType.IDENTIFIER_TYPE_URI);
			String title = element.getProperty("label");
			String description = element.getProperty("comment");

			ret = new SyllabusSakaiToolElement();
			ret.setTitle(title);
			ret.setDescription(description);
			attributes.put("sakaiToolId", uri);

		} else if (resource.getType().equals("Person")) {
			
			String firstName = resource.getProperty("firstname");
			String lastName = resource.getProperty("surname");
			String title = resource.getProperty("title");
			String email = resource.getProperty("email");
			String telephone = resource.getProperty("tel");
			String officeRoom = resource.getProperty("officeroom");
			String availability = element.getProperty("availability"); 

			ret = new SyllabusContactInfoElement();
			
			attributes.put("contactInfoFirstName", firstName);
			attributes.put("contactInfoLastName", lastName);
			attributes.put("contactInfoEmail", email);
			attributes.put("contactInfoTelephone", telephone);
			attributes.put("contactInfoOfficeRoom", officeRoom);
			attributes.put("contactInfoAvailability", availability);
			
			if (contactTitles.containsKey(title.trim()))
				attributes.put("contactInfoTitle", contactTitles.get(title.trim()));
		}
		
		if (ret != null) {
			ret.setAttributes(attributes);

			// We are generating a common syllabus
			ret.setCommon(true);

			// elements are not public by default
			ret.setPublicElement(false);

			// set hidden
			String visible = element.getProperty("visible"); 
			if (visible != null && visible.equals("false")) 
				ret.setHidden(true);
			else 
				ret.setHidden(false);

			// set important
			String importance = element.getProperty("importance"); 
			if (importance != null && importance.equals("true"))
				ret.setImportant(true);
			else
				ret.setImportant(false);

			ret.setCreatedDate(new Date());

			log.debug(ret.toString());
		}
		return ret;
	}

	private String copyResource(String uri, String newSiteId) throws PermissionException, IdUnusedException, TypeException, InUseException, OverQuotaException, IdUsedException, ServerOverloadException, InconsistentException, IdLengthException, IdUniquenessException {

		String newSiteCollectionId = contentService.getSiteCollection(newSiteId);
		String newId = contentService.copyIntoFolder(uri, newSiteCollectionId);
		return newId;
	}	

	private String copyCitation(String citationId, String newSiteId, String locale) 
			throws PermissionException, IdUnusedException, TypeException, InUseException, OverQuotaException, IdUsedException, ServerOverloadException, InconsistentException, IdLengthException, IdUniquenessException, IdInvalidException {

		String newSiteCollectionId = contentService.getSiteCollection(newSiteId);
		
		String citationListName = null; 
		if (locale.equals("en_US"))
			citationListName = rubricMap_en.get("citationListName");
		else if (locale.equals("es"))
			citationListName = rubricMap_es.get("citationListName");
		else
			citationListName = rubricMap_fr.get("citationListName");
		
		CitationCollection destinationCitationList = null;
		
		try {
			ContentResource cr = contentService.getResource(newSiteCollectionId + citationListName);
			destinationCitationList = citationService.getCollection(new String(cr.getContent()));
		} catch (IdUnusedException e) {
			ContentResourceEdit cre =
					contentService.addResource(newSiteCollectionId, citationListName, null, 1);
		
			destinationCitationList = citationService.addCollection();
			cre.setContent(destinationCitationList.getId().getBytes());
			cre.setResourceType(CitationService.CITATION_LIST_ID);
			cre.setContentType(ResourceType.MIME_TYPE_HTML);

			ResourcePropertiesEdit props = cre.getPropertiesEdit();
			props.addProperty(
				ContentHostingService.PROP_ALTERNATE_REFERENCE,
				org.sakaiproject.citation.api.CitationService.REFERENCE_ROOT);
			props.addProperty(ResourceProperties.PROP_CONTENT_TYPE,
				ResourceType.MIME_TYPE_HTML);
			props.addProperty(ResourceProperties.PROP_DISPLAY_NAME,
				citationListName);

			contentService.commitResource(cre, NotificationService.NOTI_NONE);			
		}
		
		ContentResource oldListResource = contentService.getResource(citationId.substring(0, citationId.lastIndexOf('/')));
		if (oldListResource.getContent() == null)
			return null;
		
		CitationCollection oldCollection = citationService.getCollection(new String(oldListResource.getContent()));
		String oldCitationId = citationId.substring(citationId.lastIndexOf('/')+1);
		
		Citation oldCitation = null;
		try {
			oldCitation = oldCollection.getCitation(oldCitationId);
		} catch (IdUnusedException e) {
			log.warn("Cannot retrieve citation " + oldCitationId);
			return null;
		}
		
		Citation newCitation = citationService.copyCitation(oldCitation);
		citationService.save(newCitation);
		
		destinationCitationList.add(newCitation);
		citationService.save(destinationCitationList);
		
		return newSiteCollectionId + citationListName + "/" + newCitation.getId();
	}	
}
