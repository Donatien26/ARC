FROM maven:3-eclipse-temurin-17 AS build

# Proxies
ARG HTTP_PROXY
ARG HTTPS_PROXY

# path of settings.xml 
ARG MAVEN_SETTINGS

# DB properties
ARG LOG_SETTINGS
ARG LOG_LEVEL
ARG DATABASE_URL
ARG DATABASE_USER
ARG DATABASE_PASSWORD
ARG DATABASE_RESTRICTED_USER
ARG APPLICATION_DIRECTORY
ARG DISABLE_DEBUG_GUI
ARG KUBERNETES_API_URI
ARG KUBERNETES_API_NAMESPACE
ARG KUBERNETES_API_TOKEN_PATH
ARG KUBERNETES_API_TOKEN_VALUE
ARG KUBERNETES_EXECUTOR_NUMBER
ARG KUBERNETES_EXECUTOR_LABEL
ARG KUBERNETES_EXECUTOR_USER
ARG KUBERNETES_EXECUTOR_DATABASE
ARG KUBERNETES_EXECUTOR_PORT
ARG S3_INPUT_API_URI
ARG S3_INPUT_BUCKET
ARG S3_INPUT_ACCESS
ARG S3_INPUT_SECRET
ARG S3_OUTPUT_API_URI
ARG S3_OUTPUT_BUCKET
ARG S3_OUTPUT_ACCESS
ARG S3_OUTPUT_SECRET

# Log properties
COPY . /usr/src/app/

# Run a conditional script for the maven build
RUN chmod +x usr/src/app/script.sh && usr/src/app/script.sh

#
ENV ARC_LOGLEVEL=$LOG_LEVEL

## start ArcMain.jar
# CMD ["java","-jar","usr/src/app/arc-batch/target/ArcMain.jar"]
