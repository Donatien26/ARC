FROM maven:3-eclipse-temurin-17 AS build

# Proxies
ARG HTTP_PROXY
ARG HTTPS_PROXY

# path of settings.xml 
ARG MAVEN_SETTINGS

# DB properties
ARG DATABASE_URL
ARG DATABASE_USER
ARG DATABASE_PASSWORD
ARG DATABASE_RESTRICTED_USER
ARG APPLICATION_DIRECTORY
ARG DISABLE_DEBUG_GUI

# Log properties
ARG LOG_SETTINGS=fr/insee/config/log4j2.xml

COPY . /usr/src/app/

# Run a conditional script for the maven build
RUN chmod +x usr/src/app/script.sh && usr/src/app/script.sh

# Kubernetes
FROM bitnami/kubectl:latest as kubectl

# Get a tomcat
FROM tomcat:10-jdk17

# Clean it
RUN rm -rf $CATALINA_HOME/webapps/*

ARG LOG_LEVEL=ERROR
ENV ARC_LOGLEVEL=$LOG_LEVEL


# Copy the war file
COPY --from=build usr/src/app/arc-web/target/*.war $CATALINA_HOME/webapps/ROOT.war
COPY --from=kubectl /opt/bitnami/kubectl/bin/kubectl ./
