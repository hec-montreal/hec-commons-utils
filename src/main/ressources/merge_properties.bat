@echo off
for /f "skip=1" %%x in ('wmic os get localdatetime') do if not defined MyDate set "MyDate=%%x"
set MyDate=%MyDate:~0,14%

set newPropertiesFiles="D:\Sources\source_2_9_1\chat\chat-tool\tool\src\bundle\chat_fr_CA.properties"
set updatedValuesOldPropertiesFiles="D:\Sources\source_modif\chat\chat-tool\tool\src\bundle\chat_fr_CA.properties"
set originalValuesOldPropertiesFiles="D:\Sources\source\chat\chat-tool\tool\src\bundle\chat_fr_CA.properties"

set newPropertiesPath=D:\Sources\source_2_9_1
set updatedValuesOldPropertiesPath=D:\Sources\source_modif
set originalValuesOldPropertiesPath=D:\Sources\source

set tool=chat

set pathTool=%updatedValuesOldPropertiesPath%\%tool%

echo %pathTool%

for /R %pathTool% %%a in (*.properties) do echo %%a >> mergeProperties%MyDate% 2>&1



:: java -cp ../hec-utils-with-dependencies.jar ca.hec.commons.utils.MergePropertiesUtils %newPropertiesFiles% %updatedValuesOldPropertiesFiles% %originalValuesOldPropertiesFiles% > mergeProperties%MyDate% 2>&1