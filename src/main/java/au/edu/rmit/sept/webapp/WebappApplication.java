package au.edu.rmit.sept.webapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication  
public class WebappApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebappApplication.class, args); // Test
	}
    // test running with -DskipTests flag
    // use this instead: 
    // mvn clean install -Dmaven.test.skip=true
    // and for launch:
    // mvn clean spring-boot:run -Dmaven.test.skip=true
}