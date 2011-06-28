<%@page import="java.util.Properties"%>
<html>
<body>

<%
Properties p = new Properties();
p.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("version.properties"));

String appVersion = p.getProperty("app.version");
%>

<h2>Hello World! I'm version <%= appVersion %></h2>

<pre>
<%
Properties systemProperties = System.getProperties();

for (Object kObj : systemProperties.keySet()) {
  String k = "" + kObj;
  out.println(k + "=" + systemProperties.getProperty(k) + "\n<br/>"); 
}
%>
</pre>


</body>
</html>
