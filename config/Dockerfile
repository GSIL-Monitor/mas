############################################################
# Dockerfile to build letv-mas-config Installed Containers
# Based on Centos
# Clean up context:
#docker ps -a | grep "Exited" | awk '{print $1 }'|xargs docker rm;
#docker images | grep "<none>" | awk '{print $3 }' | xargs docker rmi;
# Remove container:
#if docker stop letv-mas-config; then docker rm letv-mas-config; fi
# Remove image:
#docker rmi -f letv-mas-config:1.0
# Build image:
#[local]: docker build -t letv-mas-config:1.0 -f ./config/Dockerfile ./config
#[test/pre/prod]: docker build -t letv-mas-config:1.0 -f /letv/deploy/letv-mas-config/Dockerfile /letv/deploy/letv-mas-config
# Run image:
#[local]: docker run -it -d -v /letv/logs/mas/config:/letv/logs/mas/config -p 8011:8010 --restart=always --name letv-mas-config letv-mas-config:1.0 sh -c "java -jar /letv/app/mas/config/config-1.0-SNAPSHOT.jar"
#[pre/prod]: docker run -it -d -v /letv/logs/tomcat/iptv:/letv/logs/tomcat/iptv --net=host --restart=always --name letv-mas-config letv-mas-config:1.0 sh -c "java -jar /letv/app/mas/config/config-1.0-SNAPSHOT.jar"
# Enter the container on this image:
#docker exec -it letv-mas-config /bin/bash
# Check host net:
#route |grep default | awk '{print $2 }'
############################################################
FROM reg-sre.lecloud.com/test_image/letv-mas-base

# File Author / Maintainer
MAINTAINER dengliwei dengliwei@le.com

# Expose ports
#EXPOSE 8010

# create mw and app dir
RUN mkdir -p /letv/app/mas/config
# create the dir linking with external volume!!!
RUN mkdir -p /letv/logs/mas/config

# create the dir linking with external volume
VOLUME ["/letv/app/mas/config", "/letv/logs/mas/config"]

WORKDIR /letv/app/mas/config

# set environmental vars
#ENV CONFIG_SERVER_IP=127.0.0.1
#ENV CONFIG_SERVER_PORT=8021
#ENV EUREKA_SERVER_LIST=http://localhost:8010/eureka/

# Set the default command to execute
# when creating a new container
CMD [ "/bin/bash", "-c", "java -jar /letv/app/mas/config-1.0-SNAPSHOT.jar" ]

COPY ./target/config-1.0-SNAPSHOT.jar /letv/app/mas/