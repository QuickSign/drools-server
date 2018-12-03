package io.quicksign.drools.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.Arrays;
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
    public ObjectNode execute(@RequestBody ArrayNode input, @RequestParam("outputType") String outputTypeFqn) throws Exception {
        StatelessKieSession ksession = kc.newStatelessKieSession();

        //now create some test data
        TypeName outputTypeName = extractTypeName(outputTypeFqn);
        KieBase kb = kc.getKieBase();
        FactType outputType = kb.getFactType( outputTypeName.packageName, outputTypeName.typeName);
        checkNotNull(outputType, "Unknown output type %s", outputTypeName);

        Object outputFact = outputType.newInstance();

        List<Object> facts = new ArrayList<>();
        for (JsonNode node : input) {
            JsonNode typeNode = node.get("_type");
            checkNotNull(typeNode, "Missing _type information");
            String type = typeNode.asText();

            Map<String, Object> result = mapper.convertValue(node, Map.class);
            result.remove("_type");

            TypeName typeName = extractTypeName(type);

            FactType factType = kb.getFactType(typeName.packageName, typeName.typeName);
            checkNotNull(factType, "Unknown input type %s", typeName);

            Object inputFact = factType.newInstance();
            factType.setFromMap(inputFact, result);
            facts.add(inputFact);
        }

        facts.add(outputFact);

        ksession.execute( facts );
        Map<String, Object> outputMap = outputType.getAsMap(outputFact);
        ObjectNode outputJson = mapper.convertValue(outputMap, ObjectNode.class);
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

    @PostMapping("/test")
    public ObjectNode test() throws Exception {
        StatelessKieSession ksession = kc.newStatelessKieSession( "default");

        //now create some test data
        FactType driverType = kc.getKieBase("default").getFactType( "io.quicksign.drools.server",
                "Driver" );
        FactType policyType = kc.getKieBase("default").getFactType( "io.quicksign.drools.server",
                "Policy" );
        Object driver = driverType.newInstance();
        Object policy = policyType.newInstance();

        ksession.execute( Arrays.asList( new Object[]{driver, policy} ) );

        Map<String, Object> output = policyType.getAsMap(policy);
        ObjectNode jsonNode = mapper.convertValue(output, ObjectNode.class);
        return jsonNode;

    }


}
