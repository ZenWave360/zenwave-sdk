package io.zenwave360.sdk.plugins.eventproducer.events;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

public class CustomerEvent {
    private String stringField;
    private Integer integerWrapper;
    private Long longWrapper;
    private int intPrimitive;
    private long longPrimitive;
    private BigDecimal bigDecimalField;
    private Float floatWrapper;
    private float floatPrimitive;
    private Double doubleWrapper;
    private double doublePrimitive;
    private EnumType enumField;
    private Boolean booleanWrapper;
    private boolean booleanPrimitive;
    private LocalDate localDateField;
    private LocalDateTime localDateTimeField;
    private ZonedDateTime zonedDateTimeField;
    private Instant instantField;
    private Duration durationField;
    private UUID uuidField;
    private byte bytePrimitive;
    private byte[] byteArrayField;

    public String getStringField() {
        return stringField;
    }

    public void setStringField(String stringField) {
        this.stringField = stringField;
    }

    public Integer getIntegerWrapper() {
        return integerWrapper;
    }

    public void setIntegerWrapper(Integer integerWrapper) {
        this.integerWrapper = integerWrapper;
    }

    public Long getLongWrapper() {
        return longWrapper;
    }

    public void setLongWrapper(Long longWrapper) {
        this.longWrapper = longWrapper;
    }

    public int getIntPrimitive() {
        return intPrimitive;
    }

    public void setIntPrimitive(int intPrimitive) {
        this.intPrimitive = intPrimitive;
    }

    public long getLongPrimitive() {
        return longPrimitive;
    }

    public void setLongPrimitive(long longPrimitive) {
        this.longPrimitive = longPrimitive;
    }

    public BigDecimal getBigDecimalField() {
        return bigDecimalField;
    }

    public void setBigDecimalField(BigDecimal bigDecimalField) {
        this.bigDecimalField = bigDecimalField;
    }

    public Float getFloatWrapper() {
        return floatWrapper;
    }

    public void setFloatWrapper(Float floatWrapper) {
        this.floatWrapper = floatWrapper;
    }

    public float getFloatPrimitive() {
        return floatPrimitive;
    }

    public void setFloatPrimitive(float floatPrimitive) {
        this.floatPrimitive = floatPrimitive;
    }

    public Double getDoubleWrapper() {
        return doubleWrapper;
    }

    public void setDoubleWrapper(Double doubleWrapper) {
        this.doubleWrapper = doubleWrapper;
    }

    public double getDoublePrimitive() {
        return doublePrimitive;
    }

    public void setDoublePrimitive(double doublePrimitive) {
        this.doublePrimitive = doublePrimitive;
    }

    public EnumType getEnumField() {
        return enumField;
    }

    public void setEnumField(EnumType enumField) {
        this.enumField = enumField;
    }

    public Boolean getBooleanWrapper() {
        return booleanWrapper;
    }

    public void setBooleanWrapper(Boolean booleanWrapper) {
        this.booleanWrapper = booleanWrapper;
    }

    public boolean isBooleanPrimitive() {
        return booleanPrimitive;
    }

    public void setBooleanPrimitive(boolean booleanPrimitive) {
        this.booleanPrimitive = booleanPrimitive;
    }

    public LocalDate getLocalDateField() {
        return localDateField;
    }

    public void setLocalDateField(LocalDate localDateField) {
        this.localDateField = localDateField;
    }

    public LocalDateTime getLocalDateTimeField() {
        return localDateTimeField;
    }

    public void setLocalDateTimeField(LocalDateTime localDateTimeField) {
        this.localDateTimeField = localDateTimeField;
    }

    public ZonedDateTime getZonedDateTimeField() {
        return zonedDateTimeField;
    }

    public void setZonedDateTimeField(ZonedDateTime zonedDateTimeField) {
        this.zonedDateTimeField = zonedDateTimeField;
    }

    public Instant getInstantField() {
        return instantField;
    }

    public void setInstantField(Instant instantField) {
        this.instantField = instantField;
    }

    public Duration getDurationField() {
        return durationField;
    }

    public void setDurationField(Duration durationField) {
        this.durationField = durationField;
    }

    public UUID getUuidField() {
        return uuidField;
    }

    public void setUuidField(UUID uuidField) {
        this.uuidField = uuidField;
    }

    public byte getBytePrimitive() {
        return bytePrimitive;
    }

    public void setBytePrimitive(byte bytePrimitive) {
        this.bytePrimitive = bytePrimitive;
    }

    public byte[] getByteArrayField() {
        return byteArrayField;
    }

    public void setByteArrayField(byte[] byteArrayField) {
        this.byteArrayField = byteArrayField;
    }
}
