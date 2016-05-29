# GenericRestClient
GenericRestClient written in Java using basic HttpURLConnection, supports Basic and Digest (qop=auth) authentication.

Usage example:

```Java
import java.io.IOException;
import java.util.*;

import net.raysforge.commons.HttpsUtils;
import net.raysforge.commons.Json;
import net.raysforge.rest.client.GenericRestClient;

// FilrUser is a very simple marshalling POJO.

public class FilrRestClient extends GenericRestClient {

	public FilrRestClient(String baseURL, String user, String pass) {
		super(baseURL, user, pass, Auth.Basic);
		HttpsUtils.trustEveryone();
	}

	@SuppressWarnings("unchecked")
	List<FilrUser> getUsers() throws IOException {
		Map<String, Object> users = getDataAsMap("users/");
		List<FilrUser> fuser = new ArrayList<FilrUser>();
		List<Object> items = (List<Object>) users.get("items");
		for (Object item : items) {
			fuser.add(new FilrUser((Map<String, Object>) item));
		}
		return fuser;
	}

	FilrUser getUser(String username) throws IOException {
		Map<String, Object> user = getDataAsMap("users/name/" + username);
		return user != null?new FilrUser(user):null;
	}
  
	public void createUserIfAbsent(String username, String email, String givenName, String surname, String passwd) throws IOException {
		if (getUser(username) == null) {
  		HashMap<String, Object> map = new HashMap<String, Object>();
  		map.put("name", username);
  		map.put("email", email);
  		map.put("first_name", givenName);
  		map.put("last_name", surname);
  		map.put("entity_type", "user");
  		map.put("doc_type", "entry");
			postData("users/?password="+passwd, Json.toJsonString(map));
		}
	}
}

// only works for admin in this way
public void changePassword(String username, String new_password) throws IOException {
	int userID = getUser(username).id.intValue();
	postData("users/"+userID+"/password", "new_password="+ URLEncoder.encode(new_password, "UTF-8"), "application/x-www-form-urlencoded"); // this time it needs to be URL encoded
}
```
