package ru.itmo.calculator.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.calculator.openapi.api.DefaultApi;
import ru.itmo.calculator.openapi.model.ExecuteProgramRequest;
import ru.itmo.calculator.openapi.model.ExecuteProgramResponse;
import ru.itmo.calculator.openapi.model.PrintedValue;

@RestController
public class DefaultApiController implements DefaultApi {

    @Override
    public ExecuteProgramResponse executeProgram(ExecuteProgramRequest executeProgramRequest) {
        PrintedValue placeholder = new PrintedValue().var("result").value(42L);
        return new ExecuteProgramResponse().items(List.of(placeholder));
    }
}
