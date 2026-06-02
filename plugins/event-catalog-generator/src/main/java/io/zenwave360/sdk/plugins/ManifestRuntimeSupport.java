package io.zenwave360.sdk.plugins;

import io.zenwave360.manifest.ManifestArtifact;
import io.zenwave360.manifest.ManifestLoadOptions;
import io.zenwave360.manifest.ManifestResolvedResource;
import io.zenwave360.manifest.ManifestService;
import io.zenwave360.manifest.ZenWaveManifest;
import io.zenwave360.manifest.ZenWaveManifestLoader;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class ManifestRuntimeSupport {

    private ManifestRuntimeSupport() {
    }

    static ManifestService findService(ZenWaveManifest manifest, Map<String, Object> serviceMap) {
        String serviceRef = string(serviceMap.get("serviceRef"));
        if (serviceRef != null) {
            return manifest.getServicesByRef().get(serviceRef);
        }
        String serviceId = string(serviceMap.get("id"));
        return serviceId != null ? manifest.getServicesById().get(serviceId) : null;
    }

    static ManifestArtifact findArtifact(ManifestService service, String type) {
        for (ManifestArtifact artifact : service.getArtifacts()) {
            if (type.equals(artifact.getType())) {
                return artifact;
            }
        }
        return null;
    }

    static List<ManifestArtifact> findArtifacts(ManifestService service, String type) {
        List<ManifestArtifact> result = new ArrayList<>();
        for (ManifestArtifact artifact : service.getArtifacts()) {
            if (type.equals(artifact.getType())) {
                result.add(artifact);
            }
        }
        return result;
    }

    static ManifestLoadOptions contentOptions(ZenWaveManifest manifest, File manifestFile,
                                              String preferredSource, Boolean allowFallback, String localRoots) {
        return new ManifestLoadOptions(
                blankToNull(preferredSource),
                allowFallback == null || allowFallback,
                explicitLocalRoots(manifest, manifestFile, localRoots));
    }

    static String loadArtifactText(ZenWaveManifestLoader loader, ZenWaveManifest manifest, ManifestService service,
                                   ManifestArtifact artifact, ManifestLoadOptions options) {
        try {
            return BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, new Function2<CoroutineScope, kotlin.coroutines.Continuation<? super String>, Object>() {
                @Override
                public Object invoke(CoroutineScope scope, kotlin.coroutines.Continuation<? super String> continuation) {
                    return loader.loadArtifactText(manifest, service, artifact, options, continuation);
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while loading artifact text for " + service.getServiceRef(), e);
        }
    }

    static Map<String, String> loadServiceDocs(ZenWaveManifestLoader loader, ZenWaveManifest manifest,
                                               ManifestService service, ManifestLoadOptions options) {
        try {
            return BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, new Function2<CoroutineScope, kotlin.coroutines.Continuation<? super Map<String, String>>, Object>() {
                @Override
                public Object invoke(CoroutineScope scope, kotlin.coroutines.Continuation<? super Map<String, String>> continuation) {
                    return loader.loadServiceDocs(manifest, service, options, continuation);
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while loading docs for " + service.getServiceRef(), e);
        }
    }

    static String resolveLinkUri(ZenWaveManifest manifest, File manifestFile, ManifestService service,
                                 ManifestArtifact artifact, String relativePath, String linkSource, String localRoots) {
        String direct = relativePath != null ? relativePath : artifact.getPathExpression();
        if (hasScheme(direct)) {
            return direct;
        }
        String source = chooseLinkSource(manifest, linkSource);
        return switch (source) {
            case "http" -> buildHttpUri(manifest, service, direct);
            case "apicurio" -> buildApicurioUri(manifest, service, artifact, direct);
            case "file" -> buildFileUri(manifest, manifestFile, service, direct, localRoots);
            default -> null;
        };
    }

    static String resolveContentPath(ZenWaveManifestLoader loader, ZenWaveManifest manifest, File manifestFile, ManifestService service,
                                     ManifestArtifact artifact, String preferredSource, Boolean allowFallback, String localRoots) {
        ManifestLoadOptions options = contentOptions(manifest, manifestFile, preferredSource, allowFallback, localRoots);
        ManifestResolvedResource resolved = resolveArtifact(loader, manifest, service, artifact, options);
        if (resolved == null || resolved.getUri() == null) {
            return null;
        }
        URI uri = URI.create(resolved.getUri());
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return Path.of(uri).toString();
        }
        return resolved.getUri();
    }

    static List<Map<String, Object>> artifactMapsWithLinks(ZenWaveManifest manifest, File manifestFile,
                                                           ManifestService service, String linkSource, String localRoots) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ManifestArtifact artifact : service.getArtifacts()) {
            Map<String, Object> artifactMap = new LinkedHashMap<>();
            artifactMap.put("name", artifact.getName());
            artifactMap.put("type", artifact.getType());
            artifactMap.put("path", artifact.getPathExpression());
            if (artifact.getVersion() != null) {
                artifactMap.put("version", artifact.getVersion());
            }
            String linkUri = resolveLinkUri(manifest, manifestFile, service, artifact, artifact.getPathExpression(), linkSource, localRoots);
            if (linkUri != null) {
                artifactMap.put("linkUri", linkUri);
            }
            result.add(artifactMap);
        }
        return result;
    }

    private static ManifestResolvedResource resolveArtifact(ZenWaveManifestLoader loader, ZenWaveManifest manifest, ManifestService service,
                                                            ManifestArtifact artifact, ManifestLoadOptions options) {
        try {
            return BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, new Function2<CoroutineScope, kotlin.coroutines.Continuation<? super ManifestResolvedResource>, Object>() {
                @Override
                public Object invoke(CoroutineScope scope, kotlin.coroutines.Continuation<? super ManifestResolvedResource> continuation) {
                    return loader.resolveArtifact(manifest, service, artifact, options, continuation);
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while resolving artifact for " + service.getServiceRef(), e);
        }
    }

    private static String chooseLinkSource(ZenWaveManifest manifest, String linkSource) {
        if (linkSource != null && !linkSource.isBlank()) {
            return linkSource;
        }
        if (manifest.getConfig().getSources().getHttp() != null
                && !manifest.getConfig().getSources().getHttp().getRoots().isEmpty()) {
            return "http";
        }
        return manifest.getConfig().getSourcePriority().isEmpty()
                ? "file"
                : manifest.getConfig().getSourcePriority().get(0);
    }

    private static String buildHttpUri(ZenWaveManifest manifest, ManifestService service, String relativePath) {
        if (manifest.getConfig().getSources().getHttp() == null
                || manifest.getConfig().getSources().getHttp().getRoots().isEmpty()) {
            return null;
        }
        String root = manifest.getConfig().getSources().getHttp().getRoots().get(0);
        return join(root, resolveServiceResourcePath(service.getPath(), relativePath));
    }

    private static String buildFileUri(ZenWaveManifest manifest, File manifestFile, ManifestService service,
                                       String relativePath, String localRoots) {
        List<String> roots = explicitLocalRoots(manifest, manifestFile, localRoots);
        if (roots.isEmpty()) {
            return null;
        }
        return join(roots.get(0), resolveServiceResourcePath(service.getPath(), relativePath));
    }

    private static String buildApicurioUri(ZenWaveManifest manifest, ManifestService service,
                                           ManifestArtifact artifact, String relativePath) {
        var source = manifest.getConfig().getSources().getApicurio();
        if (source == null || source.getRegistryUrl() == null || source.getRegistryUrl().isBlank()) {
            return null;
        }
        Map<String, String> variables = resolutionVariables(manifest, service, artifact, relativePath);
        String groupIdExpr = manifest.getConfig().getNaming().getGroupIdExpression() != null
                ? manifest.getConfig().getNaming().getGroupIdExpression()
                : "{{domainPath}}";
        String artifactIdExpr = manifest.getConfig().getNaming().getArtifactIdExpression() != null
                ? manifest.getConfig().getNaming().getArtifactIdExpression()
                : "{{artifactBaseName}}";
        String groupId = interpolate(groupIdExpr, variables);
        String artifactId = interpolate(artifactIdExpr, variables);
        Map<String, String> apicurioVariables = new LinkedHashMap<>(variables);
        apicurioVariables.put("groupId", groupId);
        apicurioVariables.put("artifactId", artifactId);
        apicurioVariables.put("branch", source.getBranch());
        String contentPath = interpolate(source.getContentUrlExpression(), apicurioVariables);
        return join(source.getRegistryUrl(), contentPath);
    }

    private static Map<String, String> resolutionVariables(ZenWaveManifest manifest, ManifestService service,
                                                           ManifestArtifact artifact, String resourcePath) {
        String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1).replace("\\", "");
        if (resourcePath.contains("\\")) {
            fileName = resourcePath.substring(resourcePath.lastIndexOf('\\') + 1);
        }
        String artifactBaseName = fileName.contains(".")
                ? fileName.substring(0, fileName.lastIndexOf('.'))
                : fileName;
        String domainPath = service.getSubdomainKey() == null
                ? service.getDomainKey() + "." + service.getServiceKey()
                : service.getDomainKey() + "." + service.getSubdomainKey() + "." + service.getServiceKey();
        Map<String, String> variables = new LinkedHashMap<>(manifest.getConfig().getProperties());
        variables.put("domain", service.getDomainKey());
        variables.put("subdomain", service.getSubdomainKey() == null ? "" : service.getSubdomainKey());
        variables.put("service", service.getServiceKey());
        variables.put("domainPath", domainPath);
        variables.put("servicePath", service.getPath());
        variables.put("artifactPath", resourcePath);
        variables.put("artifactFileName", fileName);
        variables.put("artifactBaseName", artifactBaseName);
        variables.put("artifactName", artifact != null ? artifact.getName() : artifactBaseName);
        if (artifact != null && artifact.getVersion() != null) {
            variables.put("artifactVersion", artifact.getVersion());
        }
        if (service.getVersion() != null) {
            variables.put("serviceVersion", service.getVersion());
        }
        return variables;
    }

    private static String interpolate(String expression, Map<String, String> variables) {
        String value = expression;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            value = value.replace("{{" + entry.getKey() + "}}", Objects.toString(entry.getValue(), ""));
        }
        return value;
    }

    private static List<String> explicitLocalRoots(ZenWaveManifest manifest, File manifestFile, String localRoots) {
        if (localRoots != null && !localRoots.isBlank()) {
            List<String> roots = new ArrayList<>();
            for (String root : localRoots.split(",")) {
                String trimmed = root.trim();
                if (!trimmed.isEmpty()) {
                    roots.add(asUri(trimmed));
                }
            }
            return roots;
        }
        if (manifestFile != null) {
            return List.of(manifestFile.getParentFile().toURI().toString());
        }
        try {
            return List.of(Paths.get(URI.create(manifest.getUri())).getParent().toUri().toString());
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static String asUri(String value) {
        if (hasScheme(value)) {
            return value;
        }
        return new File(value).getAbsoluteFile().toURI().toString();
    }

    private static boolean hasScheme(String value) {
        return value.matches("^[A-Za-z][A-Za-z0-9+.-]*:(//)?.*");
    }

    private static String join(String root, String child) {
        return root.replaceAll("/+$", "") + "/" + child.replaceAll("^/+", "");
    }

    private static String resolveServiceResourcePath(String servicePath, String resourcePath) {
        if (resourcePath.startsWith("/")) {
            return resourcePath;
        }
        return servicePath.replaceAll("/+$", "") + "/" + resourcePath.replaceAll("^/+", "");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String string(Object value) {
        return value != null ? value.toString() : null;
    }
}
