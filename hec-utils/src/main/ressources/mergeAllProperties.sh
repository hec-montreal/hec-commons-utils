#!/bin/bash

OLD_SAKAI_DIR=$1
OLD_DIR_LEN=${#OLD_SAKAI_DIR}
NEW_SAKAI_DIR=$2

# find all fr_ca files to merge, ignore "target" directories
FILES=$(find $OLD_SAKAI_DIR -name "*_fr_CA.properties" | grep -v "target")

count=0
for item in $FILES
do 
	printf "\n"
	NEW_FILENAME=${item%_fr_CA.properties}.properties
	NEW_PATH=$NEW_SAKAI_DIR${NEW_FILENAME:$OLD_DIR_LEN}
	DEST_PATH=$NEW_SAKAI_DIR${item:$OLD_DIR_LEN}
	
	echo "Merging $item"

	# check that we can find the new english file before attempting merge
	if ! [ -f "$NEW_PATH" ]
	then
		echo "ERROR: $NEW_PATH not found."
		continue
	fi

	# check fr_CA file does not exist
	if [ -f "$DEST_PATH" ]
	then
		echo "ERROR: $DEST_PATH already exists."
		continue
	fi

	$(java -cp "$NEW_SAKAI_DIR/hec-commons-utils/hec-utils/target/hec-utils-with-dependencies.jar" ca.hec.commons.utils.MergePropertiesUtils "$item" "$NEW_PATH")
done


