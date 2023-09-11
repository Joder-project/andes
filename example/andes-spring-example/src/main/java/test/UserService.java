package test;

import org.springframework.stereotype.Service;

@Service
public class UserService {

    public void lock1(Lock1 lock1, Lock2 lock2) {

    }

    public void lock2(Lock2 lock1, Lock3 lock2) {

    }

    public record Lock1() {

    }

    public record Lock2() {

    }

    public record Lock3() {

    }
}
