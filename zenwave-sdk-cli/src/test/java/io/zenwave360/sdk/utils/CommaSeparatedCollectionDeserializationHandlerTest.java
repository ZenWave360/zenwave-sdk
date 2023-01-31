package io.zenwave360.sdk.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class CommaSeparatedCollectionDeserializationHandlerTest {

    @Test
    public void testCommaSeparatedCollectionDeserializationHandler() throws JsonProcessingException {
        String json = "{\"list\":\"a,b,c\",\"array\":\"a,b,c\",\"pojoList\":[{\"name\":\"a\"},{\"name\":\"b\"},{\"name\":\"c\"}],\"pojoArray\":[{\"name\":\"a\"},{\"name\":\"b\"},{\"name\":\"c\"}]}";
        Map jsonMap = new ObjectMapper().readValue(json, Map.class);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.addHandler(new CommaSeparatedCollectionDeserializationHandler());

        CollectionHolder holder = new CollectionHolder();
        mapper.updateValue(holder, jsonMap);

        Assertions.assertTrue(holder.list.contains("a"));

    }

    @Test
    public void testCommaSeparatedCollectionDeserializationHandler2() throws JsonProcessingException {
        String json = "{\"name\":\"a\"}";

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.addHandler(new CommaSeparatedCollectionDeserializationHandler());

        var pojo = mapper.readValue(json, POJO.class);
        Assertions.assertEquals("a", pojo.name);
    }

    static class CollectionHolder {
        List<String> list;
        String[] array;

        List<POJO> pojoList;

        POJO[] pojoArray;

        public List<String> getList() {
            return list;
        }

        public void setList(List<String> list) {
            this.list = list;
        }

        public String[] getArray() {
            return array;
        }

        public void setArray(String[] array) {
            this.array = array;
        }

        public List<POJO> getPojoList() {
            return pojoList;
        }

        public void setPojoList(List<POJO> pojoList) {
            this.pojoList = pojoList;
        }

        public POJO[] getPojoArray() {
            return pojoArray;
        }

        public void setPojoArray(POJO[] pojoArray) {
            this.pojoArray = pojoArray;
        }
    }

    static class POJO {
        String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
