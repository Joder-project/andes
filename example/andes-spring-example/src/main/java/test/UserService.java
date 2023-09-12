package test;

import jakarta.annotation.PostConstruct;
import org.andes.lock.core.AutoLock;
import org.andes.lock.spring.LockClass;
import org.springframework.stereotype.Service;

@Service
@LockClass
public class UserService {

    @PostConstruct
    void init() {
        var lock1 = new Lock1();
        var lock2 = new Lock2();
        var lock3 = new Lock3();
        lock1(lock1, lock2);
        lock2(lock2, lock3);
    }

    @AutoLock
    public void lock1(Lock1 lock1, Lock2 lock2) {

    }

    @AutoLock
    public void lock2(Lock2 lock1, Lock3 lock2) {

    }

//    @AutoLock // error
//    public void lock3(Lock2 lock1, Lock1 lock2) {
//
//    }

    public record Lock1() {

    }

    public record Lock2() {

    }

    public record Lock3() {

    }
}
