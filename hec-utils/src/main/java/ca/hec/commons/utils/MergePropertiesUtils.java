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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility to merge properties during a sakai migration. Example:
 * 
 * >java -cp hec-commons-utils/hec-utils/target/hec-utils-with-dependencies.jar newPropertiesFile oldPropertiesFile
 * 
 * The algorithm is :
 * 
 * - Properties in 'new' properties file are updated with value of the
 *   old fr_CA properties file.  Output file will have the same order as 
 *   the original for easy diff
 * - Properties that are missing from either of the files are written to 
 *   a separate file with the same name followed by ".missing"
 * 
 * The utility will create a file alongside the original english properties file
 * with fr_CA added to the name.
 *
 * Note: Currently, the continuation lines (lines ending with \) in 'new' are
 * appended into a single line.
 */
public class MergePropertiesUtils {

	public static void main(String[] args) throws Exception {

		String pathOriginalValuesOldPropertiesFile = args[0]; // file path for old fr_CA file
		String pathNewPropertiesFile = args[1]; // file path for new file

		File newPropertiesFile = new File(pathNewPropertiesFile);
		File originalValuesOldPropertiesFilesPath = new File(pathOriginalValuesOldPropertiesFile);

		Properties originalProps = load(originalValuesOldPropertiesFilesPath);

		/**
		 * Output new file with french values from old version
		 */
		merge(newPropertiesFile, originalProps);
	}

	/**
	 * Merge newProps to mainProps.
	 * 
	 * NOTE: The idea is that we want to write out the properties in exactly the
	 * same order, for future comparison purposes.
	 * 
	 * @param newPropertiesFile
	 * @param newProps
	 * @return nb of properties from main props file.
	 */
	public static void merge(File newPropertiesFile, Properties updatedProps) throws Exception {
		/**
		 * 1) Read line by line of the new PropertiesFile For each line:
		 * extract property check if we have a match one in the old file 
		 * - if no, then rewrite the same line as a comment 
		 * - if yes: rewrite the prop with the new value
		 * 
		 * 2) At the end, write the properties that were not in one or the other
		 */

		try {
			String destinationPath = newPropertiesFile.getParent() + "/";
			String destinationName = newPropertiesFile.getName().replace(".properties", "_fr_CA.properties");

			List<String> outputLines = new ArrayList<String>();
			List<String> missingOutputLines = new ArrayList<String>();

			missingOutputLines.addAll(Arrays.asList(new String[]{"Missing fr_CA translations", ""}));

			// Open the file that is the first
			// command line parameter
			FileInputStream fstream = new FileInputStream(newPropertiesFile);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			LineWithContinuation lineIn;

			// Read File Line By Line
			while ((lineIn = LineWithContinuation.readLineWithContinuation(br)) != null) {

				KeyValue keyValue = extractKeyValue(lineIn.getFullLine());

				// May be a comment line, or blank line, or not a line containing key & property
				// pair (we expect "key = value" line).
				// Simply echo the line back.
				if (keyValue == null) {
					outputLines.add(lineIn.getFullLine());
					continue;
				}

				String key = keyValue.key;
				String newValue = updatedProps.getProperty(key);
				// String valueEscaped = StringEscapeUtils.unescapeJava(keyValue.value);

				if (newValue == null) {
					// if value is missing, print line as is in comment
					outputLines.add("#" + lineIn.getFullLine());
					missingOutputLines.add(lineIn.getFullLine());
				} else {
					// escape java to tranform accented characters
					outputLines.add(composeNewPropLine(key, StringEscapeUtils.escapeJava(newValue)));
				}
				// remove so we can track missing properties from the new file
				updatedProps.remove(key);
			}
			// Close the input stream
			in.close();

			// write output file
			Path outfile = Paths.get(destinationPath + destinationName);
			Files.write(outfile, outputLines, Charset.forName("UTF-8"));

			if (missingOutputLines.size() > 0 || !updatedProps.isEmpty()) {
				missingOutputLines.addAll(Arrays.asList(new String[] {"", "Missing from new version, present in old fr_CA", ""}));
				for (Object k : updatedProps.keySet()) {
					String key = k.toString();
					missingOutputLines.add(composeNewPropLine(key, StringEscapeUtils.escapeJava(updatedProps.getProperty(key))));
				}
				// write missing
				Path missingOutfile = Paths.get(destinationPath + destinationName + ".missing");
				Files.write(missingOutfile, missingOutputLines, Charset.forName("UTF-8"));
			}
		} catch (FileNotFoundException fnfe) {
			System.out.println(newPropertiesFile + " not found");
		} finally {
		}
	}

	/**
	 * Class for reading a line, taking care of the continuation symbol '\' at the
	 * end of the line.
	 */
	static class LineWithContinuation {
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

		public String getLineWithReturn() {
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < linesAsIs.size(); i++) {
				boolean last = (i == linesAsIs.size() - 1);
				buf.append(linesAsIs.get(i) + (last ? "" : "\n"));
			}

			return buf.toString();
		}

		public String getFullLine() {
			return fullLine;
		}
	}

	/**
	 * Compose a line for a property, like 'prop1 = the small dog'.
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	private static String composeNewPropLine(String key, String value) {
		return key + " = " + ((value == null) ? "" : value);
	}

	/**
	 * @param strLine
	 * @return the key extracted. Example returns 'prop1' for input 'prop1 = the
	 *         small dog' Null is returned if the line is empty, or is a comment
	 *         line (i.e. starts with a #)
	 */
	private static KeyValue extractKeyValue(String strLine) {
		String line = strLine.trim();

		if (line.startsWith("#")) {
			return null;
		}

		int indexOfEqual = line.indexOf('=');

		if (indexOfEqual < 0) {
			return null;
		}

		String key = line.substring(0, indexOfEqual);
		String value = line.substring(indexOfEqual + 1);

		return new KeyValue(key.trim(), StringUtils.stripStart(value, null)); // NOTE: Note that value is NOT trimmed to
																				// be similar to Properties.load().
		// return new KeyValue(key.trim(), value.trim()); // NOTE: Note that value is
		// NOT trimmed to be similar to Properties.load().
	}

	/**
	 * ********** Small KeyValue class. **********
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
		try {
			Properties props = new Properties();
			FileInputStream fis = new FileInputStream(propsFile);
			props.load(fis);
			fis.close();
			return props;
		}
		catch (FileNotFoundException e) {
			System.out.println(propsFile.getAbsolutePath() + " not found");
		}

		return null; 
	}
}
