package io.zenwave360.sdk.plugins;

import io.zenwave360.manifest.ManifestArtifact;
import io.zenwave360.manifest.ManifestLoadOptions;
import io.zenwave360.manifest.ManifestReferenceResolver;
import io.zenwave360.manifest.ManifestResolvedResource;
import io.zenwave360.manifest.ManifestService;
import io.zenwave360.manifest.ZenWaveManifest;
import io.zenwave360.manifest.ZenWaveManifestLoader;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    static ManifestLoadOptions contentOptions(String preferredSource, Boolean allowFallback) {
        return new ManifestLoadOptions(
                blankToNull(preferredSource),
                allowFallback == null || allowFallback);
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

    static String resolveLinkUri(ZenWaveManifestLoader loader, ZenWaveManifest manifest, ManifestService service,
                                 ManifestArtifact artifact, String relativePath, String linkSource) {
        ManifestLoadOptions options = new ManifestLoadOptions(blankToNull(linkSource), false);
        List<ManifestResolvedResource> candidates = loader.buildArtifactCandidates(manifest, service, artifact, options);
        if (candidates.isEmpty()) {
            return null;
        }
        String artifactUri = candidates.get(0).getUri();
        if (relativePath == null || relativePath.equals(artifact.getPath())) {
            return artifactUri;
        }
        return ManifestReferenceResolver.INSTANCE.resolveReference(artifactUri, relativePath);
    }

    static String resolveContentPath(ZenWaveManifestLoader loader, ZenWaveManifest manifest, ManifestService service,
                                     ManifestArtifact artifact, ManifestLoadOptions options) {
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String string(Object value) {
        return value != null ? value.toString() : null;
    }
}
