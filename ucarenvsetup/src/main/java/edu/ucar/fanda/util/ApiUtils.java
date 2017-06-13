package edu.ucar.fanda.util;

import org.json.JSONArray;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class ApiUtils {
	public static int getMaxCode(String output, String codeType) {
		int maxCode = 0;
		
		// Create an array from the output passed into method
		JSONArray jsonArray = new JSONArray(output);
		
		// Loop through arrayu and find the maximum code
		for (int i = 0; i < jsonArray.length(); ++i) {					
			int code = jsonArray.getJSONObject(i).getInt(codeType);					
			if (code > maxCode) {
				maxCode = code;
			}
		}	
		return maxCode;
	}
}
