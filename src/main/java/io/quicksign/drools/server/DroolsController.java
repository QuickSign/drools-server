package io.quicksign.drools.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.*;
import org.kie.api.KieBase;
import org.kie.api.definition.type.FactType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

@RestController
public class DroolsController {

    @Autowired
    private KieContainer kc;

    @Autowired
    private ObjectMapper mapper;

    static class TypeName {
        public String packageName;
        public String typeName;

        @Override
        public String toString() {
            return packageName + "." + typeName;
        }
    }

    @PostMapping("/")
    @ApiOperation(value = "Execute Drools stateless session", notes = "Pass an array of input facts, each fact must define its _type")
    public ObjectNode execute(
            @ApiParam(value = "List of typed input facts objects. Each object must define its type using a _typed property", required = true)
            @RequestBody ArrayNode inputFacts,

            @ApiParam(value = "Output fact type", required = true)
            @RequestParam(name="outputType") String outputTypeFqn) throws Exception {

        StatelessKieSession ksession = kc.newStatelessKieSession();

        //now create some test data
        TypeName outputTypeName = extractTypeName(outputTypeFqn);
        KieBase kb = kc.getKieBase();
        FactType outputType = kb.getFactType( outputTypeName.packageName, outputTypeName.typeName);
        checkNotNull(outputType, "Unknown output type %s", outputTypeName);

        Object outputFact = outputType.newInstance();

        List<Object> facts = new ArrayList<>();
        for (JsonNode node : inputFacts) {
            JsonNode typeNode = node.get("_type");
            checkNotNull(typeNode, "Missing _type information");
            String type = typeNode.asText();

            Map<String, Object> result = mapper.convertValue(node, Map.class);
            result.remove("_type");

            TypeName typeName = extractTypeName(type);

            FactType factType = kb.getFactType(typeName.packageName, typeName.typeName);
            checkNotNull(factType, "Unknown input type %s", typeName);

            Class<?> factClass = factType.getFactClass();
            Object inputFact = mapper.convertValue(result, factClass);

            // convert from string to Date
            factType.setFromMap(inputFact, result);

            facts.add(inputFact);
        }

        facts.add(outputFact);

        ksession.execute( facts );
        ObjectNode outputJson = mapper.convertValue(outputFact, ObjectNode.class);
        return outputJson;
    }

    private TypeName extractTypeName(String type) {
        int i = type.lastIndexOf(".");
        String packageName = type.substring(0, i);
        String typeName = type.substring(i + 1);
        TypeName typeName_ = new TypeName();
        typeName_.packageName = packageName;
        typeName_.typeName = typeName;
        return typeName_;
    }

}
