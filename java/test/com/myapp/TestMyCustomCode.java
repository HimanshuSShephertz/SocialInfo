/**
 * 
 */
package com.myapp;

import java.util.HashMap;

import org.json.JSONObject;

import com.shephertz.app42.paas.customcode.Executor;
import com.shephertz.app42.paas.customcode.HttpRequestObject;
import com.shephertz.app42.paas.customcode.sample.MyCustomCode;
import com.shephertz.app42.paas.customcode.services.CustomCodeLog;

/**
 * Local Setup Tester
 * 
 * @author Himanshu Sharma
 *
 */
public class TestMyCustomCode {

	/**
	 * Sets the param and body and call the execute method for test
	 * 
	 * @throws Exception
	 */
	public static void testMyCode() throws Exception {

		Executor executor = new MyCustomCode();
		// Create Request Body
		JSONObject jsonBody = new JSONObject();
		jsonBody.put("githubAccessToken", "ddddddd");
		HttpRequestObject request = new HttpRequestObject(
				new HashMap<String, String>(), jsonBody);
		executor.execute(request);
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		testMyCode();
	}

}
