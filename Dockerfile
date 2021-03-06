FROM openjdk:8u131-jre-alpine
RUN apk update && apk add nginx && mkdir /run/nginx/
COPY nginx.conf /etc/nginx/nginx.conf
COPY run.sh /srv/run.sh
COPY update.sh /etc/periodic/hourly/update
RUN mkdir /srv/out && echo "Please wait..." > /srv/out/index.html && mkdir /srv/src && chmod +x /srv/run.sh /etc/periodic/hourly/update
VOLUME /srv/src
EXPOSE 80
CMD [ "/srv/run.sh" ]
COPY target/scala-2.12/git-report.jar /srv/git-report.jar

# docker build -t thstock/gitreport:latest .
# dir=$(pwd) && docker run --name some-git-report -d -p80:80 -v $dir:/srv/src thstock/gitreport:latest
# dir=$(dirname $(pwd)) && docker run --name some-git-report -d -p80:80 -v $dir:/srv/src thstock/gitreport:latest
# dir=$(cygpath -w $(pwd)) && docker run --name some-git-report -d -p80:80 -v $dir:/srv/src thstock/gitreport:latest
# docker push thstock/gitreport

