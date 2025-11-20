package ru.itmo.calculator.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import ru.itmo.calculator.dto.LiteralOperandValueDto;
import ru.itmo.calculator.dto.OperandDto;
import ru.itmo.calculator.dto.VariableOperandValueDto;

class OperandDeserializer extends StdDeserializer<OperandDto> {

    OperandDeserializer() {
        super(OperandDto.class);
    }

    @Override
    public OperandDto deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_NUMBER_INT) {
            return new LiteralOperandValueDto(parser.getLongValue());
        }
        if (token == JsonToken.VALUE_STRING) {
            return new VariableOperandValueDto(parser.getValueAsString());
        }
        return (OperandDto)
                ctxt.handleUnexpectedToken(
                        OperandDto.class, token, parser, "OperandDto must be an integer literal or variable name");
    }
}
