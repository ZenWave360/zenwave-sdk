package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.utils.JSONPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves an AsyncAPI channel address containing {@code {parameter}} expressions into the
 * list of concrete Kafka topic addresses that should be provisioned.
 *
 * <p>A channel address parameter falls into one of two categories:
 * <ul>
 *   <li><b>Enumerable</b> — the Parameter Object declares {@code enum}. One address is produced
 *       per declared value (or per CLI-selected subset). Several enumerable parameters on the same
 *       address expand as a cross product.</li>
 *   <li><b>Unbounded</b> — no {@code enum} is declared. Values only exist at runtime, so the channel
 *       is not provisionable: no topic, no schema and no ACL is generated for it and a warning is
 *       emitted naming the channel and the parameter. A channel-scoped
 *       {@code parameterValues.<channelKey>.<name>} makes it concrete for that run.</li>
 * </ul>
 *
 * <p>The value list for a parameter is chosen with the most specific source winning:
 * <ol>
 *   <li>{@code parameterValues.<channelKey>.<name>} — applies to that channel only, and is the only
 *       form accepted for an unbounded parameter</li>
 *   <li>{@code parameterValues.<name>} — applies to every channel parameter with that name, but only
 *       to parameters that declare an {@code enum}</li>
 *   <li>the channel's own {@code parameters.<name>.enum}</li>
 * </ol>
 * Values supplied on the command line are validated against each channel's own {@code enum},
 * never against a global set: two channels using the same parameter name are never assumed to
 * share the same allowed values. An unbounded parameter has no enum to validate against, so a
 * channel-scoped override for one is taken as given — which is why the bare form is not accepted
 * there: it would reach every channel using that name across every spec in the run.
 */
class AsyncAPIOpsChannelAddressResolver {

    private static final Pattern PARAMETER_EXPRESSION = Pattern.compile("\\{([^{}]+)}");

    private final Logger log = LoggerFactory.getLogger(getClass());

    /** {@code parameterValues.<name>} — applies to every channel. */
    private final Map<String, List<String>> bareValues = new LinkedHashMap<>();
    /** {@code parameterValues.<channelKey>.<name>} — applies to a single channel. */
    private final Map<String, Map<String, List<String>>> scopedValues = new LinkedHashMap<>();

    private final Set<String> usedBareValues = new LinkedHashSet<>();
    private final Set<String> usedScopedValues = new LinkedHashSet<>();
    private final Set<String> warnedChannels = new LinkedHashSet<>();

    AsyncAPIOpsChannelAddressResolver(Map<String, Object> parameterValues) {
        if (parameterValues == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : parameterValues.entrySet()) {
            // The dotted CLI convention nests one level deeper for the channel-scoped form:
            // a Map value is parameterValues.<channelKey>.<name>, anything else is a bare name.
            if (entry.getValue() instanceof Map<?, ?> scoped) {
                Map<String, List<String>> valuesByName = new LinkedHashMap<>();
                for (Map.Entry<?, ?> scopedEntry : scoped.entrySet()) {
                    valuesByName.put(String.valueOf(scopedEntry.getKey()), toValueList(scopedEntry.getValue()));
                }
                scopedValues.put(entry.getKey(), valuesByName);
            } else {
                bareValues.put(entry.getKey(), toValueList(entry.getValue()));
            }
        }
    }

    /**
     * Result of resolving one channel address. When {@link #unresolvedParameter()} is set the
     * channel carries an unbounded parameter and contributes no resources at all.
     */
    record Resolution(List<String> addresses, String unresolvedParameter) {
        boolean isSkipped() {
            return unresolvedParameter != null;
        }
    }

    Resolution resolve(String channelKey, Map channel) {
        String address = (String) channel.get("address");
        if (address == null) {
            // Keep the pre-existing behaviour for address-less channels: callers guard on null.
            return new Resolution(Collections.singletonList(null), null);
        }

        List<String> parameterNames = parameterNames(address);
        if (parameterNames.isEmpty()) {
            return new Resolution(List.of(address), null);
        }

        List<String> addresses = List.of(address);
        for (String parameterName : parameterNames) {
            List<String> values = resolveValues(channelKey, parameterName, enumValues(channel, parameterName));
            if (values.isEmpty()) {
                // Unbounded and not made concrete by a channel-scoped override.
                warnUnboundedParameter(channelKey, address, parameterName);
                return new Resolution(List.of(), parameterName);
            }
            addresses = expand(addresses, parameterName, values);
        }
        return new Resolution(addresses, null);
    }

