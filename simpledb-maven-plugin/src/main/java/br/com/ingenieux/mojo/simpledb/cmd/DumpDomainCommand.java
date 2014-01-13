package br.com.ingenieux.mojo.simpledb.cmd;

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

import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.SelectRequest;
import com.amazonaws.services.simpledb.model.SelectResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.codehaus.plexus.util.IOUtil;

import java.io.FileWriter;
import java.io.Writer;
import java.util.*;

import static org.apache.commons.lang.StringUtils.isBlank;

public class DumpDomainCommand {
    final ObjectMapper mapper = new ObjectMapper();

    final AmazonSimpleDB service;

    public DumpDomainCommand(AmazonSimpleDB service) {
        super();
        this.service = service;
    }

    public boolean execute(DumpDomainContext context) throws Exception {
        SelectRequest selectRequest = new SelectRequest(String.format("SELECT * FROM %s", context.getDomain()));
        SelectResult selectResult = service.select(selectRequest);

        ArrayNode rootNode = mapper.createArrayNode();

        while (!selectResult.getItems().isEmpty()) {
            for (Item item : selectResult.getItems())
                appendResult(rootNode, item);
                    
            if (isBlank(selectResult.getNextToken()))
                break;

            selectResult = service.select(selectRequest.withNextToken(selectResult.getNextToken()));
        }
        
        FileWriter writer = new FileWriter(context.getOutputFile());
        
        writeData(rootNode, writer);
        
        IOUtil.close(writer);

        return false;
    }

    private void writeData(JsonNode rootNode, Writer fileWriter) throws Exception {
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        
        writer.writeValue(fileWriter, rootNode);
    }

    private void appendResult(ArrayNode rootNode, Item item) {
        ObjectNode rootObjectNode = mapper.createObjectNode();
        
        rootObjectNode.put("name", item.getName());
        
        Map<String, List<String>> replaceAttributes = new TreeMap<String, List<String>>();

        /*
         * Zips It
         */
        for (Attribute a : item.getAttributes()) {
            String key = a.getName();
            String value = a.getValue();
            
            if (replaceAttributes.containsKey(key)) {
                replaceAttributes.get(key).add(value);
            } else {
                replaceAttributes.put(key, new ArrayList<String>(Arrays.asList(value)));
            }
        }
        
        /*
         * Formats and dumps
         */
        
        ObjectNode rootReplaceNode = mapper.createObjectNode();
        
        for (String k : replaceAttributes.keySet()) {
            List<String> valueList = replaceAttributes.get(k);
            
            if (1 == valueList.size()) {
                rootReplaceNode.put(k, valueList.get(0));
            } else {
                ArrayNode valueArrayNode = mapper.createArrayNode();
                
                for (String value : valueList)
                    valueArrayNode.add(value);
                        
                rootReplaceNode.put(k, valueArrayNode);
            }
        }
        
        ArrayNode replaceNode = mapper.createArrayNode();
        
        replaceNode.add(rootReplaceNode);
        
        rootObjectNode.put("replace", replaceNode);
        
        rootNode.add(rootObjectNode);
    }
}
