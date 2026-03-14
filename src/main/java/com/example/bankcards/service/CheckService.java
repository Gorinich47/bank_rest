package com.example.bankcards.service;

import com.example.bankcards.util.FieldChecker;
import org.springframework.stereotype.Service;

@Service
public class CheckService {

    private final FieldChecker checker;

    CheckService() {
        this.checker = FieldChecker.defaultChecker();
    }

    public void checkFields(Object objectDto){
        checker.check(objectDto);
    }

}
