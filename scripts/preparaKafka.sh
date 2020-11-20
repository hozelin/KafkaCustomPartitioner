docker run -d -p 2181:2181 -p 3030:3030 -p 8081-8083:8081-8083 -p 9581-9585:9581-9585 -p 9092:9092 --name=kafka  -e ADV_HOST=127.0.0.1 landoop/fast-data-dev:latest
sleep 5
docker exec -ti kafka bash -c "/opt/landoop/kafka/bin/kafka-topics --create --bootstrap-server localhost:9092 --topic pedidos --partitions 4 --config retention.ms=120000"