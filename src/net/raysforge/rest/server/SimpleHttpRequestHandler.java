package net.raysforge.rest.server;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.HashMap;

import net.raysforge.commons.Codecs;
import net.raysforge.commons.StreamUtils;

public abstract class SimpleHttpRequestHandler implements Runnable {

	protected Socket client;
	public boolean debug = false;
	private String username = "";
	private String password = "";
	private HashMap<String, String> headerMap;

	public SimpleHttpRequestHandler(Socket client) {
		this.client = client;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public HashMap<String, String> getHeaderMap() {
		return headerMap;
	}
	
	private HashMap<String, String> getHeaderMap(InputStream is) throws IOException {
		HashMap<String, String> headerMap = new HashMap<String, String>();
		for (String line; (line = StreamUtils.readOneLineISO88591(is)) != null;) {
			if (line.length() == 0)
				break;
			if (debug)
				System.out.println("line: " + line);
			String[] headerParts = line.split(": ");
			headerMap.put(headerParts[0].toLowerCase(), headerParts[1]);
		}
		return headerMap;
	}

	private void parseAuthorization(HashMap<String, String> headerMap) {
		String authorization = headerMap.get("authorization");
		if (authorization == null || authorization.length() == 0) {
			if (debug)
				System.out.println("authorization missing.");
			return;
		}
		String[] split = new String(Codecs.fromBase64(authorization.substring(6)), Charset.forName("ISO-8859-1")).split(":");
		username = split[0];
		password = split[1];
	}

	@Override
	public void run() {
		try {
			run2();
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void write(OutputStream os, String s) throws IOException {
		os.write(s.getBytes(Charset.forName("UTF-8")));
	}

	public void run2() throws IOException {
		try (InputStream httpIS = client.getInputStream(); OutputStream httpOS = client.getOutputStream();) {; 
			String line = StreamUtils.readOneLineISO88591(httpIS);
			if(line==null)
				return;
			String[] requestLine = line.split(" ");
			String method = requestLine[0];
			String path = requestLine[1];
			//String version = split[2];

			headerMap = getHeaderMap(httpIS);
			parseAuthorization(headerMap);

			String contentLengthStr = headerMap.get("content-length");
			int contentLength = 0;
			if (contentLengthStr != null) {
				contentLength = Integer.parseInt(contentLengthStr);
			}

			byte[] bodyBytes = new byte[contentLength];
			if (httpIS.read(bodyBytes) != contentLength) // TODO: this is not guaranteed to work
				throw new RuntimeException("read(cbuf) != contLen");
			String body = new String(bodyBytes, Charset.forName("UTF-8"));

			String queryString = "";
			if (path.contains("?")) {
				queryString = path.substring(path.indexOf('?') + 1);
				path = path.substring(0, path.indexOf('?'));
			}

			HttpResponse response;
			switch (method) {
				case "GET":
					response = handleGET(path, queryString);
					break;
				case "PUT":
					response = handlePUT(path, queryString, body);
					break;
				case "POST":
					response = handlePOST(path, queryString, body);
					break;
				case "DELETE":
					response = handleDELETE(path, queryString);
					break;
				default:
					response = new HttpResponse(500, "unknown method: " + method);
					break;
			}

			if (!response.error) {
				write(httpOS, "HTTP/1.0 " + response.statusCode + " OK\n");
				//w.write("Vary: Accept-Encoding");
				if(response.isBinary) {
					write(httpOS, "Content-Length: " + response.bytes.length + "\n");
				} else {
					write(httpOS, "Content-Length: " + response.message.getBytes(Charset.forName("UTF-8")).length + "\n");
				}
				//w.write("Keep-Alive: timeout=2, max=999");
				write(httpOS, "Connection: Close\n"); // Keep-Alive
				write(httpOS, "Content-Type: "+response.contentType+"\n\n");
				if(response.isBinary) {
					httpOS.write(response.bytes);
				} else {
					write(httpOS, response.message);
				}
				
			} else {
				System.out.println("ERROR: " + response);
				write(httpOS, "HTTP/1.0 " + response.statusCode + " " + response.message + "\n");
				write(httpOS, "Content-Length: 0\n");
				write(httpOS, "Connection: Close\n\n"); // Keep-Alive ?
			}
		}
	}

	abstract public HttpResponse handleGET(String path, String queryString) throws IOException;

	abstract public HttpResponse handleDELETE(String path, String queryString) throws IOException;

	abstract public HttpResponse handlePUT(String path, String queryString, String body) throws IOException;

	abstract public HttpResponse handlePOST(String path, String queryString, String body) throws IOException;

}
