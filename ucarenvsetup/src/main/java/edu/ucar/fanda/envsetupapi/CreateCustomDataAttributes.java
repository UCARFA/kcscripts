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

public class CreateCustomDataAttributes {
	private static String CUSTOM_ATTRIBUTES_API_URL;
	private static String CUSTOM_ATTRIBUTE_DOCS_API_URL;
	private static String REST_API_USER;
	private static String REST_API_PASSWORD;
	
	public CreateCustomDataAttributes (String serverName) {
		CUSTOM_ATTRIBUTES_API_URL = "https://" + serverName + ".fanda.ucar.edu/ra/research-common/api/v1/custom-attributes/";
		CUSTOM_ATTRIBUTE_DOCS_API_URL = "https://" + serverName + ".fanda.ucar.edu/ra/research-common/api/v1/custom-attribute-documents/";
		REST_API_USER = "apiuser";
		REST_API_PASSWORD = "BlueOrchid05";
	}
	
	public static void main(String[] args) {
		try {		
			customDataAttributeInsert();
			customAttributeDocumentInsert();
			System.out.println("Done!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void runUpdate() throws Exception {
		try {
			System.out.println("\n   ***** START with CreateCustomDataAttributes(); *****\n");
			customDataAttributeInsert();
			customAttributeDocumentInsert();
			System.out.println("\n   ***** END with CreateCustomDataAttributes(); *****\n");
		} catch (Exception ex) {
			throw new Exception("CreateCustomDataAttributes() Exception: " + ex.toString());
		}
	}
	
	public static void customDataAttributeInsert() throws IOException {
		System.out.println("***************** customDataAttributeInsert *****************\n");
		InputStream inputStream = UpdateAwardTransTypes.class.getClassLoader().getResourceAsStream("custom_data_attribute_insert.json");
		String jsonString = IOUtils.toString(inputStream, Charset.defaultCharset());
		JSONArray jsonFileArray = new JSONArray(jsonString);
		inputStream.close();
		
		try {
			Client client = Client.create();
			// add basic authentication
			client.addFilter(new HTTPBasicAuthFilter(CreateCustomDataAttributes.REST_API_USER, CreateCustomDataAttributes.REST_API_PASSWORD));		
			WebResource webResource = client.resource(CreateCustomDataAttributes.CUSTOM_ATTRIBUTES_API_URL);
			
			// Get all existing attributes from Kuali
			ClientResponse getResponse = webResource.type("application/json").get(ClientResponse.class);			
			
			int newAttrID = 0;
			String output = null;
			JSONArray custDataAttrJsonArray = new JSONArray();
			
			// If values are found, return code 200.  No values, return is 404
			if (getResponse.getStatus() == 200 || getResponse.getStatus() == 404) {
				output = getResponse.getEntity(String.class);
				// If there are attributes, all ApiUtils.getMaxCode to find the highest id.  Set to 1 if there are no attributes in Kuali.
				if (getResponse.getStatus() == 200) {
					int maxAttrID = ApiUtils.getMaxCode(output, "id");
					newAttrID = maxAttrID + 1;
					custDataAttrJsonArray = new JSONArray(output);
				} else {
					newAttrID = 1;
				}							
							
				// Create array for new custom data attributes
				JSONArray newCustDataAttrJsonArray = new JSONArray();
				
				// Through through file array to make sure there are no duplicates added
				for (int i = 0; i < jsonFileArray.length(); ++i) {
					boolean match = false;					
					JSONObject newCustDataAttr = jsonFileArray.getJSONObject(i);
					
					// Loop through existing attributes to see if there is a matching record already in the system
					for (int ii = 0; ii < custDataAttrJsonArray.length(); ++ii) {
						if (custDataAttrJsonArray.getJSONObject(ii).getString("name").equals(newCustDataAttr.get("name"))) {
							match = true;
							break;
						}
					}
					
					// If there is not a match, update the id number and add to new array
					if (!match) {
						newCustDataAttr.put("id", "" + newAttrID + "");
						newCustDataAttrJsonArray.put(newCustDataAttr);
						newAttrID++;
					}
				}
				
				// Post the new attributes
				ClientResponse postResponse = webResource.type("application/json").post(ClientResponse.class, newCustDataAttrJsonArray.toString());
				// Response code 201 for success
				if (postResponse.getStatus() != 201) {
					System.out.println("ERROR reponse code: " + postResponse.getStatus());
				}						
			} else {
				throw new RuntimeException("Failed : HTTP error code : " + getResponse.getStatus());
			}
			System.out.println("customDataAttributeInsert: finished\n");
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	public static void customAttributeDocumentInsert() throws IOException {
		System.out.println("***************** customAttributeDocumentInsert *****************\n");
		
		InputStream inputStream = UpdateAwardTransTypes.class.getClassLoader().getResourceAsStream("custom_attribute_document_insert.json");
		String jsonString = IOUtils.toString(inputStream, Charset.defaultCharset());
		JSONArray custDocsFileJsonArray = new JSONArray(jsonString);
		inputStream.close();
		
		try {
			Client client = Client.create();
			// add basic authentication
			client.addFilter(new HTTPBasicAuthFilter(CreateCustomDataAttributes.REST_API_USER, CreateCustomDataAttributes.REST_API_PASSWORD));		
			WebResource webResource = client.resource(CreateCustomDataAttributes.CUSTOM_ATTRIBUTES_API_URL);
			
			ClientResponse getResponse = webResource.type("application/json").get(ClientResponse.class);	
			
			// Check if custom attribute exists, and get the ID number based on the name in the JSON file
			if (getResponse.getStatus() == 200) {		
				JSONArray newCustDocJsonArray = new JSONArray();
				String output = getResponse.getEntity(String.class);
				JSONArray custAttrJsonArray = new JSONArray(output);				
				
				// Loop through records from the file
				for (int i = 0; i < custDocsFileJsonArray.length(); ++i) {
					JSONObject newAttrDocObj = custDocsFileJsonArray.getJSONObject(i);
					
					// Loop through records returned from Kuali.  If there is a match, get the id number, remove the name parameter, and add to new array
					for (int ii = 0; ii < custAttrJsonArray.length(); ++ii) {
						if (custAttrJsonArray.getJSONObject(ii).getString("name").equals(newAttrDocObj.get("name"))) {
							newAttrDocObj.put("id", "" + custAttrJsonArray.getJSONObject(ii).getInt("id") + "");
							newAttrDocObj.remove("name");
							newCustDocJsonArray.put(newAttrDocObj);
							break;
						}
					}
				}
				
				// Continue if there are attributes in Kuali that match those in the JSON file
				if (newCustDocJsonArray.length() > 0) {					
					Client existingDocsClient = Client.create();
					// add basic authentication
					existingDocsClient.addFilter(new HTTPBasicAuthFilter(CreateCustomDataAttributes.REST_API_USER, CreateCustomDataAttributes.REST_API_PASSWORD));		
					WebResource existingDocsResource = existingDocsClient.resource(CreateCustomDataAttributes.CUSTOM_ATTRIBUTE_DOCS_API_URL);
					
					// Get a list of all attribute documents for loop to make sure no duplicates are created
					ClientResponse getExistingResponse = existingDocsResource.type("application/json").get(ClientResponse.class);
					
					// If values are found and returned, code 200.  If there are no values, return is 404
					if (getExistingResponse.getStatus() == 200 || getExistingResponse.getStatus() == 404) {
						JSONArray postCustDocJsonArray = new JSONArray();
						JSONArray existingDocsArray = new JSONArray();
						
						// If values are returned, add to existing array
						if (getExistingResponse.getStatus() == 200) {
							String returnExisting = getExistingResponse.getEntity(String.class);
							existingDocsArray = new JSONArray(returnExisting);
						}
						
						// Loop through new array and compare to existing records to avoid duplicates
						for (int i = 0; i < newCustDocJsonArray.length(); ++i) {
							boolean match = false;					
							JSONObject newCustDoc = newCustDocJsonArray.getJSONObject(i);
							
							for (int ii = 0; ii < existingDocsArray.length(); ++ii) {
								JSONObject existDoc = existingDocsArray.getJSONObject(ii);
								String existID = Integer.toString(existDoc.getInt("id"));
								
								if (existID.equals(newCustDoc.get("id")) && existDoc.getString("documentTypeName").equals(newCustDoc.get("documentTypeName"))) {
									match = true;
									break;
								}
							}				
							
							// If the new record has no match, add to array to be posted
							if (!match) {
								postCustDocJsonArray.put(newCustDoc);
							}
						}
						
						// Post any records that need to be added
						if (postCustDocJsonArray.length() > 0) {
							ClientResponse postResponse = existingDocsResource.type("application/json").post(ClientResponse.class, postCustDocJsonArray.toString());
							// Response code 201 for success
							if (postResponse.getStatus() != 201) {
								System.out.println("ERROR reponse code: " + postResponse.getStatus());
							}	
						} else {
							System.out.println("All new records already in Kuali");
						}
					} else {
						throw new RuntimeException("Failed : HTTP error code : " + getExistingResponse.getStatus());
					}
				} else {
					System.out.println("No attribute found for this document");
				}
				
			} else {
				throw new RuntimeException("Failed : HTTP error code : " + getResponse.getStatus());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("customAttributeDocumentInsert: finished\n");
	}
}
