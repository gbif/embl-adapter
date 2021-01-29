mvn clean package

java -jar target/embl-adapter.jar embl-adapter --log-config src/main/resources/logback.xml --conf src/main/resources/config.yaml
