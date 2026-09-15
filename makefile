
.PHONY:  backend  clean

backend:
	clear  && ./mvnw spring-boot:run

clean:
	clear  && ./mvnw clean