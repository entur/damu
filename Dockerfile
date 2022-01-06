FROM adoptopenjdk/openjdk11:alpine-jre
RUN apk add --no-cache tini
WORKDIR /deployments
COPY target/damu-*-SNAPSHOT.jar damu.jar
RUN addgroup appuser && adduser --disabled-password appuser --ingroup appuser
USER appuser
CMD [ "/sbin/tini", "--", "java", "-jar", "damu.jar" ]
