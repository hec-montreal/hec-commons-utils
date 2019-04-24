/******************************************************************************
 * $Id: $
 ******************************************************************************
 *
 * Copyright (c) 2016 The Sakai Foundation, The Sakai Quebec Team.
 *
 * Licensed under the Educational Community License, Version 1.0
 * (the "License"); you may not use this file except in compliance with the
 * License.
 * You may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/ecl1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/
package ca.hec.commons.providers;

/*import ca.hec.tenjin.api.model.syllabus.SyllabusRubricElement;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.content.cover.ContentHostingService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.memory.api.MemoryService;

import ca.hec.tenjin.api.model.syllabus.AbstractSyllabusElement;
import ca.hec.tenjin.api.model.syllabus.SyllabusTextElement;
import ca.hec.tenjin.api.model.template.Template;
import ca.hec.tenjin.api.provider.ExternalDataProvider;/
import lombok.Setter;

/**
 *
 * @author <a href="mailto:mame-awa.diop@hec.ca">Mame Awa Diop</a>
 * @version $Id: $
 */
public class PlagiarismPolicyProvider /* implements ExternalDataProvider */ {

	public void init() {
		
	}
	
    /*private static final String CONFIGURATION_FILE = "/group/tenjin/plagiarismProvider/plagiarismPolicy.properties";
    
    private static String CACHE_NAME = "ca.hec.commons.providers.PlagiarismPolicyProvider";
    private Cache<String, ResourceBundle> cache;

	@Setter
	private MemoryService memoryService;

	public void init() {
		cache = memoryService.newCache(CACHE_NAME);
	}

    @Override
    public AbstractSyllabusElement getAbstractSyllabusElement(String siteId, String locale) {
		String bundlePath = CONFIGURATION_FILE;
		if (locale != null) {
			bundlePath = bundlePath.replace(".properties", "_"+locale+".properties");
		}
		ResourceBundle bundle = getBundle(bundlePath);

		SyllabusRubricElement rubric = null;
		if (bundle != null) {
			SyllabusTextElement textElement = new SyllabusTextElement();

			textElement.setDescription(bundle.getString("plagiarismPolicy"));
			textElement.setTitle(bundle.getString("plagiarismPolicyTitle"));
			textElement.setTemplateStructureId(-1L);
			textElement.setCommon(true);
			textElement.setPublicElement(true);
			textElement.setHidden(false);
			textElement.setImportant(false);

			rubric = new SyllabusRubricElement();
			rubric.setTitle(bundle.getString("plagiarismRubricTitle"));
			List<AbstractSyllabusElement> children = new ArrayList<AbstractSyllabusElement>();
			children.add(textElement);
			rubric.setElements(children);
		}
		return rubric;
    }

    private ResourceBundle getBundle(String path) {
    	ResourceBundle rb = null;
    	
    	if (cache != null && cache.containsKey(path)) {
			rb = cache.get(path);
		}
    	
    	if (rb == null) {
    		try {
    			ContentResource resource = ContentHostingService.getResource(path);
    			rb = new PropertyResourceBundle(resource.streamContent());
    			cache.put(path, rb);
    		} catch (PermissionException e) {
    			e.printStackTrace();
    		} catch (IdUnusedException e) {
    			e.printStackTrace();
    		} catch (TypeException e) {
    			e.printStackTrace();
    		} catch (ServerOverloadException e) {
    			e.printStackTrace();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    	}
		return rb;
	}*/

}

