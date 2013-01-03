#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ${package}.fixture.ServerRule;

import com.jayway.restassured.RestAssured;

public class ServerTest {
	@Rule
	public ServerRule serverRule = new ServerRule(7999);
	
	@Before
	public void before() {
		RestAssured.baseURI = "http://127.0.0.1:7999/services/api/v1";
	}

	@Test
	public void debugTest() {
		given().
			log().all().
		expect().
			log().all().
			statusCode(200).
		when().
			get("/debug");
	}
	
	@Test
	public void failedHealthCheckTest() {
		expect().
			statusCode(405).
		when().
			get("/health/check");
	}
	
	@Test
	public void properHealthCheckTest() {
		expect().
			statusCode(200).
		when().
			head("/health/check");
	}

}
