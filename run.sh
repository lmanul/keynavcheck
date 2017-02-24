#!/bin/sh

if [ -w "prebuilt/cyborg.jar" ]
then
  echo "cyborg.jar is writeable, recompiling..."
  cd ../cyborg
  gradle assemble && cp build/libs/cyborg.jar ../KeyNavCheck/prebuilt/cyborg.jar
  cd ../KeyNavCheck
else
  echo "Using prebuilt cyborg.jar	"
fi
gradle assemble && java -jar build/libs/KeyNavCheck.jar
