package edu.ucar.fanda.envsetupapi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import edu.ucar.fanda.util.ApiUtils;

public class UpdateAwardTransTypes {
	private static String AWARD_TRANS_TYPE_API_URL;
	private static String REST_API_USER;
	private static String REST_API_PASSWORD;
	
	public static void main(String[] args) {
		try {
			awardTransactionTypeInsert();		
			awardTransactionTypeUpdate();
			awardTransactionTypeDelete();
//	SJW - The awardTransactionTypeInsertFix() method sets award_transaction_type table back to default installation
// 			awardTransactionTypeInsertFix();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public UpdateAwardTransTypes (String serverName) {
		AWARD_TRANS_TYPE_API_URL = "https://" + serverName + ".fanda.ucar.edu/ra/award/api/v1/award-transaction-types/";
		REST_API_USER = "apiuser";
		REST_API_PASSWORD = "BlueOrchid05";
	}
	
	public void runUpdate() throws Exception {
		try {
			System.out.println("\n   ***** START with UpdateAwardTransTypes(); *****\n");
			awardTransactionTypeInsert();		
			awardTransactionTypeUpdate();
			awardTransactionTypeDelete();
			System.out.println("\n   ***** END with UpdateAwardTransTypes(); *****\n");
		} catch (Exception ex) {
			throw new Exception("UpdateAwardTransTypes() Exception: " + ex.toString());
		}
	}
	
	public static void awardTransactionTypeInsert() throws IOException {
		System.out.println("***************** awardTransactionTypeInsert *****************\n");
		InputStream inputStream = UpdateAwardTransTypes.class.getClassLoader().getResourceAsStream("award_trans_type_insert.json");
		String jsonString = IOUtils.toString(inputStream, Charset.defaultCharset());
		JSONArray jsonFileArray = new JSONArray(jsonString);
		inputStream.close();
		
		try {
			Client client = Client.create();
			// add basic authentication
			client.addFilter(new HTTPBasicAuthFilter(UpdateAwardTransTypes.REST_API_USER, UpdateAwardTransTypes.REST_API_PASSWORD));		
			WebResource webResource = client.resource(UpdateAwardTransTypes.AWARD_TRANS_TYPE_API_URL);
			ClientResponse getResponse = webResource.type("application/json").get(ClientResponse.class);
			
			if (getResponse.getStatus() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + getResponse.getStatus());
			} else {
				String output = getResponse.getEntity(String.class);
				int maxTypeCode = ApiUtils.getMaxCode(output, "_primaryKey");
				int newTypeCode = maxTypeCode + 1;
				
				for (int i = 0; i < jsonFileArray.length(); ++i) {
					jsonFileArray.getJSONObject(i).put("awardTransactionTypeCode", "" + newTypeCode + "");
					newTypeCode++;
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
	}
	
	public static void awardTransactionTypeUpdate() throws IOException {
		System.out.println("***************** awardTransactionTypeUpdate *****************\n");
		InputStream inputStream = UpdateAwardTransTypes.class.getClassLoader().getResourceAsStream("award_trans_type_update.json");
		String jsonString = IOUtils.toString(inputStream, Charset.defaultCharset());
		JSONArray jsonFileArray = new JSONArray(jsonString);
		inputStream.close();
		
		HashMap<String, String> newAwardTransTypeMap = new HashMap<String, String>();
		for (int i = 0; i < jsonFileArray.length(); ++i) {
			JSONObject jsonObj = jsonFileArray.getJSONObject(i);
			newAwardTransTypeMap.put(jsonObj.getString("oldName"), jsonObj.getString("newName"));			
	// RESET newAwardTransTypeMap.put(jsonObj.getString("newName"), jsonObj.getString("oldName"));
		}
		
		Client client = Client.create();
		// add basic authentication
		client.addFilter(new HTTPBasicAuthFilter(UpdateAwardTransTypes.REST_API_USER, UpdateAwardTransTypes.REST_API_PASSWORD));		
		WebResource webResource = client.resource(UpdateAwardTransTypes.AWARD_TRANS_TYPE_API_URL);
		
		try {
			ClientResponse response = webResource.type("application/json").get(ClientResponse.class);				
			if (response.getStatus() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
			} else {
				String output = response.getEntity(String.class);
				
				// transform response into a json array
				JSONArray existingJsonArray = new JSONArray(output);
				JSONArray newJsonArray = new JSONArray();
				
				for (int i = 0; i < existingJsonArray.length(); ++i) {	
					JSONObject awardTransType = existingJsonArray.getJSONObject(i);					
					Map<String, Object> awardTransTypeMap = awardTransType.toMap();
					String newTypeName = (String) newAwardTransTypeMap.get(awardTransTypeMap.get("description"));
					
					if (newTypeName != null) {
						awardTransType.put("description", newTypeName);
						newJsonArray.put(awardTransType);
					}
				}
				
				ClientResponse putResponse = webResource.type("application/json").put(ClientResponse.class, newJsonArray.toString());
				
				// Response code 204 for success
				if (putResponse.getStatus() != 204) {
					throw new RuntimeException("Failed : HTTP error code : " + putResponse.getStatus());
				}	
			}
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public static void awardTransactionTypeDelete() throws IOException {
		System.out.println("***************** awardTransactionTypeDelete *****************\n");
		InputStream inputStream = UpdateAwardTransTypes.class.getClassLoader().getResourceAsStream("award_trans_type_delete.json");
		String jsonString = IOUtils.toString(inputStream, Charset.defaultCharset());
		JSONArray jsonFileArray = new JSONArray(jsonString);
		inputStream.close();
		
		List<String> awardTransTypeDelList = new ArrayList<String>();
		
		for (int i = 0; i < jsonFileArray.length(); ++i) {
			JSONObject jsonObj = jsonFileArray.getJSONObject(i);
			awardTransTypeDelList.add(i, jsonObj.getString("description"));
		}
		
		Client client = Client.create();
		// add basic authentication
		client.addFilter(new HTTPBasicAuthFilter(UpdateAwardTransTypes.REST_API_USER, UpdateAwardTransTypes.REST_API_PASSWORD));		
		WebResource webResource = client.resource(UpdateAwardTransTypes.AWARD_TRANS_TYPE_API_URL);
		ClientResponse getResponse = webResource.type("application/json").get(ClientResponse.class);
		try {
		//	ClientResponse getResponse = webResource.type("application/json").get(ClientResponse.class);				
			if (getResponse.getStatus() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + getResponse.getStatus());
			} else {
				String output = getResponse.getEntity(String.class);				
				// transform response into a json array
				JSONArray transTypeJsonArray = new JSONArray(output);
				
				List<Integer> awardTransTypeDelCodes = new ArrayList<Integer>();
				
				for (int i = 0; i < transTypeJsonArray.length(); ++i) {	
					JSONObject awardTransType = transTypeJsonArray.getJSONObject(i);
					
					for(String description : awardTransTypeDelList) {
						if (awardTransType.getString("description").equals(description)) {							
							awardTransTypeDelCodes.add(awardTransType.getInt("awardTransactionTypeCode"));
							break;
						}
					}
				}
				
				for(int transTypeCode : awardTransTypeDelCodes) {					
					ClientResponse delResponse = webResource.path(Integer.toString(transTypeCode)).delete(ClientResponse.class);
					if (delResponse.getStatus() != 204) {
						throw new RuntimeException("Failed : HTTP error code : " + delResponse.getStatus());
					}
				}				
			}			
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public static void awardTransactionTypeInsertFix() throws IOException {
		System.out.println("***************** awardTransactionTypeInsertFix *****************\n");
		InputStream inputStream = UpdateAwardTransTypes.class.getClassLoader().getResourceAsStream("award_trans_type_insert_fix.json");
		String jsonString = IOUtils.toString(inputStream, Charset.defaultCharset());
		JSONArray jsonFileArray = new JSONArray(jsonString);
		inputStream.close();
		
		Client client = Client.create();
		// add basic authentication
		client.addFilter(new HTTPBasicAuthFilter(UpdateAwardTransTypes.REST_API_USER, UpdateAwardTransTypes.REST_API_PASSWORD));		
		WebResource webResource = client.resource(UpdateAwardTransTypes.AWARD_TRANS_TYPE_API_URL);
		
		try {
			ClientResponse delResponse = webResource.queryParam("_allowMulti", "true").delete(ClientResponse.class);
			
			if (delResponse.getStatus() != 204) {
				System.out.println("ERROR response code: " +delResponse.getStatus());
			}
			
			ClientResponse postResponse = webResource.type("application/json").post(ClientResponse.class, jsonFileArray.toString());
		
			// Response code 201 for success
			if (postResponse.getStatus() != 201) {
				System.out.println("ERROR reponse code: " + postResponse.getStatus());
			}		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
