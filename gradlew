#!/bin/sh

# Gradle wrapper script
# Licensed under the Apache License, Version 2.0

# Resolve links
while [ -h "$APP_PATH" ]; do
    APP_PATH="$(readlink "$APP_PATH")"
done
APP_HOME="$(cd "$(dirname "$0")" && pwd)"

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Use JAVA_HOME if set, otherwise java on PATH
if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

exec "$JAVACMD" \
    -Dorg.gradle.appname="gradlew" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
