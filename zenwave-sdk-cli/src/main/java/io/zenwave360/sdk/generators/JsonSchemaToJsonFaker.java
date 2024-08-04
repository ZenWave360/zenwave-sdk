package io.zenwave360.sdk.generators;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.javafaker.Faker;
import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;

import java.text.SimpleDateFormat;
import java.util.*;

public class JsonSchemaToJsonFaker {

    private static final Random random = new Random();

    private final Faker faker;
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public JsonSchemaToJsonFaker() {
        this(Locale.ENGLISH);
    }

    public JsonSchemaToJsonFaker(Locale locale) {
        if (locale == null) {
            locale = Locale.ENGLISH;
        }
        faker = new Faker(locale);
    }

    /**
     * Returns either a Map or a List of Maps that represents a JSON object.
     *
     * @param schema
     * @return
     */
    public Object generateExample(Map<String, Object> schema) {
        return generateObject(null, schema);
    }

    public String generateExampleAsJson(Map<String, Object> schema) {
        var jsonExample = generateExample(schema);
        try {
            return asJson(jsonExample);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String asJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }


    protected Object generateObject(String propertyName, Map<String, Object> schema) {
        var example = new LinkedHashMap<String, Object>();
        Map<String, Map<String, Object>> properties = (Map) schema.get("properties");
        if (properties != null) {
            for (var keyValue : properties.entrySet()) {
                example.put(keyValue.getKey(), generateValue(keyValue.getKey(), keyValue.getValue()));
            }
        } else {
            return generateValue(propertyName, schema);
        }

        return example;
    }

    protected Object generateValue(String propertyName, Map<String, Object> schemaNode) {
        String type = (String) schemaNode.get("type");

        return switch (type) {
            case "string" -> generateString(propertyName, schemaNode);
            case "number", "integer" -> generateNumber(schemaNode);
            case "boolean" -> random.nextBoolean();
            case "array" -> generateArray(propertyName, schemaNode);
            case "object" -> generateObject(propertyName, schemaNode);
            default -> null;
        };
    }

    protected String generateString(String propertyName, Map<String, Object> schemaNode) {
        if (schemaNode.containsKey("example")) {
            return (String) schemaNode.get("example");
        }
        var format = (String) schemaNode.get("format");
        if (format != null && format.equals("date")) {
            return dateFormatter.format(new Date());
        }
        if (format != null && format.equals("date-time")) {
            return dateTimeFormatter.format(new Date());
        }
        if (schemaNode.containsKey("enum")) {
            List<String> enums = (List<String>) schemaNode.get("enum");
            int index = random.nextInt(enums.size());
            return enums.get(index);
        }
        Integer min = asInteger(schemaNode.get("min-length"));
        Integer max = asInteger(schemaNode.get("max-length"));

        return randomString(propertyName, (String) schemaNode.get("pattern"), min, max);
    }


    protected String randomString(String propertyName, String pattern, Integer min, Integer max) {
        String result;
        String lowerPropertyName = propertyName != null? propertyName.toLowerCase() : "";
        if(min == null) {
            min = 1;
        }
        if(max == null) {
            max = min + 25;
        }

        if (lowerPropertyName.equals("firstname") || lowerPropertyName.equals("first_name")) {
            result = faker.name().firstName();
        } else if (lowerPropertyName.equals("lastname") || lowerPropertyName.equals("last_name")) {
            result = faker.name().lastName();
        } else if (lowerPropertyName.equals("fullname") || lowerPropertyName.equals("full_name")) {
            result = faker.name().fullName();
        } else if (lowerPropertyName.contains("email")) {
            result = faker.internet().emailAddress();
        } else if (lowerPropertyName.equals("phone") || lowerPropertyName.equals("phonenumber") || lowerPropertyName.equals("phone_number")) {
            result = faker.phoneNumber().phoneNumber();
        } else if (lowerPropertyName.equals("address")) {
            result = faker.address().fullAddress();
        } else if (lowerPropertyName.equals("city")) {
            result = faker.address().city();
        } else if (lowerPropertyName.equals("country")) {
            result = faker.address().country();
        } else if (lowerPropertyName.equals("company")) {
            result = faker.company().name();
        } else if (lowerPropertyName.equals("job") || lowerPropertyName.equals("jobtitle") || lowerPropertyName.equals("job_title")) {
            result = faker.job().title();
        } else if (pattern != null) {
            try {
                result = faker.regexify(pattern);
            } catch (Error e) {
                result = "BAD REGEX" + "-" + faker.lorem().characters(min, max);
            }
        } else {
            result = propertyName + "-" + faker.lorem().characters(min, max);
        }

        // Ensure the result meets the minimum length requirement
        while (result.length() < min) {
            result += " " + faker.lorem().word();
        }

        // Trim the result to meet the maximum length requirement
        if (result.length() > max) {
            result = result.substring(0, max);
        }

        return result;
    }
    protected Number generateNumber(Map<String, Object> schemaNode) {
        if (schemaNode.containsKey("minimum") && schemaNode.containsKey("maximum")) {
            double min = asDouble(schemaNode.get("minimum"));
            double max = asDouble(schemaNode.get("maximum"));
            return min + (max - min) * random.nextDouble();
        }
        return random.nextInt(100);
    }

    protected List generateArray(String propertyName, Map<String, Object> schemaNode) {
        var list = new ArrayList();
        Map<String, Object> itemsSchema = (Map) schemaNode.get("items");
        int minItems = schemaNode.containsKey("minItems") ? asInteger(schemaNode.get("minItems")) : 1;
        int maxItems = schemaNode.containsKey("maxItems") ? asInteger(schemaNode.get("maxItems")) : 1;
        int itemCount = minItems + random.nextInt(maxItems - minItems + 1);

        for (int i = 0; i < itemCount; i++) {
            list.add(generateValue(propertyName, itemsSchema));
        }

        return list;
    }

    private Double asDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            return Double.valueOf((String) value);
        }
        return null;
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            return Integer.valueOf((String) value);
        }
        return null;
    }
}
