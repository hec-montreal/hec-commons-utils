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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
/**
 * Utility to merge properties during a sakai migration.
 * Example:
 * 
 * 	> mergePropertiesHec pathNewPropertiesFile pathUpdatedValuesOldPropertiesFile pathOriginalValuesOldPropertiesFile
 * 
 * For migration from Sakai 2.8.1 to Sakai 2.9.1, the path would be:
 * 
 * - pathNewPropertiesFile : path to SAKAI 2.9.1 properties file
 * - pathUpdatedValuesOldPropertiesFile : path to SAKAI 2.8.1 MODIF properties file
 * - pathOriginalValuesOldPropertiesFile : path to SAKAI 2.8.1 properties file
 * 
 * 
 * The algorithm is :
 * 
 * - Properties in 'new' properties file are updated with value of the 'UpdatedValuesOld' properties file
 * - Properties that are present in 'UpdatedValuesOld' properties file but not in the other files are added at the end of the 'new' file: they are some personalizations we made during Sakai 2.8.1
 *        
 * The utility will send output to stdout, which you can copy and save in a properties file.
 *
 *
 *  NOte: Currently, the continuation lines (lines ending with \) 
 *        in 'new' are appended into a single line.*/
public class MergePropertiesUtils {
    
public static void main(String[] args) throws Exception {
	
	String pathNewPropertiesFile = args[0];   //file path for Sakai 2.9
	String pathUpdatedValuesOldPropertiesFile = args[1];  //file path for Sakai 2.8 MODIF
	String pathOriginalValuesOldPropertiesFile = args[2]; //file path for Sakai 2.8 

	File newPropertiesFile = new File(pathNewPropertiesFile);  //Sakai 2.9
	File updatedValuesOldPropertiesFile = new File(pathUpdatedValuesOldPropertiesFile);  //Sakai 2.8 MODIF
	File originalValuesOldPropertiesFilesPath = new File(pathOriginalValuesOldPropertiesFile); //Sakai 2.8 
	
	Properties newProps = load(newPropertiesFile);  //Sakai 2.9
	Properties updatedProps = load(updatedValuesOldPropertiesFile);  //Sakai 2.8 MODIF
	Properties originalProps = load(originalValuesOldPropertiesFilesPath); //Sakai 2.8 
	
	/**
	 * Output new properties (2.9.1) with updated values from updatedProps (2.8.1 MODIF)
	 */
	merge(newPropertiesFile, updatedProps);
	
	/**
	 * Output old properties (2.8.1 MODIF) that are not present in 2.8.1 or 2.9.1 (personalizations)
	 */
	printNewProps(updatedValuesOldPropertiesFile, newProps,  originalProps);	
	
    }

    /**
     * Merge newProps to mainProps.

     * NOTE: The idea is that we want to write out the properties in exactly the same order, for future comparison purposes.
     * 
     * @param newPropertiesFile
     * @param newProps
     * @return nb of properties from main props file.
     */
    public static void merge(File newPropertiesFile, Properties updatedProps) throws Exception {
	/**
	 * 1) Read line by line of the new PropertiesFile (Sakai 2.9.1)
	 *  For each line: extract property
	 *  check if we have a match one in the propToMerge 
	 *    - if no, then rewrite the same line
	 *    - if yes: - if same value, rewrite the same line 
	 *              - if different value, rewrite the prop with the new value
	 *              - For both cases, delete the key from newProperties.
	 *              
	 *  2) At the end, write the remaining list of propToMerge at
	 *     the end of mainProp
	 */
	
	try {
	    
	    int nbPropsInMain = 0;
	    int nbPropsChanged = 0;
	    int nbPropsSimilar = 0;
	    int nbPropsNotInNew = 0;
		    
	    // Open the file that is the first
	    // command line parameter
	    FileInputStream fstream = new FileInputStream(newPropertiesFile);
	    // Get the object of DataInputStream
	    DataInputStream in = new DataInputStream(fstream);
	    BufferedReader br = new BufferedReader(new InputStreamReader(in));
	    LineWithContinuation lineIn;
	    
	    // Read File Line By Line
	    while ((lineIn = LineWithContinuation.readLineWithContinuation(br) ) != null) {
		
		KeyValue keyValue = extractKeyValue(lineIn.getFullLine());
		
		// May be a comment line, or blank line, or not a line containing key & property pair (we expect "key = value" line).
		// Simply echo the line back.
		if (keyValue == null) {
		    System.out.println(lineIn.getFullLine());
		    continue;
		}
		
		nbPropsInMain++;
		
		String key = keyValue.key;
		//System.out.println(key);
		
		String newValue = updatedProps.getProperty(key);
		String valueEscaped = unescapeJava(keyValue.value);
		
		if (newValue != null) {
		    if (!newValue.equals(valueEscaped)) {
			String newLine = composeNewPropLine(key, StringEscapeUtils.escapeJava(newValue));
			System.out.println(newLine);
			nbPropsChanged++;
		    } else {
			System.out.println(lineIn.getLineWithReturn());
			nbPropsSimilar++;
		    }
		    
		    // remove the key from newProps because it is used
		    updatedProps.remove(key);
		} else {
		    System.out.println(lineIn.getLineWithReturn());
		    nbPropsNotInNew++;
		}
	    }
	    
	    // Close the input stream
	    in.close();
	    
	    System.out.println("\n\n### " + nbPropsInMain + " properties in SAKAI 11 (" + nbPropsChanged + " changed, " + nbPropsSimilar + " props with same value in both versions, " + nbPropsNotInNew + " not in 2.9.1)");

	    
	} catch (Exception e) {// Catch exception if any
	    System.err.println("Error: " + e.getMessage());
	    throw e;
	}
    }
    
    
    /**
     * Class for reading a line, taking care of the continuation symbol '\' at the end of the line.
     */  
    private static class LineWithContinuation {
	private String fullLine; // the line fully rendered.
	private List<String> linesAsIs; // the original lines unchanged
	
