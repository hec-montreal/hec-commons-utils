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

import ca.hec.cdm.jobs.model.CourseOffering;
import ca.hec.tenjin.api.model.syllabus.AbstractSyllabusElement;
import ca.hec.tenjin.api.model.syllabus.SyllabusRubricElement;
import ca.hec.tenjin.api.model.syllabus.SyllabusTextElement;
import ca.hec.tenjin.api.provider.ExternalDataProvider;
import ca.hec.cdm.jobs.CatalogDescriptionJobDao;
import lombok.Setter;

import java.util.ArrayList;

public class OfficialCourseDescriptionProvider implements ExternalDataProvider {

    @Setter
    CatalogDescriptionJobDao courseOfferingDao;

    @Override
    public AbstractSyllabusElement getAbstractSyllabusElement(String siteId, String locale) {

        SyllabusRubricElement descriptionRubric = new SyllabusRubricElement();
        descriptionRubric.setTitle("Description");

        String description = null;
        if (siteId.contains(".")) {
            String catalogNbr = siteId.substring(0, siteId.indexOf('.')).replace("-", "");
            description = getOfficialDescriptionString(catalogNbr);
        }

        if (description == null) {
            return null;
        }

        SyllabusTextElement descriptionText = new SyllabusTextElement();
        descriptionText.setDescription(description);
        descriptionText.setTemplateStructureId(-1L);
        descriptionText.setCommon(true);
        descriptionText.setPublicElement(true);
        descriptionText.setHidden(false);
        descriptionText.setImportant(false);

        ArrayList elements = new ArrayList<AbstractSyllabusElement>();
        elements.add(descriptionText);

        descriptionRubric.setElements(elements);
	    return descriptionRubric;
    }

    private String getOfficialDescriptionString(String catalogNbr) {
        String officialDescription = "";
        CourseOffering co = courseOfferingDao.getCourseOffering(catalogNbr);
        if (co == null)
            return null;

        if (co.getShortDescription() != null)
            officialDescription += "<p>"+co.getShortDescription()+"</p>";
        if (co.getLongDescription() != null)
            officialDescription += "<p>"+co.getLongDescription()+"</p>";
        if (co.getThemes() != null)
            officialDescription += "<h3>Thèmes</h3><p>" + co.getThemes().replace("\n", "</br>") + "</p>";

        return officialDescription;
    }
}

