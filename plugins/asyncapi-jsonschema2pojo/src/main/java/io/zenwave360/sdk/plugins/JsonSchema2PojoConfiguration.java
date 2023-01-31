package io.zenwave360.sdk.plugins;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.jsonschema2pojo.AnnotationStyle;
import org.jsonschema2pojo.Annotator;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.InclusionLevel;
import org.jsonschema2pojo.NoopAnnotator;
import org.jsonschema2pojo.SourceSortOrder;
import org.jsonschema2pojo.SourceType;
import org.jsonschema2pojo.rules.RuleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonSchema2PojoConfiguration implements GenerationConfig {

    private static String API_PACKAGE = "apiPackage";
    private static String MODEL_PACKAGE = "modelPackage";

    private static final Logger log = LoggerFactory.getLogger(JsonSchema2PojoConfiguration.class);

//    private static final String PREFIX = "jsonschema2pojo.";

    private AnnotationStyle annotationStyle = AnnotationStyle.JACKSON2;
    private String dateTimeType = "java.time.OffsetDateTime";
    private String dateType = "java.time.LocalDate";
    private String timeType = null;
    private boolean generateBuilders = true;
    private boolean includeJsr303Annotations = true;
    private boolean includeJsr305Annotations = false;
    private boolean includeDynamicAccessors = true;
    private boolean includeDynamicGetters = true;
    private boolean includeDynamicSetters = true;
    private boolean includeDynamicBuilders = true;
    private boolean includeTypeInfo = false;
    private boolean serializable = true;
    private Map<String, String> formatTypeMapping = new HashMap<String, String>();
    private boolean includeConstructorPropertiesAnnotation = false;
    private boolean usePrimitives = false;
    private Iterator<URL> source;
    private File targetDirectory = new File(".");
    private String targetPackage = "";
    private char[] propertyWordDelimiters = new char[] {'-', ' ', '_'};
    private boolean useLongIntegers = false;
    private boolean useBigIntegers = false;
    private boolean useDoubleNumbers = true;
    private boolean useBigDecimals = false;
    private boolean includeHashcodeAndEquals = true;
    private boolean includeToString = true;
    private String[] toStringExcludes = new String[] {};
    private boolean useTitleAsClassname = true;
    private InclusionLevel inclusionLevel = InclusionLevel.NON_NULL;
    private Class<? extends Annotator> customAnnotator = NoopAnnotator.class;
    private Class<? extends RuleFactory> customRuleFactory = RuleFactory.class;
    private boolean useOptionalForGetters = false;
    private SourceType sourceType = SourceType.JSONSCHEMA;
    private String outputEncoding = StandardCharsets.UTF_8.toString();
    private boolean useJodaDates = false;
    private boolean useJodaLocalDates = false;
    private boolean useJodaLocalTimes = false;
    private boolean parcelable = false;
    // private FileFilter fileFilter = new AllFileFilter();
    private boolean initializeCollections = true;
    private String classNamePrefix = "";
    private String classNameSuffix = "";
    private String[] fileExtensions = new String[] {};
    private boolean includeConstructors = false;
    private boolean constructorsRequiredPropertiesOnly = false;
    private boolean includeRequiredPropertiesConstructor = false;
    private boolean includeAllPropertiesConstructor = false;
    private boolean includeCopyConstructor = false;
    private boolean includeAdditionalProperties = true;
    private boolean includeGetters = true;
    private boolean includeSetters = true;
    private String targetVersion = "1.6";
    private boolean formatDates = false;
    private boolean formatTimes = false;
    private boolean formatDateTimes = false;
    private String customDatePattern = null;
    private String customTimePattern = null;
    private String customDateTimePattern = null;
    private String refFragmentPathDelimiters = "#/.";
    private SourceSortOrder sourceSortOrder = SourceSortOrder.OS;

    public static JsonSchema2PojoConfiguration of(Map<String, String> settings) {
        JsonSchema2PojoConfiguration config = new JsonSchema2PojoConfiguration();

        for (Field field : JsonSchema2PojoConfiguration.class.getDeclaredFields()) {
            try {
                set(config, settings, field);
            } catch (ClassNotFoundException | IllegalArgumentException | IllegalAccessException e) {
                log.error("Error configuring jsonschema2pojo.{}", field.getName(), e);
                throw new RuntimeException(e);
            }
        }

        if (StringUtils.isBlank(config.getTargetPackage())) {
            config.setTargetPackage(firstNonNull(settings.get(MODEL_PACKAGE), settings.get(API_PACKAGE)));
        }

        return config;
    }

    private static void set(JsonSchema2PojoConfiguration config, Map<String, String> s, Field f) throws IllegalArgumentException, IllegalAccessException, ClassNotFoundException {
        Object defaultValue = f.get(config);
        Object value = null;

        if (f.getType().isEnum()) {
            value = s.containsKey(f.getName()) ? Enum.valueOf((Class) f.getType(), s.get(f.getName()).toUpperCase()) : defaultValue;
        } else if (f.getType().isArray()) {
            value = s.containsKey(f.getName()) ? getArray(s, f.getName()) : defaultValue;
        } else if (f.getType().isAssignableFrom(boolean.class) || f.getType().isAssignableFrom(Boolean.class)) {
            value = getBoolean(s, f.getName(), (Boolean) defaultValue);
        } else if (f.getType().isAssignableFrom(String.class)) {
            value = s.getOrDefault(f.getName(), (String) defaultValue);
        } else if (f.getType().isAssignableFrom(Map.class)) {
            value = s.containsKey(f.getName()) ? getMap(s, f.getName()) : defaultValue;
        } else if (f.getType().isAssignableFrom(File.class)) {
            value = s.containsKey(f.getName()) ? new File(s.get(f.getName())) : defaultValue;
        } else if (f.getType().isAssignableFrom(Class.class)) {
            value = s.containsKey(f.getName()) ? Class.forName(s.get(f.getName())) : defaultValue;
        }

        if (value != null && value != defaultValue) {
            f.set(config, value);
        }
    }

    private static boolean getBoolean(Map<String, String> s, String key, boolean defaultValue) {
        if (s.containsKey(key)) {
            return Boolean.valueOf(s.get(key));
        }
        return defaultValue;
    }

    /**
     * Splits entry in the format key1=value1,key2=value2
     * 
     * @param key
     * @return
     */
    private static Map<String, String> getMap(Map<String, String> s, String key) {
        if (s.containsKey(key)) {
            return Arrays.asList(s.get(key).split(",|;|\n|\t"))
                    .stream()
                    .map(str -> str.split("=", 2))
                    .collect(Collectors.toMap(split -> StringUtils.trim(split[0]), split -> StringUtils.trim(split[1])));
        }
        return null;
    }

    private static String[] getArray(Map<String, String> s, String key) {
        return s.get(key).split(",|;|\n|\t");
    }

    public AnnotationStyle getAnnotationStyle() {
        return annotationStyle;
    }

    public void setAnnotationStyle(AnnotationStyle annotationStyle) {
        this.annotationStyle = annotationStyle;
    }

    public String getDateTimeType() {
        return dateTimeType;
    }

    public void setDateTimeType(String dateTimeType) {
        this.dateTimeType = dateTimeType;
    }

    public String getDateType() {
        return dateType;
    }

    public void setDateType(String dateType) {
        this.dateType = dateType;
    }

    public String getTimeType() {
        return timeType;
    }

    public void setTimeType(String timeType) {
        this.timeType = timeType;
    }

    public boolean isGenerateBuilders() {
        return generateBuilders;
    }

    public void setGenerateBuilders(boolean generateBuilders) {
        this.generateBuilders = generateBuilders;
    }

    public boolean isIncludeJsr303Annotations() {
        return includeJsr303Annotations;
    }

    public void setIncludeJsr303Annotations(boolean includeJsr303Annotations) {
        this.includeJsr303Annotations = includeJsr303Annotations;
    }

    public boolean isIncludeJsr305Annotations() {
        return includeJsr305Annotations;
    }

    public void setIncludeJsr305Annotations(boolean includeJsr305Annotations) {
        this.includeJsr305Annotations = includeJsr305Annotations;
    }

    public boolean isIncludeDynamicAccessors() {
        return includeDynamicAccessors;
    }

    public void setIncludeDynamicAccessors(boolean includeDynamicAccessors) {
        this.includeDynamicAccessors = includeDynamicAccessors;
    }

    public boolean isIncludeDynamicGetters() {
        return includeDynamicGetters;
    }

    public void setIncludeDynamicGetters(boolean includeDynamicGetters) {
        this.includeDynamicGetters = includeDynamicGetters;
    }

    public boolean isIncludeDynamicSetters() {
        return includeDynamicSetters;
    }

    public void setIncludeDynamicSetters(boolean includeDynamicSetters) {
        this.includeDynamicSetters = includeDynamicSetters;
    }

    public boolean isIncludeDynamicBuilders() {
        return includeDynamicBuilders;
    }

    public void setIncludeDynamicBuilders(boolean includeDynamicBuilders) {
        this.includeDynamicBuilders = includeDynamicBuilders;
    }

    public boolean isIncludeTypeInfo() {
        return includeTypeInfo;
    }

    public void setIncludeTypeInfo(boolean includeTypeInfo) {
        this.includeTypeInfo = includeTypeInfo;
    }

    public boolean isSerializable() {
        return serializable;
    }

    @Override
    public FileFilter getFileFilter() {
        return null;
    }

    public void setSerializable(boolean serializable) {
        this.serializable = serializable;
    }

    public Map<String, String> getFormatTypeMapping() {
        return formatTypeMapping;
    }

    @Override
    public boolean isUseInnerClassBuilders() {
        return GenerationConfig.super.isUseInnerClassBuilders();
    }

    @Override
    public boolean isIncludeGeneratedAnnotation() {
        return false;
    }

    @Override
    public boolean isUseJakartaValidation() {
        return false;
    }

    public void setFormatTypeMapping(Map<String, String> formatTypeMapping) {
        this.formatTypeMapping = formatTypeMapping;
    }

    public boolean isIncludeConstructorPropertiesAnnotation() {
        return includeConstructorPropertiesAnnotation;
    }

    public void setIncludeConstructorPropertiesAnnotation(boolean includeConstructorPropertiesAnnotation) {
        this.includeConstructorPropertiesAnnotation = includeConstructorPropertiesAnnotation;
    }

    public boolean isUsePrimitives() {
        return usePrimitives;
    }

    public void setUsePrimitives(boolean usePrimitives) {
        this.usePrimitives = usePrimitives;
    }

    public Iterator<URL> getSource() {
        return source;
    }

    public void setSource(Iterator<URL> source) {
        this.source = source;
    }

    public File getTargetDirectory() {
        return targetDirectory;
    }

    public void setTargetDirectory(File targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    public String getTargetPackage() {
        return targetPackage;
    }

    public void setTargetPackage(String targetPackage) {
        this.targetPackage = targetPackage;
    }

    public char[] getPropertyWordDelimiters() {
        return propertyWordDelimiters;
    }

    public void setPropertyWordDelimiters(char[] propertyWordDelimiters) {
        this.propertyWordDelimiters = propertyWordDelimiters;
    }

    public boolean isUseLongIntegers() {
        return useLongIntegers;
    }

    public void setUseLongIntegers(boolean useLongIntegers) {
        this.useLongIntegers = useLongIntegers;
    }

    public boolean isUseBigIntegers() {
        return useBigIntegers;
    }

    public void setUseBigIntegers(boolean useBigIntegers) {
        this.useBigIntegers = useBigIntegers;
    }

    public boolean isUseDoubleNumbers() {
        return useDoubleNumbers;
    }

    public void setUseDoubleNumbers(boolean useDoubleNumbers) {
        this.useDoubleNumbers = useDoubleNumbers;
    }

    public boolean isUseBigDecimals() {
        return useBigDecimals;
    }

    public void setUseBigDecimals(boolean useBigDecimals) {
        this.useBigDecimals = useBigDecimals;
    }

    public boolean isIncludeHashcodeAndEquals() {
        return includeHashcodeAndEquals;
    }

    public void setIncludeHashcodeAndEquals(boolean includeHashcodeAndEquals) {
        this.includeHashcodeAndEquals = includeHashcodeAndEquals;
    }

    public boolean isIncludeToString() {
        return includeToString;
    }

    public void setIncludeToString(boolean includeToString) {
        this.includeToString = includeToString;
    }

    public String[] getToStringExcludes() {
        return toStringExcludes;
    }

    public void setToStringExcludes(String[] toStringExcludes) {
        this.toStringExcludes = toStringExcludes;
    }

    public boolean isUseTitleAsClassname() {
        return useTitleAsClassname;
    }

    public void setUseTitleAsClassname(boolean useTitleAsClassname) {
        this.useTitleAsClassname = useTitleAsClassname;
    }

    public InclusionLevel getInclusionLevel() {
        return inclusionLevel;
    }

    public void setInclusionLevel(InclusionLevel inclusionLevel) {
        this.inclusionLevel = inclusionLevel;
    }

    public Class<? extends Annotator> getCustomAnnotator() {
        return customAnnotator;
    }

    public void setCustomAnnotator(Class<? extends Annotator> customAnnotator) {
        this.customAnnotator = customAnnotator;
    }

    public Class<? extends RuleFactory> getCustomRuleFactory() {
        return customRuleFactory;
    }

    public void setCustomRuleFactory(Class<? extends RuleFactory> customRuleFactory) {
        this.customRuleFactory = customRuleFactory;
    }

    public boolean isUseOptionalForGetters() {
        return useOptionalForGetters;
    }

    public void setUseOptionalForGetters(boolean useOptionalForGetters) {
        this.useOptionalForGetters = useOptionalForGetters;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    @Override
    public boolean isRemoveOldOutput() {
        return false;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getOutputEncoding() {
        return outputEncoding;
    }

    public void setOutputEncoding(String outputEncoding) {
        this.outputEncoding = outputEncoding;
    }

    public boolean isUseJodaDates() {
        return useJodaDates;
    }

    public void setUseJodaDates(boolean useJodaDates) {
        this.useJodaDates = useJodaDates;
    }

    public boolean isUseJodaLocalDates() {
        return useJodaLocalDates;
    }

    public void setUseJodaLocalDates(boolean useJodaLocalDates) {
        this.useJodaLocalDates = useJodaLocalDates;
    }

    public boolean isUseJodaLocalTimes() {
        return useJodaLocalTimes;
    }

    public void setUseJodaLocalTimes(boolean useJodaLocalTimes) {
        this.useJodaLocalTimes = useJodaLocalTimes;
    }

    public boolean isParcelable() {
        return parcelable;
    }

    public void setParcelable(boolean parcelable) {
        this.parcelable = parcelable;
    }

    public boolean isInitializeCollections() {
        return initializeCollections;
    }

    public void setInitializeCollections(boolean initializeCollections) {
        this.initializeCollections = initializeCollections;
    }

    public String getClassNamePrefix() {
        return classNamePrefix;
    }

    public void setClassNamePrefix(String classNamePrefix) {
        this.classNamePrefix = classNamePrefix;
    }

    public String getClassNameSuffix() {
        return classNameSuffix;
    }

    public void setClassNameSuffix(String classNameSuffix) {
        this.classNameSuffix = classNameSuffix;
    }

    public String[] getFileExtensions() {
        return fileExtensions;
    }

    public void setFileExtensions(String[] fileExtensions) {
        this.fileExtensions = fileExtensions;
    }

    public boolean isIncludeConstructors() {
        return includeConstructors;
    }

    public void setIncludeConstructors(boolean includeConstructors) {
        this.includeConstructors = includeConstructors;
    }

    public boolean isConstructorsRequiredPropertiesOnly() {
        return constructorsRequiredPropertiesOnly;
    }

    public void setConstructorsRequiredPropertiesOnly(boolean constructorsRequiredPropertiesOnly) {
        this.constructorsRequiredPropertiesOnly = constructorsRequiredPropertiesOnly;
    }

    public boolean isIncludeRequiredPropertiesConstructor() {
        return includeRequiredPropertiesConstructor;
    }

    public void setIncludeRequiredPropertiesConstructor(boolean includeRequiredPropertiesConstructor) {
        this.includeRequiredPropertiesConstructor = includeRequiredPropertiesConstructor;
    }

    public boolean isIncludeAllPropertiesConstructor() {
        return includeAllPropertiesConstructor;
    }

    public void setIncludeAllPropertiesConstructor(boolean includeAllPropertiesConstructor) {
        this.includeAllPropertiesConstructor = includeAllPropertiesConstructor;
    }

    public boolean isIncludeCopyConstructor() {
        return includeCopyConstructor;
    }

    public void setIncludeCopyConstructor(boolean includeCopyConstructor) {
        this.includeCopyConstructor = includeCopyConstructor;
    }

    public boolean isIncludeAdditionalProperties() {
        return includeAdditionalProperties;
    }

    public void setIncludeAdditionalProperties(boolean includeAdditionalProperties) {
        this.includeAdditionalProperties = includeAdditionalProperties;
    }

    public boolean isIncludeGetters() {
        return includeGetters;
    }

    public void setIncludeGetters(boolean includeGetters) {
        this.includeGetters = includeGetters;
    }

    public boolean isIncludeSetters() {
        return includeSetters;
    }

    public void setIncludeSetters(boolean includeSetters) {
        this.includeSetters = includeSetters;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(String targetVersion) {
        this.targetVersion = targetVersion;
    }

    public boolean isFormatDates() {
        return formatDates;
    }

    public void setFormatDates(boolean formatDates) {
        this.formatDates = formatDates;
    }

    public boolean isFormatTimes() {
        return formatTimes;
    }

    public void setFormatTimes(boolean formatTimes) {
        this.formatTimes = formatTimes;
    }

    public boolean isFormatDateTimes() {
        return formatDateTimes;
    }

    public void setFormatDateTimes(boolean formatDateTimes) {
        this.formatDateTimes = formatDateTimes;
    }

    public String getCustomDatePattern() {
        return customDatePattern;
    }

    public void setCustomDatePattern(String customDatePattern) {
        this.customDatePattern = customDatePattern;
    }

    public String getCustomTimePattern() {
        return customTimePattern;
    }

    public void setCustomTimePattern(String customTimePattern) {
        this.customTimePattern = customTimePattern;
    }

    public String getCustomDateTimePattern() {
        return customDateTimePattern;
    }

    public void setCustomDateTimePattern(String customDateTimePattern) {
        this.customDateTimePattern = customDateTimePattern;
    }

    public String getRefFragmentPathDelimiters() {
        return refFragmentPathDelimiters;
    }

    public void setRefFragmentPathDelimiters(String refFragmentPathDelimiters) {
        this.refFragmentPathDelimiters = refFragmentPathDelimiters;
    }

    public SourceSortOrder getSourceSortOrder() {
        return sourceSortOrder;
    }

    public void setSourceSortOrder(SourceSortOrder sourceSortOrder) {
        this.sourceSortOrder = sourceSortOrder;
    }

}
