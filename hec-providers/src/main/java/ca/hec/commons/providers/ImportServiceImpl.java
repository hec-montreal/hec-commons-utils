package ca.hec.commons.providers;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
import lombok.Setter;
import ca.hec.tenjin.api.ImportService;
import ca.hec.tenjin.api.TemplateService;
import org.sakaiquebec.opensyllabus.common.api.OsylSiteService;
import org.sakaiquebec.opensyllabus.common.model.COModeledServer;
import org.sakaiquebec.opensyllabus.shared.model.COContentResource;
import org.sakaiquebec.opensyllabus.shared.model.COContentResourceProxy;
import org.sakaiquebec.opensyllabus.shared.model.COContentRubric;
import org.sakaiquebec.opensyllabus.shared.model.COElementAbstract;
import org.sakaiquebec.opensyllabus.shared.model.COModelInterface;
import org.sakaiquebec.opensyllabus.shared.model.COPropertiesType;
import org.sakaiquebec.opensyllabus.shared.model.COStructureElement;
import org.sakaiquebec.opensyllabus.shared.model.COUnit;
import org.sakaiquebec.opensyllabus.shared.model.COUnitStructure;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.exception.PermissionException;

public class ImportServiceImpl implements ImportService {
	private static Log log = LogFactory.getLog(ImportServiceImpl.class);
	
	@Setter
	OsylSiteService osylSiteService;
	
	@Setter
	TemplateService templateService;
	
	public synchronized Syllabus importSyllabusFromSite(String siteId) throws PermissionException {
		
		//TODO : i18n
		Syllabus syllabus = templateService.getEmptySyllabusFromTemplate(1L, "fr_CA");
		
		COModeledServer osylCO = null;
		try {
			osylCO = osylSiteService.getCourseOutlineForTenjinImport(siteId);
		} catch (PermissionException pe) {
			throw pe;
		} catch (Exception e) {
			log.error("Could not retrieve specified OpenSyllabus course outline");
			return null;
		}
		
		if (osylCO == null) 
			return null;
		
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
				recursiveCopyToTenjinSyllabus(copyTo, e);			
		}
	
		// TODO: get Syllabus data
		syllabus.setTitle(osylCO.getModeledContent().getLabel());
		
		return syllabus;
		
	}
	
	private void recursiveCopyToTenjinSyllabus(SyllabusCompositeElement elem, COModelInterface comi) {
		
		SyllabusCompositeElement compositeElement = null;
		
		if (comi instanceof COContentResourceProxy) {
			COContentResourceProxy cocrp = (COContentResourceProxy) comi;
			
			String rubric = null;
			Iterator<String> i = cocrp.getRubrics().keySet().iterator(); 
			while (i.hasNext()) {
				String key = (String) i.next();
				
				// for some reason the rubric keyword is it's type
				rubric = cocrp.getRubrics().get(key).getType();
				
				log.error("Rubric: " + rubric);
			}
			
			AbstractSyllabusElement tenjinElement = convertToTenjinElement(cocrp);
			elem.getElements().add(tenjinElement);

		} else if (comi instanceof COUnit) {
			COUnit cou = (COUnit) comi;
			compositeElement = convertToTenjinCompositeElement(cou);

			if (compositeElement != null) {
				elem.getElements().add(compositeElement);
			}
		}
		
		if (comi instanceof COElementAbstract) {
			COElementAbstract abstractElement = (COElementAbstract) comi;
			
			for (Object child : abstractElement.getChildrens()) {
				
				if (compositeElement != null) {
					recursiveCopyToTenjinSyllabus(compositeElement, (COModelInterface)child);
				} else {
					recursiveCopyToTenjinSyllabus(elem, (COModelInterface)child);
				}
			}
		}
	}

	private SyllabusCompositeElement convertToTenjinCompositeElement(COUnit element) {
		SyllabusCompositeElement ret = null;
		
		if (element.getType().equals("AssessmentUnit")) {
			// TODO : exam/eval attributes
			HashMap<String, String> attributes = new HashMap<String, String>();
			
			if (element.getProperty("assessmentType").equals("intra_exam") || 
					element.getProperty("assessmentType").equals("final_exam")) {
				
				ret = new SyllabusExamElement();
				attributes.put("examWeight", element.getProperty("weight"));
				
			} else if (element.getProperty("assessmentType").equals("quiz") ||
					element.getProperty("assessmentType").equals("session_work") ||
					element.getProperty("assessmentType").equals("participation") ||
					element.getProperty("assessmentType").equals("other")) {
				
				ret = new SyllabusEvaluationElement();
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
			// TODO created by current user.
			//ret.setCreatedBy()
		}
		
		log.error(ret);
		return ret;
	}
	
	private AbstractSyllabusElement convertToTenjinElement(COContentResourceProxy element) {
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
			
			attributes.put("documentId", uri);
			attributes.put("documentType", type);
			
		} else if (resource.getType().equals("BiblioResource")) {
			
			String uri = resource.getProperty(
					COPropertiesType.IDENTIFIER,
				    COPropertiesType.IDENTIFIER_TYPE_URI);
			String title = element.getProperty("label");
			String description = element.getProperty("comment");

			//TODO: copy the citation first
			ret = new SyllabusCitationElement();
			ret.setTitle(title);
			ret.setDescription(description);
			attributes.put("citationId", uri);
			
		} else if (resource.getType().equals("Entity")) {
			
			String uri = resource.getProperty(
					COPropertiesType.IDENTIFIER,
				    COPropertiesType.IDENTIFIER_TYPE_URI);
			String title = element.getProperty("label");
			String description = element.getProperty("comment");

			//TODO: copy the resource first
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
		//TODO : set currentuser
		//ret.setCreatedBy(createdBy); 
		
		log.error(ret);
		return ret;
	}
	
}
