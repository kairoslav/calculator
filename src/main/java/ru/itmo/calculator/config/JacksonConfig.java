package ru.itmo.calculator.config;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.itmo.calculator.openapi.model.Operand;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer operandDeserializerCustomizer() {
        return builder -> builder.deserializerByType(Operand.class, new OperandDeserializer());
    }
}