    /**
     * Warns about {@code parameterValues} entries that never matched a channel parameter, so that
     * a typo in a channel key or parameter name does not silently generate the full enum.
     */
    void warnUnusedParameterValues() {
        bareValues.keySet().stream()
                .filter(name -> !usedBareValues.contains(name))
                .forEach(name -> log.warn("parameterValues.{} did not match any channel address parameter — ignored.", name));
        scopedValues.forEach((channelKey, valuesByName) -> valuesByName.keySet().stream()
                .filter(name -> !usedScopedValues.contains(channelKey + "." + name))
                .forEach(name -> log.warn("parameterValues.{}.{} did not match any channel address parameter — ignored.", channelKey, name)));
    }

    // -------------------------------------------------------------------------
    // Resolution
    // -------------------------------------------------------------------------

    private List<String> parameterNames(String address) {
        Set<String> names = new LinkedHashSet<>();
        Matcher matcher = PARAMETER_EXPRESSION.matcher(address);
        while (matcher.find()) {
            names.add(matcher.group(1).trim());
        }
        return new ArrayList<>(names);
    }

    private List<String> enumValues(Map channel, String parameterName) {
        Map<String, Map> parameters = JSONPath.get(channel, "$.parameters", Collections.emptyMap());
        Map parameter = parameters.get(parameterName);
        if (parameter == null || !(parameter.get("enum") instanceof List<?> values)) {
            return List.of();
        }
        return values.stream().map(String::valueOf).filter(value -> !value.isBlank()).toList();
    }

    /**
     * @param enumValues the channel's declared enum, empty when the parameter is unbounded
     * @return the values to expand with, empty only when the parameter is unbounded and no
     *         channel-scoped override names it
     */
    private List<String> resolveValues(String channelKey, String parameterName, List<String> enumValues) {
        Map<String, List<String>> scoped = channelKey != null ? scopedValues.get(channelKey) : null;
        if (scoped != null && scoped.containsKey(parameterName)) {
            usedScopedValues.add(channelKey + "." + parameterName);
            return validate(scoped.get(parameterName), enumValues,
                    "parameterValues." + channelKey + "." + parameterName, channelKey);
        }
        // The bare form deliberately does not apply to unbounded parameters: it would reach every
        // channel using that name across every spec in the run, with no enum to catch a mistake.
        if (!enumValues.isEmpty() && bareValues.containsKey(parameterName)) {
            usedBareValues.add(parameterName);
            return validate(bareValues.get(parameterName), enumValues,
                    "parameterValues." + parameterName, channelKey);
        }
        return enumValues;
    }

    private List<String> validate(List<String> selected, List<String> enumValues, String optionName, String channelKey) {
        if (selected.isEmpty()) {
            throw new IllegalArgumentException(optionName + " is empty for channel " + channelKey
                    + " — supply at least one value"
                    + (enumValues.isEmpty() ? "" : " (allowed: " + String.join(", ", enumValues) + ")"));
        }
        if (enumValues.isEmpty()) {
            // An unbounded parameter made concrete by naming the channel explicitly: there is no
            // declared enum to validate against, so the supplied values are taken as given.
            return selected;
        }
        for (String value : selected) {
            if (!enumValues.contains(value)) {
                throw new IllegalArgumentException(optionName + "=" + value + " is not valid for channel "
                        + channelKey + " (allowed: " + String.join(", ", enumValues) + ")");
            }
        }
        // Keep the spec declaration order so the generated Terraform is stable regardless of CLI order.
        return enumValues.stream().filter(selected::contains).toList();
    }

    private List<String> expand(List<String> addresses, String parameterName, List<String> values) {
        List<String> expanded = new ArrayList<>(addresses.size() * values.size());
        for (String address : addresses) {
            for (String value : values) {
                expanded.add(address.replace("{" + parameterName + "}", value));
            }
        }
        return expanded;
    }

    private void warnUnboundedParameter(String channelKey, String address, String parameterName) {
        if (!warnedChannels.add(channelKey + "|" + address)) {
            return;
        }
        if (bareValues.containsKey(parameterName)) {
            log.warn("Channel '{}' (address '{}') declares address parameter '{}' without an enum, so no topic, "
                            + "schema or ACL is generated for it. parameterValues.{} is not applied to parameters "
                            + "without an enum — name the channel explicitly with parameterValues.{}.{}=... to provision it.",
                    channelKey, address, parameterName, parameterName, channelKey, parameterName);
            return;
        }
        log.warn("Channel '{}' (address '{}') declares address parameter '{}' without an enum: "
                        + "its values only exist at runtime, so no topic, schema or ACL is generated for this channel. "
                        + "Declare an enum for '{}', override the address (apiOverlayFiles), or supply the values "
                        + "explicitly with parameterValues.{}.{}=... to provision it.",
                channelKey, address, parameterName, parameterName, channelKey, parameterName);
    }

    private static List<String> toValueList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).map(String::trim).filter(item -> !item.isEmpty()).toList();
        }
        if (value instanceof String string) {
            return Arrays.stream(string.split(",")).map(String::trim).filter(item -> !item.isEmpty()).toList();
        }
        return value == null ? List.of() : List.of(String.valueOf(value));
    }
}
