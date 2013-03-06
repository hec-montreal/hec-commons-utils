::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
::::::::::::::::::::::          SET THESE PROPERTIES BEFORE LAUNCHING BATCH FILE   :::::::::::::::::::::::::::::::::::::::::::::::::
set newPropertiesPath=D:\Sakai_2_9_1\source
set originalValuesOldPropertiesPath=D:\Sakai_2_8_1\source
set updatedValuesOldPropertiesPath=D:\Sakai_2_8_1\source_modif
set updatedValuesNewPropertiesPath=D:\Sakai_2_9_1\source_modif
set tool=chat
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::




@echo off
for /f "skip=1" %%x in ('wmic os get localdatetime') do if not defined MyDate set "MyDate=%%x"
set MyDate=%MyDate:~0,14%
set pathTool=%updatedValuesOldPropertiesPath%\%tool%

echo %pathTool%
REM Display relative file path

SETLOCAL DisableDelayedExpansion

for /R %pathTool% %%a in (*.properties) do (
  SET "p=%%a"
  SET "q=%%~dpa"
   SETLOCAL EnableDelayedExpansion
  ECHO %updatedValuesNewPropertiesPath%!p:%updatedValuesOldPropertiesPath%=!
  mkdir %updatedValuesNewPropertiesPath%!q:%updatedValuesOldPropertiesPath%=!
  java -cp ../hec-utils-with-dependencies.jar ca.hec.commons.utils.MergePropertiesUtils %newPropertiesPath%!p:%updatedValuesOldPropertiesPath%=! %updatedValuesOldPropertiesPath%!p:%updatedValuesOldPropertiesPath%=! %originalValuesOldPropertiesPath%!p:%updatedValuesOldPropertiesPath%=! > "%updatedValuesNewPropertiesPath%!p:%updatedValuesOldPropertiesPath%=!" 2>&1
  ENDLOCAL
) >> mergeProperties%MyDate%