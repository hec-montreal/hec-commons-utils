@echo off
for /f "skip=1" %%x in ('wmic os get localdatetime') do if not defined MyDate set "MyDate=%%x"
set MyDate=%MyDate:~0,14%

set mainPropertiesFiles="D:\Sources\source_modif\chat\chat-tool\tool\src\bundle\chat_fr_CA.properties"
set newPropertiesFiles="D:\Sources\source_2_9_1\chat\chat-tool\tool\src\bundle\chat_fr_CA.properties"


java -cp ../hec-utils-with-dependencies.jar ca.hec.commons.utils.MergePropertiesUtils %mainPropertiesFiles% %newPropertiesFiles% > mergeProperties%MyDate% 2>&1