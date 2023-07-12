mvn clean package

java -jar target/embl-adapter.jar ena-taxonomy --log-config src/main/resources/logback.xml --conf src/main/resources/taxonomy-config.yaml
