package io.quicksign.drools.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.diff.JsonDiff;
import io.swagger.annotations.*;
import org.kie.api.KieBase;
import org.kie.api.definition.type.FactType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping(value = "/", produces = "application/json")
    @ApiOperation(value = "Execute Drools stateless session", notes = "Pass an array of facts, each fact must define its _type")
    public JsonNode execute(
            @ApiParam(value = "List of typed facts objects. Each object must define its type using a _type property", required = true)
            @RequestBody ArrayNode factNodes) throws Exception {

        ArrayNode outputJson = doExecute(factNodes);
        return outputJson;
    }

    @PostMapping(value = "/_diff", produces = "application/json-patch+json")
    @ApiOperation(value = "Execute Drools stateless session and returns the facts as a RFC 6902 JSON Patch", notes = "Pass an array of facts, each fact must define its _type")
    public JsonNode executePatch(
            @ApiParam(value = "List of typed facts objects. Each object must define its type using a _type property", required = true)
            @RequestBody ArrayNode factNodes) throws Exception {

        ArrayNode outputJson = doExecute(factNodes);
        final JsonNode patch = JsonDiff.asJson(factNodes, outputJson);
        return patch;
    }

    private ArrayNode doExecute(@ApiParam(value = "List of typed facts objects. Each object must define its type using a _type property", required = true) @RequestBody ArrayNode factNodes) {
        StatelessKieSession ksession = kc.newStatelessKieSession();

        KieBase kb = kc.getKieBase();

        List<Object> facts = new ArrayList<>();
        for (JsonNode node : factNodes) {
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

            facts.add(inputFact);
        }

        ksession.execute( facts );
        ArrayNode outputJson = mapper.createArrayNode();
        JsonNodeFactory nodeFactory = mapper.getNodeFactory();
        for (Object fact : facts) {
            ObjectNode node = mapper.convertValue(fact, ObjectNode.class);
            node.set("_type", nodeFactory.textNode(fact.getClass().getName()));
            outputJson.add(node);
        }
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
