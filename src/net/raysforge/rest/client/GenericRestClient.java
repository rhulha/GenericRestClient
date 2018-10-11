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
import net.raysforge.commons.StreamUtils;

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
		None, Basic, Digest, Token
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

	public void removeHeader(String key) {
		headerMap.remove(key);
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

	private void setRequestProperties(HttpURLConnection con) {
		if (auth == Auth.Basic) {
			String b64 = DatatypeConverter.printBase64Binary((user + ":" + pass).getBytes());
			con.setRequestProperty("Authorization", "Basic " + b64);
		}

		if (digest != null && auth == Auth.Digest) {
			String authorization = digest.calculateDigestAuthorization("GET", con.getURL().getPath());
			con.setRequestProperty("Authorization", authorization);
		}
		headerMap.forEach((k, v) -> con.setRequestProperty(k, v));
	}

	public InputStream getBodyInputStream(String path) throws IOException {
		if (debugURL)
			System.out.println(baseURL + path);
		URL url = new URL(baseURL + path);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		setRequestProperties(con);
		int responseCode = con.getResponseCode();
		if (responseCode == 404) {
			System.out.println("error 404");
			return null;
		} else if (responseCode == 401) {
			if (auth == Auth.Digest) {
				digest = new Digest(user, pass, con.getHeaderField(WWW_AUTHENTICATE));
				return getBodyInputStream(path);
			} else {
				System.out.println("error 401");
			}

		} else if (responseCode < 200 || responseCode >= 300) {
			InputStream is = responseCode < HttpURLConnection.HTTP_BAD_REQUEST ? con.getInputStream() : con.getErrorStream();
			String response = StreamUtils.readCompleteInputStream(is, "ISO-8859-1");
			throw new RuntimeException("HTTP RESPONSE CODE:" + responseCode + ", \n" + response);
		}
		return con.getInputStream();
	}

	public String getUTF8Body(String path) throws IOException {
		InputStream bodyInputStream = getBodyInputStream(path);
		if(bodyInputStream==null)
			return null;
		return StreamUtils.readCompleteInputStream(bodyInputStream, "UTF-8");
	}

	public Object getData(String path) throws IOException {
		InputStream is = getBodyInputStream(path);
		if(is==null)
			return null;
		Reader r = new InputStreamReader(is, "UTF-8");
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

		if (auth == Auth.Token) {
			con.setRequestProperty("Authorization", "Bearer " + pass);
		}

		if (digest != null && auth == Auth.Digest) {
			String authorization = digest.calculateDigestAuthorization("GET", con.getURL().getPath());
			con.setRequestProperty("Authorization", authorization);
		}

		headerMap.forEach((k, v) -> con.setRequestProperty(k, v));

		try (OutputStream os = con.getOutputStream();) {
			os.write(data.getBytes(Charset.forName("UTF-8")));
		}

		int responseCode=0;
		try {
			responseCode = con.getResponseCode();
		} catch (IOException ioe) {
			System.out.println("Server returned HTTP response code: 400 ?");
			responseCode=400;
		}
		
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

		InputStream is = responseCode < HttpURLConnection.HTTP_BAD_REQUEST ? con.getInputStream() : con.getErrorStream(); 
		
		try (Reader r = new InputStreamReader(is)) {
			Json json = new Json();
			return json.parse(r);
		}
	}

}
