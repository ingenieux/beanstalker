<%-- 
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
--%>
<%@page import="java.util.Map.Entry"--%>
<%@page import="java.util.*" %>
<html>
<body>

<%
Properties p = new Properties();
p.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("version.properties"));

String appVersion = p.getProperty("app.version");
%>

<h2>Hello World! I'm version <%= appVersion %></h2>

<h2>Properties:</h2>
<pre>
<%
Properties systemProperties = System.getProperties();

for (Object kObj : systemProperties.keySet()) {
  String k = "" + kObj;
  out.println(k + "=" + systemProperties.getProperty(k) + "<br/>"); 
}
%>
</pre>

<h3>Environment:</h3>
<pre>
<%
Map<String, String> environment = System.getenv();

for (Entry<String, String> entry : environment.entrySet()) {
  out.println(String.format("%s=%s<br/>", entry.getKey(), entry.getValue())); 
}
%>
</pre>

</body>
</html>
