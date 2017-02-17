package ca.hec.commons.providers;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.hec.tenjin.api.model.syllabus.AbstractSyllabusElement;
import ca.hec.tenjin.api.model.syllabus.Syllabus;
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

public class ImportServiceImpl implements ImportService {
	private static Log log = LogFactory.getLog(ImportServiceImpl.class);
	
	@Setter
	@Autowired
	OsylSiteService osylSiteService;
	
	@Setter
	@Autowired
	TemplateService templateService;
	
	public synchronized Syllabus importSyllabusFromSite(String siteId) {
		
		ca.hec.tenjin.api.model.syllabus.Syllabus syllabus = templateService.getEmptySyllabusFromTemplate(1L, "fr_CA");
		
		COModeledServer osylCO = null;
		try {
			osylCO = osylSiteService.getCourseOutlineForTenjinImport(siteId);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		// traversal
		
		LinkedList<COModelInterface> elementQueue = new LinkedList<COModelInterface>();
		elementQueue.push(osylCO.getModeledContent());
		
		while (!elementQueue.isEmpty()) {
			COModelInterface elem = elementQueue.removeFirst();

			if (elem instanceof COContentResourceProxy) {
				COContentResourceProxy cocrp = (COContentResourceProxy) elem;
				
				String rubric = null;
				Iterator<String> i = cocrp.getRubrics().keySet().iterator(); 
				while (i.hasNext()) {
					String key = (String) i.next();
					
					// for some reason the rubric keyword is it's type
					rubric = cocrp.getRubrics().get(key).getType();
					
					log.error("Rubric: " + rubric);
				}
				
				AbstractSyllabusElement tenjinElement = convertToTenjinElement(cocrp);
				
			} else if (elem instanceof COUnit) {
				COUnit cou = (COUnit) elem;
				SyllabusCompositeElement compositeElement = convertToTenjinCompositeElement(cou);
				
				// add as child of it's parent
				
				correspondenceMap.put(elem.getId(), compositeElement);
			} else if (elem instanceof COStructureElement) {
				// AssessmentStruct et PedagogicalStruct
				log.error(elem.getType());
				
			}
			
			if (elem instanceof COElementAbstract) {
				elementQueue.addAll(((COElementAbstract)elem).getChildrens());
			}
		}
		
		if (osylCO != null) {
			syllabus.setTitle(osylCO.getModeledContent().getLabel());
		} else {
			syllabus.setTitle(siteId);
		}
		
		return syllabus;
		
	}

	private SyllabusCompositeElement convertToTenjinCompositeElement(COUnit element) {
		SyllabusCompositeElement ret = new SyllabusCompositeElement();
		ret.setTitle(element.getLabel());
		ret.setElements(new ArrayList<AbstractSyllabusElement>());
		
		log.error(element.getType() + ": " + element.getLabel());
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

			//TODO: copy the citation first?
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

		} else {
			log.error("    -> unknown: " + resource.getProperties());
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
		//currentuser
		//ret.setCreatedBy(createdBy); 
		
		log.error(ret);
		return ret;
	}
	
}
