package io.zenwave360.sdk.plugins.eventproducer.events;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

public record CustomerEventRecord(
    String stringField,
    Integer integerWrapper,
    Long longWrapper,
    int intPrimitive,
    long longPrimitive,
    BigDecimal bigDecimalField,
    Float floatWrapper,
    float floatPrimitive,
    Double doubleWrapper,
    double doublePrimitive,
    EnumType enumField,
    Boolean booleanWrapper,
    boolean booleanPrimitive,
    LocalDate localDateField,
    LocalDateTime localDateTimeField,
    ZonedDateTime zonedDateTimeField,
    Instant instantField,
    Duration durationField,
    UUID uuidField,
    byte bytePrimitive,
    byte[] byteArrayField
) {}
