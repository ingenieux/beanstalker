<%@page import="java.util.Map.Entry"%>
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
