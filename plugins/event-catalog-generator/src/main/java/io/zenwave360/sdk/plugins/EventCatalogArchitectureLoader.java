package io.zenwave360.sdk.plugins;

import io.zenwave360.manifest.ManifestArtifact;
import io.zenwave360.manifest.ManifestDomain;
import io.zenwave360.manifest.ManifestService;
import io.zenwave360.manifest.ManifestSubdomain;
import io.zenwave360.manifest.ZenWaveManifest;
import io.zenwave360.manifest.ZenWaveManifestLoader;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.processors.Processor;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads {@code zenwave-architecture.yml} through {@code manifest-core} and adapts the
 * normalized manifest into the map-based context model used by the EventCatalog pipeline.
 */
public class EventCatalogArchitectureLoader implements Processor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ZenWaveManifestLoader manifestLoader = new ZenWaveManifestLoader();

    @DocumentedOption(description = "Path to the zenwave-architecture.yml master file.")
    public String inputFile;

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        File file = new File(inputFile);
        if (!file.exists()) {
            throw new RuntimeException("zenwave-architecture.yml not found: " + file.getAbsolutePath());
        }

        ZenWaveManifest manifest = loadManifest(file);
        Map<String, Object> architecture = toArchitectureMap(manifest);

        contextModel.put("manifest", manifest);
        contextModel.put("manifestLoader", manifestLoader);
        contextModel.put("manifestFile", file.getAbsoluteFile());
        contextModel.put("architecture", architecture);
        return contextModel;
    }

    private ZenWaveManifest loadManifest(File file) {
        try {
            return BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, new Function2<CoroutineScope, kotlin.coroutines.Continuation<? super ZenWaveManifest>, Object>() {
                @Override
                public Object invoke(CoroutineScope scope, kotlin.coroutines.Continuation<? super ZenWaveManifest> continuation) {
                    return manifestLoader.load(file.toURI().toString(), continuation);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Cannot load " + inputFile + ": " + e.getMessage(), e);
        }
    }

    private Map<String, Object> toArchitectureMap(ZenWaveManifest manifest) {
        Map<String, Object> architecture = new LinkedHashMap<>();
        architecture.put("config", toConfigMap(manifest));

        Map<String, Object> domains = new LinkedHashMap<>();
        Map<String, Object> services = new LinkedHashMap<>();

        for (ManifestDomain domain : manifest.getDomains()) {
            Map<String, Object> domainMap = toDomainMap(manifest, domain, services);
            domains.put(domain.getKey(), domainMap);
        }

        architecture.put("domains", domains);
        architecture.put("services", services);

        manifest.getDiagnostics().forEach(diagnostic ->
                log.warn("Manifest diagnostic [{}] at {}: {}",
                        diagnostic.getCode(),
                        diagnostic.getLocation(),
                        diagnostic.getMessage()));

        return architecture;
    }

    private Map<String, Object> toConfigMap(ZenWaveManifest manifest) {
        Map<String, Object> config = new LinkedHashMap<>();
        if (manifest.getConfig().getTitle() != null) {
            config.put("title", manifest.getConfig().getTitle());
        }
        if (manifest.getConfig().getVersion() != null) {
            config.put("version", manifest.getConfig().getVersion());
        }
        if (!manifest.getConfig().getProperties().isEmpty()) {
            config.put("properties", new LinkedHashMap<>(manifest.getConfig().getProperties()));
        }
        if (!manifest.getConfig().getSourcePriority().isEmpty()) {
            config.put("sourcePriority", new ArrayList<>(manifest.getConfig().getSourcePriority()));
        }
        return config;
    }

    private Map<String, Object> toDomainMap(ZenWaveManifest manifest, ManifestDomain domain, Map<String, Object> flattenedServices) {
        Map<String, Object> domainMap = new LinkedHashMap<>();
        putIfNotNull(domainMap, "id", defaultString(domain.getId(), domain.getKey()));
        putIfNotNull(domainMap, "name", domain.getName());
        putIfNotNull(domainMap, "description", domain.getDescription());

        if (!domain.getServices().isEmpty()) {
            Map<String, Object> servicesMap = new LinkedHashMap<>();
            for (ManifestService service : domain.getServices()) {
                Map<String, Object> serviceMap = toServiceMap(manifest, service);
                servicesMap.put(service.getServiceKey(), serviceMap);
                flattenedServices.put(serviceKey(service), serviceMap);
            }
            domainMap.put("services", servicesMap);
        }

        if (!domain.getSubdomains().isEmpty()) {
            Map<String, Object> subdomains = new LinkedHashMap<>();
            for (ManifestSubdomain subdomain : domain.getSubdomains()) {
                Map<String, Object> subdomainMap = toSubdomainMap(manifest, subdomain, flattenedServices);
                subdomains.put(subdomain.getKey(), subdomainMap);
            }
            domainMap.put("subdomains", subdomains);
        }

        return domainMap;
    }

    private Map<String, Object> toSubdomainMap(ZenWaveManifest manifest, ManifestSubdomain subdomain, Map<String, Object> flattenedServices) {
        Map<String, Object> subdomainMap = new LinkedHashMap<>();
        putIfNotNull(subdomainMap, "id", defaultString(subdomain.getId(), subdomain.getKey()));
        putIfNotNull(subdomainMap, "name", subdomain.getName());
        putIfNotNull(subdomainMap, "description", subdomain.getDescription());

        if (!subdomain.getServices().isEmpty()) {
            Map<String, Object> servicesMap = new LinkedHashMap<>();
            for (ManifestService service : subdomain.getServices()) {
                Map<String, Object> serviceMap = toServiceMap(manifest, service);
                servicesMap.put(service.getServiceKey(), serviceMap);
                flattenedServices.put(serviceKey(service), serviceMap);
            }
            subdomainMap.put("services", servicesMap);
        }

        return subdomainMap;
    }

    private Map<String, Object> toServiceMap(ZenWaveManifest manifest, ManifestService service) {
        Map<String, Object> serviceMap = new LinkedHashMap<>();

        String serviceId = defaultString(service.getId(), service.getServiceRef().replace('/', '.'));
        serviceMap.put("id", serviceId);
        serviceMap.put("serviceRef", service.getServiceRef());
        putIfNotNull(serviceMap, "version", service.getVersion());
        putIfNotNull(serviceMap, "name", service.getName());
        putIfNotNull(serviceMap, "description", service.getDescription());
        serviceMap.put("domain", service.getDomainKey());
        if (service.getSubdomainKey() != null) {
            serviceMap.put("subdomain", service.getSubdomainKey());
        }
        serviceMap.put("path", service.getPath());

        if (!service.getDocs().isEmpty()) {
            serviceMap.put("docs", new LinkedHashMap<>(service.getDocs()));
        }

        if (!service.getArtifacts().isEmpty()) {
            List<Map<String, Object>> artifacts = new ArrayList<>();
            for (ManifestArtifact artifact : service.getArtifacts()) {
                Map<String, Object> artifactMap = new LinkedHashMap<>();
                artifactMap.put("name", artifact.getName());
                artifactMap.put("type", artifact.getType());
                artifactMap.put("path", artifact.getPathExpression());
                putIfNotNull(artifactMap, "version", artifact.getVersion());
                artifacts.add(artifactMap);
            }
            serviceMap.put("artifacts", artifacts);
        }

        if (manifest != null && !service.getConsumers().isEmpty()) {
            List<String> consumers = new ArrayList<>();
            for (String consumerRef : service.getConsumers()) {
                ManifestService consumerService = manifest.findService(consumerRef);
                consumers.add(consumerReferenceToId(consumerRef, consumerService));
            }
            serviceMap.put("consumers", consumers);
        }

        return serviceMap;
    }

    private String serviceKey(ManifestService service) {
        return defaultString(service.getId(), service.getServiceRef().replace('/', '.'));
    }

    private String consumerReferenceToId(String consumerRef, ManifestService consumerService) {
        if (consumerService != null) {
            return defaultString(consumerService.getId(), consumerService.getServiceRef().replace('/', '.'));
        }
        return consumerRef != null ? consumerRef.replace('/', '.') : null;
    }

    private String defaultString(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
