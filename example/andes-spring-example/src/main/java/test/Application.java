package test;

import org.andes.lock.spring.EnableAutoLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAutoLock
public class Application {

    private final UserService userService;

    public Application(UserService userService) {
        this.userService = userService;
    }


    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
