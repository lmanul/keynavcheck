#!/bin/sh

if [ -w "prebuilt/cyborg.jar" ]
then
  echo "cyborg.jar is writeable, recompiling..."
  cd ../cyborg
  ./gradlew assemble && cp build/libs/cyborg.jar ../keynavcheck/prebuilt/cyborg.jar
  cd ../keynavcheck
else
  echo "Using prebuilt cyborg.jar	"
fi
./gradlew assemble && java -jar build/libs/KeyNavCheck.jar
