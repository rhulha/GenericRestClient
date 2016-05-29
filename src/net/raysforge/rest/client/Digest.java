package net.raysforge.rest.client;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import net.raysforge.commons.MD5Utils;

public class Digest {
	public final String user;
	public final String pass;
	public final String nonce;
	public final String realm;
	public final String opaque;
	public final String qop;
	
	protected int nonceCount = 1;
	
	// test with: http://httpbin.org/digest-auth/auth/user/passwd
	// compare: https://en.wikipedia.org/wiki/Digest_access_authentication
	// also: http://svn.apache.org/repos/asf/httpcomponents/oac.hc3x/trunk/src/java/org/apache/commons/httpclient/auth/DigestScheme.java

	public Digest(String user, String pass, String nonce, String realm, String opaque, String qop) {
		this.user = user;
		this.pass = pass;
		this.nonce = nonce;
		this.realm = realm;
		this.opaque = opaque;
		this.qop = qop;
	}

	public Digest(String user, String pass, String wwwAuth) {
		Map<String, String> tupels = getTupels(wwwAuth);
		this.user = user;
		this.pass = pass;
		this.nonce = tupels.get("nonce");
		this.realm = tupels.get("Digest realm");
		this.opaque = tupels.get("opaque");
		this.qop = tupels.get("qop");
	}
	
	
	private Map<String, String> getTupels(String wwwAuth) {
		Map<String, String> tupels = new HashMap<>();
		String[] split = wwwAuth.split(", ");
		for (String part : split) {
			String[] pair = part.split("=");
			tupels.put(pair[0], pair[1].substring(1, pair[1].length()-1));
		}
		return tupels;
	}

	public String calculateDigestAuthorization(String method, String digestURI) {
		String cnonce = Long.toHexString(Double.doubleToLongBits(Math.random()));
		return calculateDigestAuthorization(method, digestURI, cnonce);
	}
	
	String getISOHash(String s) {
		return MD5Utils.getMD5Hash(s.getBytes(Charset.forName("ISO-8859-1")));
	}

	public String calculateDigestAuthorization(String method, String digestURI, String cnonce) {
		
		String HA1= getISOHash(user+":"+realm+":"+pass).toLowerCase();
		String HA2= getISOHash(method+":"+digestURI).toLowerCase();
		String nonceCountStr = String.format("%08d", nonceCount++);
		String response=getISOHash(HA1+":"+nonce+":"+nonceCountStr+":"+cnonce+":"+qop+":"+HA2).toLowerCase();
		
		return "Digest username=\""+ user + "\", realm=\""+realm+"\", nonce=\""+nonce+"\", uri=\""+digestURI+"\", qop=auth, nc="+nonceCountStr+", cnonce=\""+cnonce+"\", response=\""+response+"\", opaque=\""+opaque+"\"";
	}
	public String calculateDigestAuthorizationNoSpaces(String method, String digestURI, String cnonce) {
		
		String HA1= getISOHash(user+":"+realm+":"+pass).toLowerCase();
		String HA2= getISOHash(method+":"+digestURI).toLowerCase();
		String nonceCountStr = String.format("%08d", nonceCount++);
		String response=getISOHash(HA1+":"+nonce+":"+nonceCountStr+":"+cnonce+":"+qop+":"+HA2).toLowerCase();
		
		return "Digest username=\""+ user + "\",realm=\""+realm+"\",nonce=\""+nonce+"\",uri=\""+digestURI+"\",qop=auth,nc="+nonceCountStr+",cnonce=\""+cnonce+"\",response=\""+response+"\",opaque=\""+opaque+"\"";
	}
}
