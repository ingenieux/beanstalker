FROM ingenieux/alpine-java8-supervisord:jdk-8u60-b27

RUN mkdir /app /app/log
ADD . /app
RUN chmod 755 /app/bin/app.sh

ADD supervisord.conf /etc/supervisor/conf.d/supervisord.conf

CMD [ "/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf" ]

EXPOSE 8080
