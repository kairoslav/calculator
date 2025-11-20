package ru.itmo.calculator.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;
import ru.itmo.calculator.openapi.api.CalculatorApi;
import ru.itmo.calculator.openapi.model.ExecuteProgramRequest;
import ru.itmo.calculator.openapi.model.ExecuteProgramResponse;
import ru.itmo.calculator.openapi.model.PrintedValue;

@RestController
public class CalculatorController implements CalculatorApi {

    @Override
    public ExecuteProgramResponse executeProgram(ExecuteProgramRequest executeProgramRequest) {
        PrintedValue placeholder = new PrintedValue().var("result").value(42L);
        return new ExecuteProgramResponse().items(List.of(placeholder));
    }
}
