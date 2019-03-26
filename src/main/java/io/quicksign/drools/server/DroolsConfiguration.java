package io.quicksign.drools.server;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

@Configuration
public class DroolsConfiguration {

    private static Logger log = LoggerFactory.getLogger(DroolsConfiguration.class);

    @Bean
    public KieContainer kieContainer(KieServices kieServices, KieFileSystem kfs) {
        KieRepository kieRepository = kieServices.getRepository();

        kieRepository.addKieModule(() -> kieRepository.getDefaultReleaseId());

        KieBuilder kieBuilder = kieServices
                .newKieBuilder(kfs)
                .buildAll();

        return kieServices.newKieContainer(kieRepository.getDefaultReleaseId());
    }

    @Bean
    public KieServices kieServices() {
        return KieServices.Factory.get();
    }

    @Value("${drools.folder}")
    private File droolsFolder;

    @Bean
    public KieFileSystem kieFileSystem(KieServices kieServices) throws IOException {
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        loadFilesRecursively(kieFileSystem, droolsFolder);
        return kieFileSystem;
    }

    private static void loadFilesRecursively(KieFileSystem kieFileSystem, File file) {
        if (file.isFile()) {
            Resource resource = ResourceFactory.newFileResource(file);
            log.info("Loading Drools resource {}", file);
            kieFileSystem.write(resource);
        } else if (file.isDirectory()) {
            for (final File entry : file.listFiles(filter())) {
                loadFilesRecursively(kieFileSystem, entry);
            }
        }
    }

    private static FilenameFilter filter() {
        return (dir, name) -> !name.startsWith("~");
    }

}
