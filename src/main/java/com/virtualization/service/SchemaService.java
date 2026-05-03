package com.virtualization.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class SchemaService {
    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

    public boolean validate(String json, String schemaContent) {
        if (schemaContent == null || schemaContent.isEmpty()) return true;
        try {
            JsonNode node = mapper.readTree(json);
            JsonSchema schema = factory.getSchema(schemaContent);
            Set<ValidationMessage> errors = schema.validate(node);
            return errors.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
