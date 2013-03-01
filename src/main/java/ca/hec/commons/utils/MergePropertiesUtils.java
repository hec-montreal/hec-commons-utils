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
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
/**
 * Utility to merge properties to an existing properties files.
 * Example:
 * 
 * 	> mergePropertiesHec main-properties-file new-properties-file
 * 
 * Properties in 'new' properties file are merged into 'main'.
 *   - if a property occurs in both, then the new property replaces the main (if they are different).
 *     The line containing that property in the new section is removed.
 *   - properties in one, but not in the other are unchanged.
 *   
 * The utility will send output to stdout, which you can copy and save in a properties file.
 * This tool is useful if the properties files contain mainly the same properties, but
 * the order of them are very different, making a visual merge is impossible.
 *
 *                                     +-
 *                                     |
 *   Main properties                   | Main props
 *                  `-.                |
 *                     `-._            +-
 *                       .'  Merged:
 *                     .'              +-
 *                   .'                |
 *    New properties                   | New props
 *                                     |
 *                                     +-
 *
 *  NOte: Currently, the continuation lines (lines ending with \) 
 *        in 'new' are appended into a single line.*/
public class MergePropertiesUtils {
    
public static void main(String[] args) throws Exception {
	
	String mainPropertiesFiles = args[0];
	String newPropertiesFiles = args[1];

	File newPropsFile = new File(newPropertiesFiles);
	Properties newProps = load(newPropsFile);
	int nbNewProperties = newProps.size();
	
	merge(new File(mainPropertiesFiles), newProps);
	
	/**
	 * Now we print out values not used from the newProps
	 */
	System.out.println();
	System.out.println("####################### Properties from 2.7.1-source-modif #######################");
	
	int nbPropsRemaining = newProps.size();
	System.out.println("### " + nbNewProperties + " properties in 2.7.1-source-modif (" + (nbNewProperties - nbPropsRemaining) + " merged (with same or different value), " + nbPropsRemaining + " remaining)\n");
	
	printNewProps(newPropsFile, newProps);
    }

