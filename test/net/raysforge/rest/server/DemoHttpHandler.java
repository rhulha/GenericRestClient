package net.raysforge.rest.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import net.raysforge.rest.server.HttpResponse;
import net.raysforge.rest.server.SimpleHttpRequestHandler;



public class DemoHttpHandler extends SimpleHttpRequestHandler {

	public DemoHttpHandler(Socket client) {
		super(client);
	}

	@Override
	public HttpResponse handleGET(String path, String queryString) throws IOException {
		return new HttpResponse("cool path: " + path);
	}

	@Override
	public HttpResponse handleDELETE(String path, String queryString) throws IOException {
		return new HttpResponse("cool path: " + path);
	}

	@Override
	public HttpResponse handlePUT(String path, String queryString, String body) throws IOException {
		return new HttpResponse("cool path: " + path);
	}

	@Override
	public HttpResponse handlePOST(String path, String queryString, String body) throws IOException {
		return new HttpResponse("cool path: " + path);
	}

	public static void main(String[] args) throws IOException {
		try (ServerSocket ss = new ServerSocket(10000)) {
			System.out.println("Ready on: " + 10000);
			while (true) {
				new Thread(new DemoHttpHandler(ss.accept())).start();
			}
		}
	}

}
