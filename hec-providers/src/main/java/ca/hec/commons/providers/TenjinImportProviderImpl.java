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
    static {
        Map<String, String> aMap = new HashMap<String, String>();
        aMap.put("description", "Description");
        aMap.put("objectives", "Objectifs");
        aMap.put("learningstrat", "Approche pédagogique");
        aMap.put("coordinators", "Coordonnateur");
        aMap.put("lecturers", "Enseignant(s)");
        aMap.put("teachingassistants", "Stagiaire(s) d'enseignement");
        aMap.put("speakers", "Conférencier(s)");
        aMap.put("secretaries", "Secrétaire(s)");
        aMap.put("bibliographicres", "Ressources bibliographiques");
        aMap.put("misresources", "Ressources générales");
        aMap.put("complbibres", "Ressources bibliographiques complémentaires");
        aMap.put("case", "Cas");
        aMap.put("tools", "Outils");
        aMap.put("pastexams", "Anciens examens");
        aMap.put("evalcriteria", "Critères d'évaluation");
        aMap.put("evalpreparation", "Préparation à l'évaluation");
        aMap.put("evalsubproc", "Modalités de remise et pénalités");
        aMap.put("actResBefore", "Activités/Ressources avant la séance");
        aMap.put("actResDuring", "Activités/Ressources pendant la séance");
        aMap.put("actResAfter", "Activités/Ressources après la séance");
        aMap.put("citationListName", "Références bibliographiques du cours");
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
        cMap.put("description", "Descripción");
        cMap.put("objectives", "Objetivos");
        cMap.put("learningstrat", "Estrategia de aprendizaje");
        cMap.put("coordinators", "Coordinador");
        cMap.put("lecturers", "Profesor(es)");
        cMap.put("teachingassistants", "Profesor(es) ayudant(es)");
        cMap.put("speakers", "Conferencista(s)");
        cMap.put("secretaries", "Secretaria(s)");
        cMap.put("bibliographicres", "Referencias bibliográficas");
        cMap.put("misresources", "Recursos generales");
        cMap.put("complbibres", "Recursos bibliográficos complementarios");
        cMap.put("case", "Caso");
        cMap.put("tools", "Herramientas");
        cMap.put("pastexams", "Exámenes anteriores");
        cMap.put("evalcriteria", "Criterios de evaluación");
        cMap.put("evalpreparation", "Preparación para la evaluación");
        cMap.put("evalsubproc", "Procedimientos de presentación y penalidades");
        cMap.put("actResBefore", "Actividades y/o Recursos antes de la sesión");
        cMap.put("actResDuring", "Actividades y/o Recursos durante de la sesión");
        cMap.put("actResAfter", "Actividades y/o Recursos después de la sesión");
        cMap.put("citationListName", "Curso referencias");
        rubricMap_es = Collections.unmodifiableMap(cMap);
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
			syllabus.setTitle("Común");
		
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
			attributes.put("hyperlinkType", type); //TODO : probably have to translate the types?
			
		} else if (resource.getType().equals("Document")) {
			String license = resource.getProperty("license");
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
			attributes.put("documentType", type);
			
		} else if (resource.getType().equals("BiblioResource")) {
			
			String uri = resource.getProperty(
					COPropertiesType.IDENTIFIER,
				    COPropertiesType.IDENTIFIER_TYPE_URI);
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
			attributes.put("contactInfoTitle", title);
			attributes.put("contactInfoEmail", email);
			attributes.put("contactInfoTelephone", telephone);
			attributes.put("contactInfoOfficeRoom", officeRoom);
			attributes.put("contactInfoAvailability", availability);

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
		CitationCollection oldCollection = citationService.getCollection(new String(oldListResource.getContent()));
		
		String oldCitationId = citationId.substring(citationId.lastIndexOf('/')+1);
		
		Citation oldCitation = null;
		try {
			oldCitation = oldCollection.getCitation(oldCitationId);
		} catch (IdUnusedException e) {
			return null;
		}
		
		destinationCitationList.add(oldCitation);
		citationService.save(destinationCitationList);
		
		return newSiteCollectionId + citationListName + "/" + oldCitationId;
	}	
}
