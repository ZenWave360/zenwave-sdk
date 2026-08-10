package io.zenwave360.sdk.plugins;

import io.zenwave360.manifest.ManifestArtifact;
import io.zenwave360.manifest.ManifestDomain;
import io.zenwave360.manifest.ManifestService;
import io.zenwave360.manifest.ManifestSubdomain;
import io.zenwave360.manifest.ZenWaveManifest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mutable EventCatalog-only enrichment keyed by the typed manifest model.
 *
 * Manifest identity, hierarchy, documents, and artifacts stay in manifest-core. This model
 * contains only data derived while parsing those artifacts for EventCatalog.
 */
final class EventCatalogModel {

    private final Map<String, Map<String, Object>> domainData = new LinkedHashMap<>();
    private final Map<String, Map<String, Object>> subdomainData = new LinkedHashMap<>();
    private final Map<String, Map<String, Object>> serviceData = new LinkedHashMap<>();
    private final Map<String, Map<ManifestArtifact, Map<String, Object>>> artifactData = new LinkedHashMap<>();

    EventCatalogModel(ZenWaveManifest manifest) {
        for (ManifestDomain domain : manifest.getDomains()) {
            domainData.put(domain.getKey(), new LinkedHashMap<>());
            for (ManifestSubdomain subdomain : domain.getSubdomains()) {
                subdomainData.put(subdomainKey(domain, subdomain), new LinkedHashMap<>());
            }
        }
        for (ManifestService service : manifest.getServices()) {
            serviceData.put(service.getServiceRef(), new LinkedHashMap<>());
            artifactData.put(service.getServiceRef(), new LinkedHashMap<>());
        }
    }

    Map<String, Object> domainData(ManifestDomain domain) {
        return domainData.computeIfAbsent(domain.getKey(), ignored -> new LinkedHashMap<>());
    }

    Map<String, Object> subdomainData(ManifestDomain domain, ManifestSubdomain subdomain) {
        return subdomainData.computeIfAbsent(subdomainKey(domain, subdomain), ignored -> new LinkedHashMap<>());
    }

    Map<String, Object> serviceData(ManifestService service) {
        return serviceData.computeIfAbsent(service.getServiceRef(), ignored -> new LinkedHashMap<>());
    }

    Map<String, Object> artifactData(ManifestService service, ManifestArtifact artifact) {
        return artifactData
                .computeIfAbsent(service.getServiceRef(), ignored -> new LinkedHashMap<>())
                .computeIfAbsent(artifact, ignored -> new LinkedHashMap<>());
    }

    String catalogServiceId(ManifestService service) {
        return service.getId().equals(service.getServiceKey())
                ? service.getServiceRef().replace('/', '.')
                : service.getId();
    }

    String catalogSubdomainId(ManifestDomain domain, ManifestSubdomain subdomain) {
        return subdomain.getId().equals(subdomain.getKey())
                ? domain.getId() + "." + subdomain.getId()
                : subdomain.getId();
    }

    private String subdomainKey(ManifestDomain domain, ManifestSubdomain subdomain) {
        return domain.getKey() + "/" + subdomain.getKey();
    }
}
