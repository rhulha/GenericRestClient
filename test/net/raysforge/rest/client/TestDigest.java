package net.raysforge.rest.client;

public class TestDigest {

	public static void main1(String[] args) {
		String compareTo = "Authorization: Digest username=\"Admin\",realm=\"public\",nonce=\"fd0c177a88cb7c2b297a9e966bf40c9a\",opaque=\"be76d632563452ff\",qop=auth,uri=\"/v1/documents\",cnonce=\"f203c3e1\",nc=00000001,response=\"50e3dd7bc478230557cca9d6dc2732e2\"";
		String nonce = "fd0c177a88cb7c2b297a9e966bf40c9a";
		String opaque = "be76d632563452ff";
		String uri = "/v1/documents";
		String cnonce = "f203c3e1";
		
		Digest digest = new Digest("Admin", "Admin", nonce, "public", opaque, "auth");
		
		String c = digest.calculateDigestAuthorization("PUT", uri, cnonce);
		System.out.println("Authorization: " + c);
		System.out.println(compareTo);
	}
	
	public static void main2(String[] args) {
		String server_WWW_Authenticate = "Digest realm=\"public\", qop=\"auth\", nonce=\"6054df58f3151a61cd1ae7fbdc2729e2\", opaque=\"bc09ab8d08a35130\"";
		String httpclient = "Authorization: Digest username=\"Admin\",realm=\"public\",nonce=\"6054df58f3151a61cd1ae7fbdc2729e2\",opaque=\"bc09ab8d08a35130\",qop=auth,uri=\"/v1/documents\",cnonce=\"1d564805\",nc=00000001,response=\"2d1a5c72d847e480d48855cf1fb366e1\"";
		//String nonce = "fd0c177a88cb7c2b297a9e966bf40c9a";
		//String opaque = "be76d632563452ff";
		String uri = "/v1/documents";
		String cnonce = "1d564805";
		
		Digest digest = new Digest("Admin", "Admin", server_WWW_Authenticate);
		String c = digest.calculateDigestAuthorizationNoSpaces("PUT", uri, cnonce);
		System.out.println("Authorization: " + c);
		System.out.println(httpclient);
	}

}
