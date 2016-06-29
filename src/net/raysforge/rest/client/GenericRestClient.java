package net.raysforge.rest.client;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import net.raysforge.commons.HttpsUtils;
import net.raysforge.commons.Json;

public class GenericRestClient {
	static final String WWW_AUTHENTICATE = "WWW-Authenticate";
	String baseURL;
	protected String user;
	protected String pass;
	Auth auth;
	Digest digest;
	protected HashMap<String, String> headerMap = new HashMap<String, String>();
	public boolean debugURL = false;

	public static enum Auth {
		Basic, Digest, Token
	}

	public GenericRestClient(String baseURL, String user, String pass, Auth auth) {

		this.baseURL = baseURL.endsWith("/") ? baseURL : baseURL + "/";
		this.user = user;
		this.pass = pass;
		this.auth = auth;
		HttpsUtils.trustEveryone();
	}

	public void setHeader(String key, String value) {
		headerMap.put(key, value);
	}

	@SuppressWarnings({ "unchecked" })
	public Map<String, Object> getDataAsMap(String path) throws IOException {
		Object data = getData(path);
		if (data == null) {
			return null;
		} else if (data instanceof Map<?, ?>) {
			return (Map<String, Object>) data;
		} else {
			throw new RuntimeException("data != instance of Map");
		}
	}

	public Object getData(String path) throws IOException {
		if (debugURL)
			System.out.println(baseURL + path);
		URL url = new URL(baseURL + path);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();

		if (auth == Auth.Basic) {
			String b64 = DatatypeConverter.printBase64Binary((user + ":" + pass).getBytes());
			con.setRequestProperty("Authorization", "Basic " + b64);
		}

		if (digest != null && auth == Auth.Digest) {
			String authorization = digest.calculateDigestAuthorization("GET", con.getURL().getPath());
			con.setRequestProperty("Authorization", authorization);
		}

		headerMap.forEach((k, v) -> con.setRequestProperty(k, v));

		int responseCode = con.getResponseCode();
		if (responseCode == 404) {
			System.out.println("404 for " + path);
			return null;
		} else if (responseCode == 401) {
			if (auth == Auth.Digest) {
				digest = new Digest(user, pass, con.getHeaderField(WWW_AUTHENTICATE));
				getData(path);
			} else {
				System.out.println("error 401");
			}

		} else if (responseCode < 200 || responseCode >= 300) {
			throw new RuntimeException("HTTP RESPONSE CODE:" + responseCode);
		}

		Reader r = new InputStreamReader(con.getInputStream());
		Json json = new Json();
		return json.parse(r);
	}

	public Object postData(String method, String path, String data) throws IOException {
		URL url = new URL(baseURL + path);
		//System.out.println(url);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod(method);
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestProperty("charset", "utf-8");
		con.setRequestProperty("Content-Length", Integer.toString(data.getBytes(Charset.forName("UTF-8")).length));
		con.setUseCaches(false);

		if (auth == Auth.Basic) {
			String b64 = DatatypeConverter.printBase64Binary((user + ":" + pass).getBytes("ISO-8859-1"));
			con.setRequestProperty("Authorization", "Basic " + b64);
		}

		if (digest != null && auth == Auth.Digest) {
			String authorization = digest.calculateDigestAuthorization("GET", con.getURL().getPath());
			con.setRequestProperty("Authorization", authorization);
		}

		headerMap.forEach((k, v) -> con.setRequestProperty(k, v));

		try (OutputStream os = con.getOutputStream();) {
			os.write(data.getBytes(Charset.forName("UTF-8")));
		}

		int responseCode = con.getResponseCode();
		if (responseCode == 401) {
			if (auth == Auth.Digest) {
				if (digest != null) {
					System.out.println("this should not happen.");
					System.out.println(con.getResponseMessage());
					System.exit(1);
				}
				digest = new Digest(user, pass, con.getHeaderField(WWW_AUTHENTICATE));
				return postData(method, path, data);
			} else {
				System.out.println("error 401");
			}
		}

		//System.out.println(responseCode);

		try (Reader r = new InputStreamReader(con.getInputStream())) {
			Json json = new Json();
			return json.parse(r);
		}
	}

}
