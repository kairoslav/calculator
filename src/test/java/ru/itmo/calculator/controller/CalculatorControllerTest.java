package ru.itmo.calculator.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.calculator.config.JacksonConfig;
import ru.itmo.calculator.converter.CalculatorApiConverter;
import ru.itmo.calculator.dto.*;
import ru.itmo.calculator.execution.InstructionExecutionService;
import ru.itmo.calculator.openapi.model.ExecuteProgramRequest;
import ru.itmo.calculator.openapi.model.Operation;
import ru.itmo.calculator.openapi.model.PrintedValue;

@WebMvcTest(controllers = CalculatorController.class)
@Import({JacksonConfig.class, GlobalExceptionHandler.class})
class CalculatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InstructionExecutionService executionService;

    @MockBean
    private CalculatorApiConverter converter;

    @Test
    void executesProgramAndReturnsPrintedValues() throws Exception {
        String requestBody =
                """
                {
                  "instructions": [
                    { "type": "calc", "op": "+", "var": "x", "left": 1, "right": 2 },
                    { "type": "print", "var": "x" }
                  ]
                }
                """;

        List<Instruction> domainInstructions =
                List.of(
                        new CalcInstruction("x", ArithmeticOp.ADD, new LiteralOperand(1), new LiteralOperand(2)),
                        new PrintInstruction("x"));
        List<PrintResult> executionResults = List.of(new PrintResult("x", 3));
        List<PrintedValue> responseItems = List.of(new PrintedValue().var("x").value(3L));

        when(converter.toDomainInstructions(any(ExecuteProgramRequest.class))).thenReturn(domainInstructions);
        when(executionService.execute(domainInstructions)).thenReturn(executionResults);
        when(converter.toPrintedValues(executionResults)).thenReturn(responseItems);

        mockMvc.perform(post("/api/v1/executions").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].var").value("x"))
                .andExpect(jsonPath("$.items[0].value").value(3));

        ArgumentCaptor<ExecuteProgramRequest> requestCaptor = ArgumentCaptor.forClass(ExecuteProgramRequest.class);
        verify(converter).toDomainInstructions(requestCaptor.capture());
        ExecuteProgramRequest parsedRequest = requestCaptor.getValue();

        assertEquals(2, parsedRequest.getInstructions().size());
        ru.itmo.calculator.openapi.model.CalcInstructionDto calc =
                (ru.itmo.calculator.openapi.model.CalcInstructionDto) parsedRequest.getInstructions().getFirst();
        assertEquals(Operation.PLUS, calc.getOp());
        assertTrue(calc.getLeft() instanceof LiteralOperandValue);
        assertTrue(parsedRequest.getInstructions().get(1)
                instanceof ru.itmo.calculator.openapi.model.PrintInstructionDto);
    }

    @Test
    void returnsBadRequestWhenValidationFails() throws Exception {
        String invalidBody = """
                { "instructions": [] }
                """;

        mockMvc.perform(post("/api/v1/executions").contentType(MediaType.APPLICATION_JSON).content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details[0]", containsString("size must be")));
    }

    @Test
    void returnsBadRequestWhenExecutionFails() throws Exception {
        String requestBody =
                """
                {
                  "instructions": [
                    { "type": "print", "var": "missing" }
                  ]
                }
                """;

        when(converter.toDomainInstructions(any(ExecuteProgramRequest.class)))
                .thenThrow(new IllegalArgumentException("boom"));

        mockMvc.perform(post("/api/v1/executions").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("boom"));
    }
}
