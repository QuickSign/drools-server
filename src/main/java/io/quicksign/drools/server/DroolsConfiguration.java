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
        for (final File fileEntry : droolsFolder.listFiles()) {
            if (fileEntry.isFile()) {
                Resource resource = ResourceFactory.newFileResource(fileEntry);
                log.info("Loading Drools resource {}", fileEntry);
                kieFileSystem.write(resource);
            }
        }
        return kieFileSystem;
    }

}
