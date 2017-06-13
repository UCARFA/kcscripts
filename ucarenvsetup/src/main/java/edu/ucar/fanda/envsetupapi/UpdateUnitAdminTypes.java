package edu.ucar.fanda.envsetupapi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;


import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

import edu.ucar.fanda.util.ApiUtils;
import edu.ucar.fanda.util.DbUtils;

public class UpdateUnitAdminTypes {

	private static String UNIT_ADMIN_TYPE_API_URL;
	private static String UNIT_ADMINISTRATOR_API_URL;
	private static String REST_API_USER;
	private static String REST_API_PASSWORD;
	
	public UpdateUnitAdminTypes (String serverName) {
		UNIT_ADMIN_TYPE_API_URL = "https://" + serverName + ".fanda.ucar.edu/ra/research-common/api/v1/unit-administrator-types/";
		UNIT_ADMINISTRATOR_API_URL = "https://" + serverName + ".fanda.ucar.edu/ra/research-common/api/v1/unit-administrators/";
		REST_API_USER = "apiuser";
		REST_API_PASSWORD = "BlueOrchid05";
	}
	
	public static void main(String[] args) {
		try {				
			unitAdminTypeDelete();	
			unitAdminTypeInsert();
			assignProjectAccountants();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void runUpdate() throws Exception {
		try {
			System.out.println("\n   ***** START with UpdateUnitAdminTypes(); *****\n");
			unitAdminTypeDelete();
			unitAdminTypeInsert();
			assignProjectAccountants();
			System.out.println("\n   ***** END with UpdateUnitAdminTypes(); *****\n");
		} catch (Exception ex) {
			throw new Exception("UpdateUnitAdminTypes() Exception: " + ex.toString());
		}
	}
	
	public static String getKualiUserId(String userName) throws ClassNotFoundException {
		System.out.println("   ********************** getKualiUsers() ***********************\n");
		String userId = null;
		try {
			userId = DbUtils.getUserId(userName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("   ********************* END getKualiUsers() ********************\n");
		return userId;
	}
	
	public static void assignProjectAccountants() throws IOException {
		System.out.println("***************** assignProjectAccountants() *****************\n");
		InputStream inputStream = UpdateUnitAdminTypes.class.getClassLoader().getResourceAsStream("unit_admin_projacct_assignment.json");
		String jsonString = IOUtils.toString(inputStream, Charset.defaultCharset());
		JSONArray jsonFileArray = new JSONArray(jsonString);
		inputStream.close();
		
		try {
			JSONArray jsonUpdateArray = new JSONArray();
			for (int i = 0; i < jsonFileArray.length(); ++i) {
				JSONObject jsonObj = jsonFileArray.getJSONObject(i);
				String userId = getKualiUserId(jsonObj.getString("userName"));
				String unitNumber = jsonObj.getString("unitNumber");
				if (userId == null) {
					System.out.println("  ** User " + jsonObj.getString("userName") + " not found, or multiple records were returned for that user. **");
				} else {
					System.out.println("User ID for user " + jsonObj.getString("userName") + " is " + userId);
			
					String existingUnitAdminTypes = getExistingUnitAdminTypes();
					
					String adminTypeCode = null;
					boolean found = false;
					if (!existingUnitAdminTypes.equals(null)) {
						JSONArray unitAdminTypeJsonArray = new JSONArray(existingUnitAdminTypes);
						
						for (int ii = 0; ii < unitAdminTypeJsonArray.length(); ++ii) {
							JSONObject unitAdminType = unitAdminTypeJsonArray.getJSONObject(ii);
							if (unitAdminType.getString("description").equals(jsonObj.getString("adminAcctTypeName")) && unitAdminType.getString("defaultGroupFlag").equals(jsonObj.getString("adminAcctType"))) {
								System.out.println("Type code for " + jsonObj.getString("adminAcctTypeName") + " is " + unitAdminType.getString("code"));
								adminTypeCode = unitAdminType.getString("code");
								found = true;
								break;
							}
						}
						
					}
					if (!found) {
						System.out.println("   *** Admin Type Code for " + jsonObj.getString("adminAcctTypeName") + " not found. ***");
					} else {
						System.out.println("User id: " + userId + ", Admin Type Code: " + adminTypeCode + ", Unit Number: " + unitNumber);		
						
						JSONObject updateObj = new JSONObject();
						updateObj.put("personId", "" + userId + "");
						updateObj.put("unitAdministratorTypeCode", "" + adminTypeCode + "");
						updateObj.put("unitNumber", "" + unitNumber + "");
						System.out.println("Update Object: " + updateObj);
						
						jsonUpdateArray.put(updateObj);
					}
				}
			}
			
			System.out.println("JSON Update Array: " + jsonUpdateArray.toString());
			Client client = Client.create();
			// add basic authentication
			client.addFilter(new HTTPBasicAuthFilter(REST_API_USER, REST_API_PASSWORD));		
			WebResource webResource = client.resource(UNIT_ADMINISTRATOR_API_URL);
			ClientResponse response = webResource.type("application/json").post(ClientResponse.class, jsonUpdateArray.toString());
			// Response code 201 for success
			if (response.getStatus() != 201) {
				System.out.println("ERROR reponse code: " + response.getStatus());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		System.out.println("***************** END assignProjectAccountants() *****************\n");
	}
	
	public static String getExistingUnitAdminTypes() throws IOException {
		System.out.println("***************** getExistingUnitAdminTypes() *****************\n");
		String output = null;
		try {
			Client client = Client.create();
			// add basic authentication
			client.addFilter(new HTTPBasicAuthFilter(REST_API_USER, REST_API_PASSWORD));		
			WebResource webResource = client.resource(UNIT_ADMIN_TYPE_API_URL);
			ClientResponse getResponse = webResource.type("application/json").get(ClientResponse.class);
			if (getResponse.getStatus() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + getResponse.getStatus());
			} else {
				output = getResponse.getEntity(String.class);
			}			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return output;
	}
	
	public static void unitAdminTypeInsert() throws IOException {
		System.out.println("***************** unitAdminTypeInsert() *****************\n");
		InputStream inputStream = UpdateUnitAdminTypes.class.getClassLoader().getResourceAsStream("unit_admin_type_insert.json");
		String jsonString = IOUtils.toString(inputStream, Charset.defaultCharset());
		JSONArray jsonFileArray = new JSONArray(jsonString);
		inputStream.close();
		try {
			Client client = Client.create();
			// add basic authentication
			client.addFilter(new HTTPBasicAuthFilter(REST_API_USER, REST_API_PASSWORD));		
			WebResource webResource = client.resource(UNIT_ADMIN_TYPE_API_URL);
			String existingUnitAdminTypes = getExistingUnitAdminTypes();
			if (!existingUnitAdminTypes.equals(null)) {
				int maxCode = ApiUtils.getMaxCode(existingUnitAdminTypes, "code");
				int newCode = maxCode + 1;
				for (int i = 0; i < jsonFileArray.length(); ++i) {
					jsonFileArray.getJSONObject(i).put("code", "" + newCode + "");
					newCode++;
				}
				ClientResponse response = webResource.type("application/json").post(ClientResponse.class, jsonFileArray.toString());
				// Response code 201 for success
				if (response.getStatus() != 201) {
					System.out.println("ERROR reponse code: " + response.getStatus());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("***************** END unitAdminTypeInsert() *************\n");
	}
	
	public static void unitAdminTypeDelete() throws IOException {
		System.out.println("***************** unitAdminTypeDelete() *****************\n");
		String existingUnitAdminTypes = getExistingUnitAdminTypes();
		if (!existingUnitAdminTypes.equals(null)) {
			try {		
				Client client = Client.create();
				// add basic authentication
				client.addFilter(new HTTPBasicAuthFilter(REST_API_USER, REST_API_PASSWORD));		
				WebResource webResource = client.resource(UNIT_ADMIN_TYPE_API_URL);
				JSONArray unitAdminTypeJsonArray = new JSONArray(existingUnitAdminTypes);
				for (int i = 0; i < unitAdminTypeJsonArray.length(); ++i) {
					JSONObject unitAdminType = unitAdminTypeJsonArray.getJSONObject(i);
					if (unitAdminType.getString("defaultGroupFlag").equals("C") || unitAdminType.getString("defaultGroupFlag").equals("U")) {
						ClientResponse delResponse = webResource.path(unitAdminType.getString("_primaryKey")).delete(ClientResponse.class);
						if (delResponse.getStatus() != 204) {
							throw new RuntimeException("Failed : HTTP error code : " + delResponse.getStatus());
						}
					}
				}					
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("***************** END unitAdminTypeDelete() *************\n");
	}
}