    /**
     * Merge newProps to mainProps.

     * NOTE: The idea is that we want to write out the properties in exactly the same order, for future comparison purposes.
     * 
     * @param mainPropFile
     * @param newProps
     * @return nb of properties from main props file.
     */
    public static void merge(File mainPropFile, Properties newProps) throws Exception {
	/**
	 * 1) Read line by line of the mainProp 
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
	    FileInputStream fstream = new FileInputStream(mainPropFile);
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
		
		String newValue = newProps.getProperty(key);
		String valueEscaped = unescapeJava(keyValue.value);
		
		if (newValue != null) {
		    if (!newValue.equals(valueEscaped)) {
			String newLine = composeNewPropLine(key, newValue);
			System.out.println(escapeAccentedToUnicode(newLine));
			nbPropsChanged++;
		    } else {
			System.out.println(escapeAccentedToUnicode(lineIn.getLineWithReturn()));
			nbPropsSimilar++;
		    }
		    
		    // remove the key from newProps because it is used
		    newProps.remove(key);
		} else {
		    System.out.println(lineIn.getLineWithReturn());
		    nbPropsNotInNew++;
		}
	    }
	    
	    // Close the input stream
	    in.close();
	    
	    System.out.println("\n\n### " + nbPropsInMain + " properties in 2.8.1 (" + nbPropsChanged + " changed, " + nbPropsSimilar + " props with same value in both versions, " + nbPropsNotInNew + " not in 2.7.1)");

	    
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
    
    private static void printNewProps(File newPropFile, Properties remainingNewProps) {
	/**
	 * Read new props line by line.
	 * For each line, extract the prop key
	 *   if it is not in the remaining, then it is used. So we write the line, but commented out
	 *   otherwise, write the line as is.
	 */
	try {
	    // Open the file that is the first
	    // command line parameter
	    FileInputStream fstream = new FileInputStream(newPropFile);
	    // Get the object of DataInputStream
	    DataInputStream in = new DataInputStream(fstream);
	    BufferedReader br = new BufferedReader(new InputStreamReader(in));
	    String strLine;
	    
	    // Read File Line By Line
	    while ((strLine = br.readLine()) != null) {

		KeyValue keyValue = extractKeyValue(strLine);
		
		if (keyValue == null) {
		    System.out.println(strLine);
		    continue;
		}
		
		String key = keyValue.key;
		
		boolean isUsed = (remainingNewProps.get(key) == null);
		
		// Write out line only if property is not used
		if (!isUsed) {
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
     * Code from http://www.javapractices.com/topic/TopicAction.do?Id=96.
     * Example: "Notes envoyÃ©es" -to-> "Notes envoy\u00E9es"
     * @param aText
     * @return
     */
    public static String escapeAccentedToUnicode(String aText) {

	final StringBuilder result = new StringBuilder();
	final StringCharacterIterator iterator = new StringCharacterIterator(aText);
	char character = iterator.current();
	
	while (character != CharacterIterator.DONE) {
	    String uchar = UnicodePair.toUnicode(Character.toString(character));
	      
	    // the char is not a special one
	    // add it to the result as is
	    result.append(uchar != null ? uchar : character);
	    
	    character = iterator.next();
	}
	
	String resultStr = result.toString();
	return resultStr;
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
     * Mappe les characteres avec accent a leur equivalent Unicode.
     */
    
    /**
     * Conversion 
     */
    private static class UnicodePair {
	/**
	 * Unicode map is from http://mess.genezys.net/unicode/
	 */
	private static UnicodePair[] unicodemap = {
	    new UnicodePair("Ã€", "\\u00C0"), new UnicodePair("Ã?", "\\u00C1"), new UnicodePair("Ã‚", "\\u00C2"), new UnicodePair("Ãƒ", "\\u00C3"), 
	    new UnicodePair("Ã„", "\\u00C4"), new UnicodePair("Ã…", "\\u00C5"), new UnicodePair("Ã†", "\\u00C6"), new UnicodePair("Ã‡", "\\u00C7"), 
	    new UnicodePair("Ãˆ", "\\u00C8"), new UnicodePair("Ã‰", "\\u00C9"), new UnicodePair("ÃŠ", "\\u00CA"), new UnicodePair("Ã‹", "\\u00CB"), 
	    new UnicodePair("ÃŒ", "\\u00CC"), new UnicodePair("Ã?", "\\u00CD"), new UnicodePair("ÃŽ", "\\u00CE"), new UnicodePair("Ã?", "\\u00CF"),
	    
	    new UnicodePair("Ã?", "\\u00D0"), new UnicodePair("Ã‘", "\\u00D1"), new UnicodePair("Ã’", "\\u00D2"), new UnicodePair("Ã“", "\\u00D3"), 
	    new UnicodePair("Ã”", "\\u00D4"), new UnicodePair("Ã•", "\\u00D5"), new UnicodePair("Ã–", "\\u00D6"), new UnicodePair("Ã—", "\\u00D7"), 
	    new UnicodePair("Ã˜", "\\u00D8"), new UnicodePair("Ã™", "\\u00D9"), new UnicodePair("Ãš", "\\u00DA"), new UnicodePair("Ã›", "\\u00DB"), 
	    new UnicodePair("Ãœ", "\\u00DC"), new UnicodePair("Ã?", "\\u00DD"), new UnicodePair("Ãž", "\\u00DE"), new UnicodePair("ÃŸ", "\\u00DF"),
	    
	    new UnicodePair("Ã ", "\\u00E0"), new UnicodePair("Ã¡", "\\u00E1"), new UnicodePair("Ã¢", "\\u00E2"), new UnicodePair("Ã£", "\\u00E3"),
	    new UnicodePair("Ã¤", "\\u00E4"), new UnicodePair("Ã¥", "\\u00E5"), new UnicodePair("Ã¦", "\\u00E6"), new UnicodePair("Ã§", "\\u00E7"),
	    new UnicodePair("Ã¨", "\\u00E8"), new UnicodePair("Ã©", "\\u00E9"), new UnicodePair("Ãª", "\\u00EA"), new UnicodePair("Ã«", "\\u00EB"),
	    new UnicodePair("Ã¬", "\\u00EC"), new UnicodePair("Ã­", "\\u00ED"), new UnicodePair("Ã®", "\\u00EE"), new UnicodePair("Ã¯", "\\u00EF"),
	    
	    new UnicodePair("Ã°", "\\u00F0"), new UnicodePair("Ã±", "\\u00F1"), new UnicodePair("Ã²", "\\u00F2"), new UnicodePair("Ã³", "\\u00F3"),
	    new UnicodePair("Ã´", "\\u00F4"), new UnicodePair("Ãµ", "\\u00F5"), new UnicodePair("Ã¶", "\\u00F6"), new UnicodePair("Ã·", "\\u00F7"),
	    new UnicodePair("Ã¸", "\\u00F8"), new UnicodePair("Ã¹", "\\u00F9"), new UnicodePair("Ãº", "\\u00FA"), new UnicodePair("Ã»", "\\u00FB"),
	    new UnicodePair("Ã¼", "\\u00FC"), new UnicodePair("Ã½", "\\u00FD"), new UnicodePair("Ã¾", "\\u00FE"), new UnicodePair("Ã¿", "\\u00FF"),
	    
	    new UnicodePair("Â¿", "\\u00BF"), new UnicodePair("Â©", "\\u00A9"), new UnicodePair("Â®", "\\u00AE")
	     	    
	};
		
	private String ch;
	private String unicode;
	private UnicodePair(String ch, String unicode) {
	    this.ch = ch;
	    this.unicode = unicode;
	}
	
	/**
	 * @return character converted to unicode string if there is a match. Otherwise return null.
	 */
	public static String toUnicode(String ch) {
	    for (UnicodePair upair: unicodemap) {
		if (ch == upair.ch) {
		    return upair.unicode;
		}
	    }
	    
	    return null;
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

