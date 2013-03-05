::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
::::::::::::::::::::::          SET THESE PROPERTIES BEFORE LAUNCHING BATCH FILE   :::::::::::::::::::::::::::::::::::::::::::::::::
set newPropertiesPath=D:\Sources\source_2_9_1
set updatedValuesOldPropertiesPath=D:\Sources\source_modif
set originalValuesOldPropertiesPath=D:\Sources\source
set updatedValuesNewPropertiesPath=D:\Sources\source_2_9_1\modif
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
   SETLOCAL EnableDelayedExpansion
  ECHO %updatedValuesNewPropertiesPath%!p:%updatedValuesOldPropertiesPath%=!
  java -cp ../hec-utils-with-dependencies.jar ca.hec.commons.utils.MergePropertiesUtils %newPropertiesPath%!p:%updatedValuesOldPropertiesPath%=! %updatedValuesOldPropertiesPath%!p:%updatedValuesOldPropertiesPath%=! %originalValuesOldPropertiesPath%!p:%updatedValuesOldPropertiesPath%=! > "%updatedValuesNewPropertiesPath%!p:%updatedValuesOldPropertiesPath%=!" 2>&1
  ENDLOCAL
) >> mergeProperties%MyDate%