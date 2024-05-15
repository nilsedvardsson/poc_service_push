docker run -d --name my-redis -p 6379:6379 redis

cd poc_service_push
mvn clean install
docker build -t push-server .
docker run -p 8080:8080 -e NAME='server1' push-server
docker run -p 8081:8080 -e NAME='server2' push-server

cd poc_service_push/lb
docker build -t lb .
docker run -p 80:80 --name lb lb

http://localhost/subscriber/{identifier}?lookbackseconds=3600
http://localhost/push/{identifier}/{message}

For high availability subscribe with retry:

http://localhost/kurt.html (open up developer console and monitor messages)
http://localhost/stina.html 