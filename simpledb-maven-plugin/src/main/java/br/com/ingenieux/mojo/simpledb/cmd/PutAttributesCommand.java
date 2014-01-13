package br.com.ingenieux.mojo.simpledb.cmd;

import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.UpdateCondition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class PutAttributesCommand {

    final AmazonSimpleDB service;

    public PutAttributesCommand(AmazonSimpleDB service) {
        this.service = service;
    }

    public void execute(PutAttributesContext ctx) throws Exception {
        if (ctx.isCreateDomainIfNeeded())
            createIfNeeded(ctx);

        ArrayNode attributesArray = (ArrayNode) new ObjectMapper().readTree(ctx.getSource());

        for (int i = 0; i < attributesArray.size(); i++) {
            ObjectNode putAttributeNode = (ObjectNode) attributesArray.get(i);

            putAttribute(ctx, putAttributeNode);
        }
    }

    private void createIfNeeded(PutAttributesContext ctx) {
        Set<String> domainSet = new TreeSet<String>(service.listDomains().getDomainNames());

        if (!domainSet.contains(ctx.getDomain()))
            service.createDomain(new CreateDomainRequest(ctx.domain));
    }

    private void putAttribute(PutAttributesContext ctx, ObjectNode objectNode) {
        PutAttributesRequest request = new PutAttributesRequest();

        request.setDomainName(ctx.getDomain());

        Iterator<String> itFieldName = objectNode.fieldNames();

        while (itFieldName.hasNext()) {
            String key = itFieldName.next();

            if ("name".equals(key)) {
                String value = objectNode.get("name").textValue();

                request.setItemName(value);
            } else if ("append".equals(key) || "replace".equals(key)) {
                boolean replaceP = "replace".equals(key);

                ArrayNode attributesNode = (ArrayNode) objectNode.get(key);

                Collection<ReplaceableAttribute> value = getAttributesFrom(attributesNode, replaceP);

                request.getAttributes().addAll(value);
            } else if ("expect".equals(key)) {
                ObjectNode expectNode = (ObjectNode) objectNode.get("expect");

                request.setExpected(getUpdateCondition(expectNode));
            }
        }

        service.putAttributes(request);
    }

    private UpdateCondition getUpdateCondition(ObjectNode expectNode) {
        String nameParameter = expectNode.get("name").asText();
        String valueParameter = null;
        Boolean existsParameter = null;

        if (null != expectNode.get("value"))
            valueParameter = expectNode.get("value").asText();

        if (null != expectNode.get("exists"))
            existsParameter = Boolean.valueOf("" + expectNode.get("exists"));

        return new UpdateCondition(nameParameter, valueParameter, existsParameter);
    }

    private Collection<ReplaceableAttribute> getAttributesFrom(ArrayNode attributesNode, boolean replaceP) {
        List<ReplaceableAttribute> attributeList = new ArrayList<ReplaceableAttribute>();

        for (int i = 0; i < attributesNode.size(); i++) {
            ObjectNode objectNode = (ObjectNode) attributesNode.get(i);

            Iterator<String> itFieldName = objectNode.fieldNames();
            while (itFieldName.hasNext()) {
                String key = itFieldName.next();
                JsonNode valueNode = objectNode.get(key);

                if (valueNode.isValueNode()) {
                    attributeList.add(new ReplaceableAttribute(key, valueNode.asText(), replaceP));
                } else if (valueNode.isArray()) {
                    for (int j = 0; j < valueNode.size(); j++) {
                        JsonNode scalarValueNode = valueNode.get(j);

                        attributeList.add(new ReplaceableAttribute(key, scalarValueNode.asText(), replaceP));
                    }
                }
            }
        }

        return attributeList;
    }
}
