FROM aldrinleal/base-dropwizard-docker

RUN mkdir /app /app/log
ADD . /app
RUN chmod 755 /app/bin/app.sh

ADD supervisord.conf /etc/supervisor/conf.d/supervisord.conf

CMD [ "/usr/bin/supervisord" ]

EXPOSE 8080
