/*
 * Copyright (c) 2016 ingenieux Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package br.com.ingenieux.mojo.lambda;

import com.google.common.collect.Lists;

import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.FunctionConfiguration;
import com.amazonaws.services.lambda.model.ListAliasesRequest;
import com.amazonaws.services.lambda.model.ListFunctionsRequest;
import com.amazonaws.services.lambda.model.ListFunctionsResult;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import br.com.ingenieux.mojo.aws.util.GlobUtil;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

/**
 * List Function Aliases
 */
@Mojo(name = "list-aliases")
public class ListAliasesMojo extends AbstractLambdaMojo {
  public static final Pattern PATTERN_ALIAS = Pattern.compile("(?!^[0-9]+$)([a-zA-Z0-9-_]+)");
  public static final String
      PATTERN_ALIAS_ARN =
      "arn:aws:lambda:[a-z]{2}-[a-z]+-\\d{1}:\\d{12}:function:[a-zA-Z0-9-_]+:(\\$LATEST|[a-zA-Z0-9-_]+)";

  private AWSLambdaClient lambdaClient;

  /**
   * Glob of Functions to Include (default: all)
   */
  @Parameter(property = "lambda.function.includes", defaultValue="*")
  List<String> includes;

  List<Pattern> globIncludes;

  @Override
  protected void configure() {
    super.configure();

    try {
      configureInternal();
    } catch (Exception exc) {
      throw new RuntimeException(exc);
    }
  }

  private void configureInternal() throws MojoExecutionException {
    lambdaClient = this.getService();

    globIncludes = Lists.transform(includes, GlobUtil::globify);
  }

  @Override
  protected Object executeInternal() throws Exception {
    Map<String, Set<String>> aliasMap = fetchAliases();

    return aliasMap;
  }

  private Map<String, Set<String>> fetchAliases() {
    Map<String, Set<String>> aliases = new LinkedHashMap<String, Set<String>>();

    String marker = null;

    do {
      final ListFunctionsResult
          listFunctionsResult =
          lambdaClient.listFunctions(new ListFunctionsRequest().withMarker(marker));

      listFunctionsResult.
          getFunctions().
          stream().
          map(FunctionConfiguration::getFunctionName).
          filter(this::isItIncluded).
          forEach(func -> {
        Set<String> aliasesSet = lambdaClient.listAliases(new ListAliasesRequest().withFunctionName(func)).
            getAliases().
            stream().
            map(x -> x.getAliasArn().replaceAll(PATTERN_ALIAS_ARN, "$1")).
            collect(Collectors.toSet());

        aliases.put(func, aliasesSet);
      });

      marker = listFunctionsResult.getNextMarker();

    } while (isNotEmpty(marker));

    return aliases;
  }

  public boolean isItIncluded(String functionArn) {
    for (Pattern p : globIncludes) {
      if (p.matcher(functionArn).matches()) {
        return true;
      }
    }

    return false;
  }
}
