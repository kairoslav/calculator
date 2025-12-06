package ru.itmo.calculator.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import ru.itmo.calculator.dto.LiteralOperandValue;
import ru.itmo.calculator.exception.GlobalExceptionHandler;
import ru.itmo.calculator.execution.InstructionExecutionFacade;
import ru.itmo.calculator.openapi.model.ExecuteProgramRequestDto;
import ru.itmo.calculator.openapi.model.OperationDto;
import ru.itmo.calculator.openapi.model.PrintedValueDto;
import ru.itmo.calculator.openapi.model.ExecuteProgramResponseDto;

@WebMvcTest(controllers = CalculatorController.class)
@Import({JacksonConfig.class, GlobalExceptionHandler.class})
class CalculatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InstructionExecutionFacade executionFacade;

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

        ExecuteProgramResponseDto response =
                new ExecuteProgramResponseDto().items(List.of(new PrintedValueDto().var("x").value(3L)));
        when(executionFacade.execute(org.mockito.ArgumentMatchers.any(ExecuteProgramRequestDto.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/executions").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].var").value("x"))
                .andExpect(jsonPath("$.items[0].value").value(3));

        ArgumentCaptor<ExecuteProgramRequestDto> requestCaptor = ArgumentCaptor.forClass(ExecuteProgramRequestDto.class);
        verify(executionFacade).execute(requestCaptor.capture());
        ExecuteProgramRequestDto parsedRequest = requestCaptor.getValue();

        assertEquals(2, parsedRequest.getInstructions().size());
        ru.itmo.calculator.openapi.model.CalcInstructionDto calc =
                (ru.itmo.calculator.openapi.model.CalcInstructionDto) parsedRequest.getInstructions().getFirst();
        assertEquals(OperationDto.PLUS, calc.getOp());
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

        when(executionFacade.execute(org.mockito.ArgumentMatchers.any(ExecuteProgramRequestDto.class)))
                .thenThrow(new IllegalArgumentException("boom"));

        mockMvc.perform(post("/api/v1/executions").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("boom"));
    }
}
