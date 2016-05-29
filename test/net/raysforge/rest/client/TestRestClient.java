package net.raysforge.rest.client;

import java.io.IOException;

import net.raysforge.rest.client.GenericRestClient.Auth;

public class TestRestClient {
	final static String URL = "http://localhost:8000/LATEST/documents?uri="; 
	final static String User = "Admin"; 
	final static String Pass = "Admin"; 

	public static void main(String[] args) throws IOException {
		
		GenericRestClient rc = new GenericRestClient(URL, User, Pass, Auth.Digest);
		
		rc.postData("PUT", "/test.json", "{name: \"Iced Mocha\", size: \"Grandé\", tasty: true}");
		
		Object data = rc.getData("/test.json");
		System.out.println(data);
		
		
	}

}
