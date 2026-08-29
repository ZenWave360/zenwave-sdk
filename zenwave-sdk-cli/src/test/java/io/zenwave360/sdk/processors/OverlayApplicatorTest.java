package io.zenwave360.sdk.processors;

import io.zenwave360.sdk.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Overlay 1.0 / 1.1 application, driven through the public {@link YamlOverlyMerger#applyOverlay}
 * entry point.
 *
 * <p>
 * Most of what this class does is reject malformed overlays with a message naming the offending
 * action, so the validation surface is tested as carefully as the merge behaviour.
 * </p>
 */
class OverlayApplicatorTest {

    private static Map<String, Object> base() {
        var info = new LinkedHashMap<String, Object>();
        info.put("title", "Base API");
        info.put("version", "0.0.1");

        var tags = new ArrayList<>(List.of(map("name", "first"), map("name", "second")));

        var base = new LinkedHashMap<String, Object>();
        base.put("openapi", "3.0.0");
        base.put("info", info);
        base.put("tags", tags);
        return base;
    }

    private static Map<String, Object> map(Object... keyValues) {
        var map = new LinkedHashMap<String, Object>();
        for (int index = 0; index < keyValues.length; index += 2) {
            map.put((String) keyValues[index], keyValues[index + 1]);
        }
        return map;
    }

    private static Map<String, Object> overlay(String version, Object... actions) {
        var overlay = new LinkedHashMap<String, Object>();
        overlay.put("overlay", version);
        overlay.put("info", map("title", "Test overlay", "version", "1.0.0"));
        overlay.put("actions", List.of(actions));
        return overlay;
    }

    private static Map<String, Object> apply(Map<String, Object> overlay) {
        return YamlOverlyMerger.applyOverlay(base(), overlay);
    }

    private static String errorOf(Map<String, Object> overlay) {
        return Assertions.assertThrows(IllegalArgumentException.class, () -> apply(overlay)).getMessage();
    }

    private static String errorOf(Map<String, Object> base, Map<String, Object> overlay) {
        return Assertions.assertThrows(IllegalArgumentException.class,
                () -> YamlOverlyMerger.applyOverlay(base, overlay)).getMessage();
    }

    // ── null handling ─────────────────────────────────────────────────────────

    @Test
    void nullBaseOrOverlayIsANoOp() {
        var base = base();
        Assertions.assertSame(base, YamlOverlyMerger.applyOverlay(base, null));
        Assertions.assertNull(YamlOverlyMerger.applyOverlay(null, overlay("1.0.0", map("target", "$.info", "update", map()))));
    }

    // ── overlay document validation ───────────────────────────────────────────

    @Test
    void overlayVersionIsRequiredAndMustBeAString() {
        var missing = overlay("1.0.0", map("target", "$.info", "update", map()));
        missing.remove("overlay");
        Assertions.assertEquals("Overlay field 'overlay' is required and must be a version string", errorOf(missing));

        var notAString = overlay("1.0.0", map("target", "$.info", "update", map()));
        notAString.put("overlay", 1.0);
        Assertions.assertEquals("Overlay field 'overlay' is required and must be a version string", errorOf(notAString));
    }

    @Test
    void overlayVersionMustBeMajorMinorPatch() {
        Assertions.assertEquals("Unsupported overlay version '1.0': expected major.minor.patch",
                errorOf(overlay("1.0", map("target", "$.info", "update", map()))));
    }

    @Test
    void onlyOverlayFeatureSets10And11AreSupported() {
        Assertions.assertEquals("Unsupported Overlay feature set 2.0",
                errorOf(overlay("2.0.0", map("target", "$.info", "update", map()))));
        // patch version is irrelevant to the feature set
        Assertions.assertNotNull(apply(overlay("1.0.7", map("target", "$.info", "update", map("x", "y")))));
        Assertions.assertNotNull(apply(overlay("1.1.0", map("target", "$.info", "update", map("x", "y")))));
    }

    @Test
    void overlayInfoIsRequiredAndValidated() {
        var noInfo = overlay("1.0.0", map("target", "$.info", "update", map()));
        noInfo.remove("info");
        Assertions.assertEquals("Overlay field 'info' is required and must be an object", errorOf(noInfo));

        var noTitle = overlay("1.0.0", map("target", "$.info", "update", map()));
        noTitle.put("info", map("version", "1.0.0"));
        Assertions.assertEquals("Overlay info field 'title' is required and must be a string", errorOf(noTitle));

        var noVersion = overlay("1.0.0", map("target", "$.info", "update", map()));
        noVersion.put("info", map("title", "Test overlay"));
        Assertions.assertEquals("Overlay info field 'version' is required and must be a string", errorOf(noVersion));

        var badDescription = overlay("1.0.0", map("target", "$.info", "update", map()));
        badDescription.put("info", map("title", "T", "version", "1.0.0", "description", 42));
        Assertions.assertEquals("Overlay info field 'description' must be a string", errorOf(badDescription));

        var goodDescription = overlay("1.0.0", map("target", "$.info", "update", map("x", "y")));
        goodDescription.put("info", map("title", "T", "version", "1.0.0", "description", "why"));
        Assertions.assertEquals("y", JSONPath.get(apply(goodDescription), "$.info.x"));
    }

    @Test
    void overlayActionsAreRequiredAndNonEmpty() {
        var noActions = overlay("1.0.0");
        noActions.remove("actions");
        Assertions.assertEquals("Overlay field 'actions' is required and must be a non-empty array", errorOf(noActions));
        Assertions.assertEquals("Overlay field 'actions' is required and must be a non-empty array", errorOf(overlay("1.0.0")));
    }

    // ── action validation ─────────────────────────────────────────────────────

    @Test
    void actionsMustBeObjectsWithATarget() {
        Assertions.assertEquals("Overlay action 0 must be an object", errorOf(overlay("1.0.0", "not-an-object")));
        Assertions.assertEquals("Overlay action 0 field 'target' is required and must be a string",
                errorOf(overlay("1.0.0", map("update", map()))));
    }

    @Test
    void actionDescriptionAndRemoveAreTypeChecked() {
        Assertions.assertEquals("Overlay action 0 field 'description' must be a string",
                errorOf(overlay("1.0.0", map("target", "$.info", "description", 1, "update", map()))));
        Assertions.assertEquals("Overlay action 0 field 'remove' must be a boolean",
                errorOf(overlay("1.0.0", map("target", "$.info", "remove", "yes"))));
    }

    @Test
    void actionIndexIsReportedSoTheOffendingActionCanBeFound() {
        Assertions.assertEquals("Overlay action 1 must define update, copy, or remove: true",
                errorOf(overlay("1.0.0",
                        map("target", "$.info", "update", map("x", "y")),
                        map("target", "$.tags"))));
    }

    @Test
    void copyRequiresOverlay11AndAJsonPathString() {
        Assertions.assertEquals("Overlay action 0 field 'copy' requires Overlay 1.1",
                errorOf(overlay("1.0.0", map("target", "$.info", "copy", "$.tags"))));
        Assertions.assertEquals("Overlay action 0 field 'copy' must be a JSONPath string",
                errorOf(overlay("1.1.0", map("target", "$.info", "copy", 42))));
    }

    @Test
    void invalidJsonPathIsReportedWithTheExpressionType() {
        Assertions.assertTrue(errorOf(overlay("1.1.0", map("target", "$.[", "update", map())))
                .startsWith("Overlay action 0 invalid target JSONPath '$.['"));
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void updateMergesIntoObjectTargets() {
        var result = apply(overlay("1.1.0", map("target", "$.info",
                "update", map("version", "2.0.0", "contact", map("name", "ZenWave")))));

        Assertions.assertEquals("2.0.0", JSONPath.get(result, "$.info.version"));
        Assertions.assertEquals("Base API", JSONPath.get(result, "$.info.title"));
        Assertions.assertEquals("ZenWave", JSONPath.get(result, "$.info.contact.name"));
    }

    @Test
    void updateAppendsToArrayTargets() {
        var appendOne = apply(overlay("1.1.0", map("target", "$.tags", "update", map("name", "third"))));
        Assertions.assertEquals(3, ((List<?>) JSONPath.get(appendOne, "$.tags")).size());

        var appendMany = apply(overlay("1.1.0", map("target", "$.tags",
                "update", List.of(map("name", "third"), map("name", "fourth")))));
        Assertions.assertEquals(4, ((List<?>) JSONPath.get(appendMany, "$.tags")).size());
    }

    @Test
    void updateReplacesPrimitiveTargetsInOverlay11() {
        var result = apply(overlay("1.1.0", map("target", "$.info.title", "update", "Renamed")));
        Assertions.assertEquals("Renamed", JSONPath.get(result, "$.info.title"));
    }

    @Test
    void overlay10RejectsPrimitiveTargets() {
        Assertions.assertEquals("Overlay action 0 Overlay 1.0 targets must select only objects or arrays",
                errorOf(overlay("1.0.0", map("target", "$.info.title", "update", "Renamed"))));
    }

    @Test
    void overlay10ReplacesWhenTargetAndUpdateAreNotBothObjects() {
        // legacy behaviour: an array target with an object update is replaced wholesale, not appended
        var result = apply(overlay("1.0.0", map("target", "$.tags", "update", map("name", "only"))));
        Assertions.assertEquals("only", JSONPath.get(result, "$.tags.name"));
    }

    @Test
    void overlay10CannotReplaceTheDocumentRootWithANonObject() {
        Assertions.assertEquals("The root of the target document must remain an object",
                errorOf(overlay("1.0.0", map("target", "$", "update", "not-an-object"))));
    }

    @Test
    void incompatibleUpdateTypesAreRejected() {
        Assertions.assertEquals("Overlay action 0 cannot apply primitive value to object target",
                errorOf(overlay("1.1.0", map("target", "$.info", "update", "a string"))));
        Assertions.assertEquals("Overlay action 0 cannot apply object value to primitive target",
                errorOf(overlay("1.1.0", map("target", "$.info.title", "update", map("a", "b")))));
        Assertions.assertEquals("Overlay action 0 incompatible values for property 'title': primitive and object",
                errorOf(overlay("1.1.0", map("target", "$.info", "update", map("title", map("a", "b"))))));
    }

    @Test
    void updateObjectKeysMustBeStrings() {
        Map<Object, Object> update = new LinkedHashMap<>();
        update.put(1, "x");
        Assertions.assertEquals("Overlay action 0 update object keys must be strings",
                errorOf(overlay("1.1.0", map("target", "$.info", "update", update))));
    }

    @Test
    void targetsSelectedByOneActionMustAllBeOfTheSameKind() {
        // $.info.title is a primitive, $.tags is an array
        Assertions.assertEquals("Overlay action 0 update and copy targets must all be objects, arrays, or primitives",
                errorOf(overlay("1.1.0", map("target", "$['info','tags']", "update", map("a", "b")))));
    }

    @Test
    void aTargetThatSelectsNothingIsSilentlySkipped() {
        var result = apply(overlay("1.1.0", map("target", "$.missing", "update", map("a", "b"))));
        Assertions.assertEquals(base(), result);
    }

    // ── union targets and unusual keys ────────────────────────────────────────

    @Test
    void unionTargetsAreExpandedToTheirMemberNodes() {
        var base = new LinkedHashMap<String, Object>();
        base.put("first", map("kept", true));
        base.put("second", map("kept", true));

        var result = YamlOverlyMerger.applyOverlay(base,
                overlay("1.1.0", map("target", "$['first','second']", "update", map("added", true))));

        Assertions.assertEquals(Boolean.TRUE, JSONPath.get(result, "$.first.added"));
        Assertions.assertEquals(Boolean.TRUE, JSONPath.get(result, "$.second.added"));
        Assertions.assertEquals(Boolean.TRUE, JSONPath.get(result, "$.first.kept"));
    }

    @Test
    void keysContainingPathSyntaxAreHandled() {
        // the normalized paths for these keys contain quotes, dots and brackets, which the segment
        // scanner must not mistake for path structure
        var base = new LinkedHashMap<String, Object>();
        base.put("a.b", map("kept", true));
        base.put("c[0]", map("kept", true));
        base.put("d,e", map("kept", true));

        var result = YamlOverlyMerger.applyOverlay(base, overlay("1.1.0",
                map("target", "$['a.b']", "update", map("added", true)),
                map("target", "$['c[0]']", "update", map("added", true)),
                map("target", "$['d,e']", "update", map("added", true))));

        Assertions.assertEquals(Boolean.TRUE, ((Map<?, ?>) result.get("a.b")).get("added"));
        Assertions.assertEquals(Boolean.TRUE, ((Map<?, ?>) result.get("c[0]")).get("added"));
        Assertions.assertEquals(Boolean.TRUE, ((Map<?, ?>) result.get("d,e")).get("added"));
    }

    @Test
    void arrayIndexTargetsAreUpdatedInPlace() {
        var result = apply(overlay("1.1.0", map("target", "$.tags[1]", "update", map("added", true))));
        Assertions.assertEquals(Boolean.TRUE, JSONPath.get(result, "$.tags[1].added"));
        Assertions.assertNull(JSONPath.get(result, "$.tags[0].added"));
    }

    // ── remove ────────────────────────────────────────────────────────────────

    @Test
    void removeDeletesTheSelectedNodes() {
        var result = apply(overlay("1.1.0", map("target", "$.info.version", "remove", true)));
        Assertions.assertNull(JSONPath.get(result, "$.info.version"));
        Assertions.assertEquals("Base API", JSONPath.get(result, "$.info.title"));
    }

    @Test
    void removeDeletesArrayElementsFromTheEndSoIndexesStayValid() {
        var result = apply(overlay("1.1.0", map("target", "$.tags[*]", "remove", true)));
        Assertions.assertEquals(List.of(), JSONPath.get(result, "$.tags"));
    }

    @Test
    void theDocumentRootCannotBeRemoved() {
        Assertions.assertEquals("Overlay action 0 the document root cannot be removed",
                errorOf(overlay("1.1.0", map("target", "$", "remove", true))));
    }

    @Test
    void removeIsNotConfusedByOverlappingSelections() {
        // the parent is removed first by depth ordering; the child removal must then be ignored
        var result = apply(overlay("1.1.0", map("target", "$..name", "remove", true),
                map("target", "$.tags", "remove", true)));
        Assertions.assertNull(JSONPath.get(result, "$.tags"));
    }

    // ── copy ──────────────────────────────────────────────────────────────────

    @Test
    void copyAppliesTheValueOfTheSourceNode() {
        var result = apply(overlay("1.1.0", map("target", "$.info", "copy", "$.tags[0]")));
        Assertions.assertEquals("first", JSONPath.get(result, "$.info.name"));
    }

    @Test
    void copyMustSelectExactlyOneNode() {
        Assertions.assertEquals("Overlay action 0 copy expression must select exactly one node but selected 2",
                errorOf(overlay("1.1.0", map("target", "$.info", "copy", "$.tags[*]"))));
        Assertions.assertEquals("Overlay action 0 copy expression must select exactly one node but selected 0",
                errorOf(overlay("1.1.0", map("target", "$.info", "copy", "$.missing"))));
    }

    // ── the base document is never mutated ────────────────────────────────────

    @Test
    void theBaseDocumentIsLeftUntouched() {
        var base = base();
        YamlOverlyMerger.applyOverlay(base, overlay("1.1.0",
                map("target", "$.info", "update", map("version", "2.0.0")),
                map("target", "$.tags", "update", map("name", "third"))));

        Assertions.assertEquals("0.0.1", JSONPath.get(base, "$.info.version"));
        Assertions.assertEquals(2, ((List<?>) JSONPath.get(base, "$.tags")).size());
    }
}
