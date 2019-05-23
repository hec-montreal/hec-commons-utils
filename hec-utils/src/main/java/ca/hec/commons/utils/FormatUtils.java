/******************************************************************************
 * $Id: $
 ******************************************************************************
 *
 * Copyright (c) 2013 The Sakai Foundation, The Sakai Quebec Team.
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
package ca.hec.commons.utils;
/**
 *
 * Class that gather utils functions to format entities according to HEC rules
 */
public class FormatUtils {

    /**
     * Returns the course id with hyphens using the catalog number
     * from PeopleSoft (or Course Management)
     *
     * @param  courseId the catalog number without hyphens
     * @return      the hyphenated courseId
     */
	public static String formatCourseId(String courseId) {
		String cheminement = null;
		String numero = null;
		String annee = null;
		String formattedCourseId;

		// courseId formatting code taken from OfficialSitesJobImpl (must match)

		// ZCII-3735: ne pas insérer de tirets si l'id de cours commence par une lettre 
		// (nouveau format de numéro de répertoire)
		courseId = courseId.replaceAll(" ", "");
		if (courseId.matches("^[a-zA-Z]")) {
			return courseId;
		}

		// if courseId does not contain a letter
		if (!courseId.matches(".*[^0-9].*")) {
			if (courseId.length() == 7) {
				cheminement = courseId.substring(0, 2);
				numero = courseId.substring(2, 5);
				annee = courseId.substring(5);
			} else if (courseId.length() == 6) {
				cheminement = courseId.substring(0, 1);
				numero = courseId.substring(1, 4);
				annee = courseId.substring(4);
			}
		}
		// if courseId does contain at least one letter
		else {
			if (courseId.endsWith("A") || courseId.endsWith("E")
					|| courseId.endsWith("R") || courseId.endsWith("W")) {
				if (courseId.length() == 8) {
					cheminement = courseId.substring(0, 2);
					numero = courseId.substring(2, 5);
					annee = courseId.substring(5);
				} else if (courseId.length() == 7) {
					cheminement = courseId.substring(0, 1);
					numero = courseId.substring(1, 4);
					annee = courseId.substring(4);
				}
			}
		}

		if (cheminement != null && numero != null && annee != null) {
			formattedCourseId = cheminement + "-" + numero + "-" + annee;
			return formattedCourseId.trim();
		} else {
			return courseId;
		}
	}

    /**
     * Returns the session name (ie H2013, A2012, E2014) corresponding
     * to the PeopleSoft session enterprise id (from course management).
     *
     * @param  sessionId the session eid from PeopleSoft
     * @return      the name of the session in H2014 format
     * @see         AcademicSession
     */
    public static String getSessionName(String sessionId) {
    	String sessionName = "";

    	if (sessionId.charAt(3) == '1')
    		sessionName = "H";
    	else if (sessionId.charAt(3) == '2')
    		sessionName = "E";
    	else if (sessionId.charAt(3) == '3')
    		sessionName = "A";

    	sessionName += "20" + sessionId.substring(1, 3);

    	return sessionName;
    }

}

