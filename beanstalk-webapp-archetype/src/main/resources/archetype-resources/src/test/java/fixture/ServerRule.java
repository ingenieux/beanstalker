#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.fixture;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.rules.ExternalResource;

public class ServerRule extends ExternalResource {
	private int port;

	private final String warPath;

	private final String contextPath;

	private Server server;

	public ServerRule() {
		this(8080, "/");
	}
	
	public ServerRule(int port) {
		this(port, "/");
	}

	public ServerRule(int port, String contextPath) {
		this(port, "src/main/webapp", contextPath);
	}

	public ServerRule(int port, String warPath, String contextPath) {
		this.port = port;
		this.warPath = warPath;
		this.contextPath = contextPath;
	}

	public void start() throws Exception {
		server = new Server();

		WebAppContext wac = new WebAppContext();
		wac.setContextPath(contextPath);
		wac.setWar(warPath);
		server.setHandler(wac);

		SelectChannelConnector scc = new SelectChannelConnector();
		scc.setPort(port);
		server.addConnector(scc);
		server.start();
	}

	public static void main(String[] args) throws Exception {
		ServerRule webStart = new ServerRule();

		webStart.start();

		System.err.println("Running. Hit <Enter> to Stop");

		new BufferedReader(new InputStreamReader(System.in)).readLine();

		webStart.stop();

	}

	public void stop() throws Exception {
		//server.join();
		server.stop();
	}

	@Override
	protected void before() throws Throwable {
		start();
	}

	@Override
	protected void after() {
		try {
			stop();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}