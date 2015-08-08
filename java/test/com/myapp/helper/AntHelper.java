package com.myapp.helper;

import org.json.JSONException;
import org.json.JSONObject;

import com.shephertz.app42.paas.sdk.java.ServiceAPI;
import com.shephertz.app42.paas.sdk.java.customcode.CustomCodeService;

public class AntHelper {
	
	
	
	/**
	 * Deploy the Custom Code module by given name and jar file location
	 * @param apiKey
	 * @param secretKey
	 * @param name
	 */
	public static void upload(String apiKey, String secretKey, String name) {
		ServiceAPI sp = new ServiceAPI(apiKey, secretKey);
		CustomCodeService customCodeService = sp.buildCustomCodeService();
		customCodeService.deployJarFile(name, name+".jar");
	}
	
	public static void main(String[] args) throws Exception {
		if(args==null || args.length <4) {
			throw new Exception("Please provide required arguments...apikey, secretkey, name, action");
		}
		if(args[3].equals("upload")) {
			upload(args[0], args[1], args[2]);
		}else if(args[3].equals("run")) {
			run(args[0], args[1], args[2], args[4]);
		}
		else if(args[3].equals("undeploy")) {
			undeploy(args[0], args[1], args[2]);
		}
	}

	/**
	 * Undeploy the Custom Code module by Given Name
	 * @param apiKey
	 * @param secretKey
	 * @param name
	 */
	public static void undeploy(String apiKey, String secretKey, String name) {
		ServiceAPI sp = new ServiceAPI(apiKey, secretKey);
		CustomCodeService customCodeService = sp.buildCustomCodeService();
		customCodeService.undeployJarFile(name);	
	}

	/**
	 * Runs the cloud code by given name
	 * @param apiKey
	 * @param secretKey
	 * @param name
	 * @param body
	 * @throws JSONException
	 */
	public static void run(String apiKey, String secretKey, String name, String body) throws JSONException {
		ServiceAPI sp = new ServiceAPI(apiKey, secretKey);
		CustomCodeService customCodeService = sp.buildCustomCodeService();
		if(body!=null)
			System.out.println("Response From Custom Code : " + customCodeService.runJavaCode(name, new JSONObject(body)));
		else {
			System.out.println("Response From Custom Code : " + customCodeService.runJavaCode(name, null));
		}
	}

}