	public LineWithContinuation(String fullLine, List<String> linesAsIs) {
	    this.fullLine = fullLine;
	    this.linesAsIs = linesAsIs;
	}

	public static LineWithContinuation readLineWithContinuation(BufferedReader br) throws IOException {
	    StringBuffer lineAppended = new StringBuffer();
	    List<String> linesAsIs = new ArrayList<String>();
	    String line;
	    
	    while ((line = br.readLine()) != null) {
		boolean isComment = line.startsWith("#");
		boolean isContinued = !isComment && line.endsWith("\\"); // if comment, there is no continuation
		String lineClean = line.substring(0, isContinued ? line.length() - 1 : line.length());
		lineAppended.append(lineClean);
		linesAsIs.add(line);
		
		if (!isContinued) {
		    break;
		}
	    }

	    return line == null ? null : new LineWithContinuation(lineAppended.toString(), linesAsIs);
	}
	
	public String getLineWithReturn () {
	    StringBuffer buf = new StringBuffer();
	    for (int i = 0; i < linesAsIs.size(); i++) {
		boolean last = (i == linesAsIs.size() - 1);
		buf.append(linesAsIs.get(i) + (last ? "": "\n"));
	    }
	    
	    return buf.toString();
	}
	
	public String getFullLine() {
	    return fullLine;
	}
    }
    

    /**
     * Print the newprops.
     * Properties that are 'used' already are commented out.
     * 
     * NOTE: The idea is that we want to write out the properties in exactly the same order, for future comparison purposes.
     * 
     * @param newProps
     * @param remainingNewProps New properties that are left, i.e. remaining ones.
     */
    
    private static void printNewProps(File updatedValuesOldPropertiesFile, Properties newProps, Properties originalProps) {
	/**
	 * Read new props line by line.
	 * For each line, extract the prop key
	 *   if it is not in the remaining, then it is used. So we write the line, but commented out
	 *   otherwise, write the line as is.
	 */
	
	
	System.out.println("#########################################################################################");
	System.out.println("####################### Personalizations  from 2.9.1-source-modif #######################");
	
	try {
	    // Open the file that contains Sakai 2.8.1 modif personalizations
	    FileInputStream fstream = new FileInputStream(updatedValuesOldPropertiesFile);
	    // Get the object of DataInputStream
	    DataInputStream in = new DataInputStream(fstream);
	    BufferedReader br = new BufferedReader(new InputStreamReader(in));
	    String strLine;
	    
	    // Read File Line By Line
	    while ((strLine = br.readLine()) != null) {

		KeyValue keyValue = extractKeyValue(strLine);
		
		//if the line does not have the pattern key = value, we skip it 
		if (keyValue == null) {
		    continue;
		}
		
		String key = keyValue.key;
		
		boolean isNotUsedInNewVersion = (newProps.get(key) == null);
		boolean isNotUsedInOldVersion = (originalProps.get(key) == null);
		
		// Write out line only if property is a personalized property:it is not used in new version and was not used in old version
		if (isNotUsedInNewVersion && isNotUsedInOldVersion) {
		    System.out.println(strLine);
		}
	    }
	    // Close the input stream
	    in.close();
	    
	} catch (Exception e) {// Catch exception if any
	    System.err.println("Error: " + e.getMessage());
	}
    }
    

    /**
     * Compose a line for a property, like 'prop1 = the small dog'.
     * @param key
     * @param value
     * @return
     */
    private static String composeNewPropLine(String key, String value) {
	return key + " = " + value;
    }
    
    private static String unescapeJava(String str) {
	return StringEscapeUtils.unescapeJava(str);
    }

    /**
     * @param strLine 
     * @return the key extracted. Example returns 'prop1' for input 'prop1 = the small dog'
     *    Null is returned if the line is empty, or is a comment line (i.e. starts with a #)
     */
    private static KeyValue extractKeyValue(String strLine) {
	String line = strLine.trim() ;
	
	if (line.startsWith("#")) {
	    return null;
	}
	
	int indexOfEqual = line.indexOf('=');
	
	if (indexOfEqual < 0) {
	    return null;
	}
	
	String key = line.substring(0, indexOfEqual);
	String value = line.substring(indexOfEqual + 1);
	
	return new KeyValue(key.trim(), StringUtils.stripStart(value, null)); // NOTE: Note that value is NOT trimmed to be similar to Properties.load().
	//return new KeyValue(key.trim(), value.trim()); // NOTE: Note that value is NOT trimmed to be similar to Properties.load().
    }
    
   
    
    /**
     * **********
     * Small KeyValue class.
     * **********
     */
     private static class KeyValue {
	public String key;
	public String value;
	public KeyValue(String key, String value) {
	    this.key = key;
	    this.value = value;
	}
    }

    /**
     * Load a Properties File
     * 
     * @param propsFile
     * @return Properties
     * @throws IOException
     */
    public static Properties load(File propsFile) throws IOException {
	Properties props = new Properties();
	FileInputStream fis = new FileInputStream(propsFile);
	props.load(fis);
	fis.close();
	return props;
    }

}

