#!/usr/bin/env sh
DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA_EXE=java
exec "$JAVA_EXE" -Dorg.gradle.appname=gradlew -classpath "$DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
