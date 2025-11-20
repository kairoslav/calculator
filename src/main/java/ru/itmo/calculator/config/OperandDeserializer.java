package ru.itmo.calculator.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import ru.itmo.calculator.dto.LiteralOperandValue;
import ru.itmo.calculator.dto.Operand;
import ru.itmo.calculator.dto.VariableOperandValue;

class OperandDeserializer extends StdDeserializer<Operand> {

    OperandDeserializer() {
        super(Operand.class);
    }

    @Override
    public Operand deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_NUMBER_INT) {
            return new LiteralOperandValue(parser.getLongValue());
        }
        if (token == JsonToken.VALUE_STRING) {
            return new VariableOperandValue(parser.getValueAsString());
        }
        return (Operand)
                ctxt.handleUnexpectedToken(
                        Operand.class, token, parser, "OperandDto must be an integer literal or variable name");
    }
}
