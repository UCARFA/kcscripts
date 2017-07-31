package edu.ucar.fanda.envsetupapi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.opencsv.CSVReader;
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
	private static String server;
	
	public UpdateUnitAdminTypes (String serverName) {
		server = serverName;
		UNIT_ADMIN_TYPE_API_URL = "https://" + serverName + ".fanda.ucar.edu/ra/research-common/api/v1/unit-administrator-types/";
		UNIT_ADMINISTRATOR_API_URL = "https://" + serverName + ".fanda.ucar.edu/ra/research-common/api/v1/unit-administrators/";
		REST_API_USER = "apiuser";
		REST_API_PASSWORD = "BlueOrchid05";
	}
	
	public static void main(String[] args) {
		try {
			System.out.println("\n   ***** Running UpdateUnitAdminTypes(); *****\n");
			server = "localhost";
			UNIT_ADMIN_TYPE_API_URL = "http://localhost:8080/kc-dev/research-common/api/v1/unit-administrator-types/";
			UNIT_ADMINISTRATOR_API_URL = "http://localhost:8080/kc-dev/research-common/api/v1/unit-administrators/";
			REST_API_USER = "admin";
			REST_API_PASSWORD = "restapipassword";
//			unitAdminTypeDelete();	
			unitAdminTypeInsert();
			deleteProjectAccountants();
			assignProjectAccountants();
			System.out.println("\n   ***** End UpdateUnitAdminTypes(); *****\n");
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
	//	System.out.println("   ********************** getKualiUsers() ***********************\n");
		String userId = null;
		try {
			userId = DbUtils.getUserId(userName, server);
		} catch (Exception e) {
			e.printStackTrace();
		}
	//	System.out.println("   ********************* END getKualiUsers() ********************\n");
		return userId;
	}
	
	public static void deleteProjectAccountants() throws IOException {
		System.out.println("***************** deleteProjectAccountants() *****************");
		
		InputStream inputStream = UpdateUnitAdminTypes.class.getClassLoader().getResourceAsStream("unit_admin_projacct_delete.csv");
		CSVReader reader = null;		
		JSONArray jsonFileArray = new JSONArray();
		try {
			reader = new CSVReader(new InputStreamReader(inputStream));
			String[] line;
            while ((line = reader.readNext()) != null) {
            	JSONObject jsonObject = new JSONObject();
            	jsonObject.put("userName", "" + line[0] + "");
            	jsonObject.put("adminAcctTypeName", "" + line[1] + "");
            	jsonObject.put("unitNumber", "" + line[2] + "");  			
    			jsonFileArray.put(jsonObject);
            }
		} catch (IOException e) {
			e.printStackTrace();
		}		
		inputStream.close();
		
		Client client = Client.create();
		// add basic authentication
		client.addFilter(new HTTPBasicAuthFilter(REST_API_USER, REST_API_PASSWORD));
		
		try {
			String existingUnitAdminTypes = getExistingUnitAdminTypes();
			JSONArray unitAdminTypeJsonArray = new JSONArray(existingUnitAdminTypes);
			for (int i = 0; i < jsonFileArray.length(); ++i) {
				JSONObject jsonObj = jsonFileArray.getJSONObject(i);
				String userId = getKualiUserId(jsonObj.getString("userName"));
				String unitNumber = jsonObj.getString("unitNumber");
				if (userId == null) {
					System.out.println("  ** User " + jsonObj.getString("userName") + " not found, or multiple records were returned for that user. **");
				} else {
					String adminTypeCode = null;
					boolean found = false;
					if (!existingUnitAdminTypes.equals(null)) {			
						for (int ii = 0; ii < unitAdminTypeJsonArray.length(); ++ii) {
							JSONObject unitAdminType = unitAdminTypeJsonArray.getJSONObject(ii);
							if (unitAdminType.getString("description").equals(jsonObj.getString("adminAcctTypeName"))) {
				//				System.out.println("Type code for " + jsonObj.getString("adminAcctTypeName") + " is " + unitAdminType.getString("code"));
								adminTypeCode = unitAdminType.getString("code");
								found = true;
								break;
							}
						}
					}
					if (!found) {
						System.out.println("   *** Admin Type Code for " + jsonObj.getString("adminAcctTypeName") + " not found. ***");
					} else {
					//	System.out.println("User id: " + userId + ", Admin Type Code: " + adminTypeCode + ", Unit Number: " + unitNumber);		
						String unitAdminKey = userId + ":" + adminTypeCode + ":" + unitNumber;
						WebResource webResource = client.resource(UNIT_ADMINISTRATOR_API_URL + unitAdminKey);
						ClientResponse response = webResource.type("application/json").get(ClientResponse.class);
						
						if (response.getStatus() == 200) {
							ClientResponse delResponse = webResource.type("application/json").delete(ClientResponse.class);
							if (delResponse.getStatus() != 204) {
								System.out.println("ERROR - there was a problem deleting the record for key: " + unitAdminKey);
							} else {
								System.out.println("   Successful deletion of unit administrator: " + (jsonObj.getString("userName")) + ", " + (jsonObj.getString("adminAcctTypeName")) + ", " + unitNumber);  
							}
						} else {
							System.out.println("   Unit administrator to delete does NOT exist in Kuali: " + (jsonObj.getString("userName")) + ", " + (jsonObj.getString("adminAcctTypeName")) + ", " + unitNumber);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			client.destroy();
		}	
		System.out.println("*************** END deleteProjectAccountants() ***************\n");
	}
	
	public static void assignProjectAccountants() throws IOException {
		System.out.println("***************** assignProjectAccountants() *****************");
	
		InputStream inputStream = UpdateUnitAdminTypes.class.getClassLoader().getResourceAsStream("unit_admin_projacct_assignment.csv");
		CSVReader reader = null;		
		JSONArray jsonFileArray = new JSONArray();
		try {
			reader = new CSVReader(new InputStreamReader(inputStream));
			String[] line;
            while ((line = reader.readNext()) != null) {
            	JSONObject jsonObject = new JSONObject();
            	jsonObject.put("userName", "" + line[1] + "");
            	jsonObject.put("adminAcctType", "" + line[0] + "");
            	jsonObject.put("adminAcctTypeName", "" + line[2] + "");
            	jsonObject.put("unitNumber", "" + line[3] + "");  			
    			jsonFileArray.put(jsonObject);
            }
		} catch (IOException e) {
			e.printStackTrace();
		}		
		inputStream.close();
		
		Client client = Client.create();
		// add basic authentication
		client.addFilter(new HTTPBasicAuthFilter(REST_API_USER, REST_API_PASSWORD));
		try {
			JSONArray jsonUpdateArray = new JSONArray();
			String existingUnitAdminTypes = getExistingUnitAdminTypes();
			JSONArray unitAdminTypeJsonArray = new JSONArray(existingUnitAdminTypes);
			for (int i = 0; i < jsonFileArray.length(); ++i) {
				JSONObject jsonObj = jsonFileArray.getJSONObject(i);
				String userId = getKualiUserId(jsonObj.getString("userName"));
				String unitNumber = jsonObj.getString("unitNumber");
				boolean unitFound = DbUtils.checkUnit(unitNumber, server);
				if (userId == null) {
					System.out.println("  ** User " + jsonObj.getString("userName") + " not found, or multiple records were returned for that user. **");
				} else if (!unitFound) {
					System.out.println("  ** Unit not found: " + unitNumber + " **");
				} else {
				//	System.out.println("User ID for user " + jsonObj.getString("userName") + " is " + userId);					
					String adminTypeCode = null;
					boolean found = false;
					if (!existingUnitAdminTypes.equals(null)) {			
						for (int ii = 0; ii < unitAdminTypeJsonArray.length(); ++ii) {
							JSONObject unitAdminType = unitAdminTypeJsonArray.getJSONObject(ii);
							if (unitAdminType.getString("description").equals(jsonObj.getString("adminAcctTypeName")) && unitAdminType.getString("defaultGroupFlag").equals(jsonObj.getString("adminAcctType"))) {
						//		System.out.println("Type code for " + jsonObj.getString("adminAcctTypeName") + " is " + unitAdminType.getString("code"));
								adminTypeCode = unitAdminType.getString("code");
								found = true;
								break;
							}
						}
					}
					if (!found) {
						System.out.println("   *** Admin Type Code NOT FOUND: " + (jsonObj.getString("userName")) + ", " + (jsonObj.getString("adminAcctTypeName")));
					} else {
						System.out.println("   User id: " + userId + ", Admin Type Code: " + adminTypeCode + ", Unit Number: " + unitNumber);		
						String unitAdminKey = userId + ":" + adminTypeCode + ":" + unitNumber;
						WebResource webResource = client.resource(UNIT_ADMINISTRATOR_API_URL + unitAdminKey);
						ClientResponse response = webResource.type("application/json").get(ClientResponse.class);
						
						if (response.getStatus() == 200) {
							System.out.println("   Unit administrator already exists in Kuali: " + (jsonObj.getString("userName")) + ", " + (jsonObj.getString("adminAcctTypeName")) + ", " + unitNumber + ", " + unitAdminKey);
						} else {
							JSONObject updateObj = new JSONObject();
							updateObj.put("personId", "" + userId + "");
							updateObj.put("unitAdministratorTypeCode", "" + adminTypeCode + "");
							updateObj.put("unitNumber", "" + unitNumber + "");
				//			System.out.println("Update Object: " + updateObj);
							jsonUpdateArray.put(updateObj);
						}
					}
				}
			}
			
			if (jsonUpdateArray.length() > 0) {
				System.out.println("JSON Update Array: " + jsonUpdateArray.toString());	
				
				WebResource webResource = client.resource(UNIT_ADMINISTRATOR_API_URL);
				ClientResponse response = webResource.type("application/json").post(ClientResponse.class, jsonUpdateArray.toString());
				// Response code 201 for success
				if (response.getStatus() != 201) {
					System.out.println("ERROR reponse code: " + response.getStatus());
				} else {
					System.out.println("   * Successful insert of unit administrators *");
				}
				
			} else {
				System.out.println("   * No unit administrators need to be inserted *");
			//	System.out.println("     ***** JSON Update Array empty *****");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			client.destroy();
		}
		
		System.out.println("***************** END assignProjectAccountants() *****************\n");
	}
	
	public static String getExistingUnitAdminTypes() throws IOException {
	//	System.out.println("***************** getExistingUnitAdminTypes() *****************\n");
		String output = null;
		Client client = Client.create();
		try {
			
			// add basic authentication
			client.addFilter(new HTTPBasicAuthFilter(REST_API_USER, REST_API_PASSWORD));		
			WebResource webResource = client.resource(UNIT_ADMIN_TYPE_API_URL);
			ClientResponse getResponse = webResource.type("application/json").get(ClientResponse.class);
			
			if (getResponse.getStatus() == 200) {
				output = getResponse.getEntity(String.class);
			} else if (getResponse.getStatus() == 404) {
				output = "notfound";
			} else {
				throw new RuntimeException("Failed : HTTP error code : " + getResponse.getStatus());
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			client.destroy();
		}
		return output;
	}
	
	public static void unitAdminTypeInsert() throws IOException {
		System.out.println("***************** unitAdminTypeInsert() *****************");
		InputStream inputStream = UpdateUnitAdminTypes.class.getClassLoader().getResourceAsStream("unit_admin_type_insert.json");
		String jsonString = IOUtils.toString(inputStream, Charset.defaultCharset());
		JSONArray jsonFileArray = new JSONArray(jsonString);
		inputStream.close();
		JSONArray unitAdminTypeUpdateArray = new JSONArray();		
		int newCode = 0;
		try {				
			String existingUnitAdminTypes = getExistingUnitAdminTypes();
			if (!existingUnitAdminTypes.equals(null) && !existingUnitAdminTypes.equals("notfound")) {				
				int maxCode = ApiUtils.getMaxCode(existingUnitAdminTypes, "code");
				newCode = maxCode + 1;
				
				for (int i = 0; i < jsonFileArray.length(); ++i) {
					JSONObject jsonObj = jsonFileArray.getJSONObject(i);					
					boolean found = false;
					JSONArray existingUnitAdminTypeJsonArray = new JSONArray(existingUnitAdminTypes);					
					for (int ii = 0; ii < existingUnitAdminTypeJsonArray.length(); ++ii) {
						JSONObject existingUnitAdminType = existingUnitAdminTypeJsonArray.getJSONObject(ii);
						if (existingUnitAdminType.getString("description").equals(jsonObj.getString("description")) && (existingUnitAdminType.getBoolean("multiplesFlag") == jsonObj.getBoolean("multiplesFlag")) && existingUnitAdminType.getString("defaultGroupFlag").equals(jsonObj.getString("defaultGroupFlag"))) {
							found = true;
							break;
						}				
					}
					
					if (!found) {
						JSONObject updateObj = new JSONObject();
						updateObj.put("description", "" + jsonObj.getString("description") + "");
						updateObj.put("multiplesFlag", jsonObj.getBoolean("multiplesFlag"));
						updateObj.put("defaultGroupFlag", "" + jsonObj.getString("defaultGroupFlag") + "");
						updateObj.put("code", "" + newCode + "");
						newCode++;
					//	System.out.println("Update Object: " + updateObj);							
						unitAdminTypeUpdateArray.put(updateObj);
					} else {
						System.out.println("   Unit Administrator Type already exists: " + jsonObj.getString("description"));
					}					
				}	
			} else {		
				newCode = 1;
				for (int i = 0; i < jsonFileArray.length(); ++i) {
					JSONObject jsonObj = jsonFileArray.getJSONObject(i);
					JSONObject updateObj = new JSONObject();
					updateObj.put("description", "" + jsonObj.getString("description") + "");
					updateObj.put("multiplesFlag", jsonObj.getBoolean("multiplesFlag"));
					updateObj.put("defaultGroupFlag", "" + jsonObj.getString("defaultGroupFlag") + "");
					updateObj.put("code", "" + newCode + "");
					newCode++;
				//	System.out.println("Update Object: " + updateObj);							
					unitAdminTypeUpdateArray.put(updateObj);
				}
			}
			if (unitAdminTypeUpdateArray.length() > 0) {
		//		System.out.println("Post to API...");
				System.out.println("JSON Update Array: " + unitAdminTypeUpdateArray.toString());	
				Client client = Client.create();
				// add basic authentication
				client.addFilter(new HTTPBasicAuthFilter(REST_API_USER, REST_API_PASSWORD));		
				WebResource webResource = client.resource(UNIT_ADMIN_TYPE_API_URL);
				ClientResponse response = webResource.type("application/json").post(ClientResponse.class, unitAdminTypeUpdateArray.toString());
				// Response code 201 for success
				if (response.getStatus() != 201) {
					System.out.println("ERROR response code: " + response.getStatus());
				} else {
					System.out.println("   * Successful insert of unit administrator types *");
				}
			} else {
				System.out.println("   * No unit administrator types need to be inserted *");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("***************** END unitAdminTypeInsert() *************\n");
	}
	
	public static void unitAdminTypeDelete() throws IOException {
		System.out.println("***************** unitAdminTypeDelete() *****************");
		String existingUnitAdminTypes = getExistingUnitAdminTypes();
		if (!existingUnitAdminTypes.equals(null) && !existingUnitAdminTypes.equals("notfound")) {
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