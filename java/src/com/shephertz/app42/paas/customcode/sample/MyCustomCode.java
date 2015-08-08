package com.shephertz.app42.paas.customcode.sample;

import org.json.JSONException;
import org.json.JSONObject;

import com.shephertz.app42.paas.customcode.Executor;
import com.shephertz.app42.paas.customcode.HttpRequestObject;
import com.shephertz.app42.paas.customcode.HttpResponseObject;
import com.shephertz.app42.paas.customcode.services.CustomCodeLog;
import com.shephertz.app42.paas.customcode.services.FBService;
import com.shephertz.app42.paas.customcode.services.GitHubService;
import com.shephertz.app42.paas.customcode.services.GoogleService;
import com.shephertz.app42.paas.customcode.services.LinkedInService;
import com.shephertz.app42.paas.sdk.java.App42Exception;
import com.shephertz.app42.paas.sdk.java.ServiceAPI;
import com.shephertz.app42.paas.sdk.java.session.SessionService;
import com.shephertz.app42.paas.sdk.java.storage.StorageService;
import com.shephertz.app42.paas.sdk.java.user.UserService;
/**
 * 
 * @author Himanshu Sharma
 *
 */
public class MyCustomCode implements Executor {

	private final int HttpResultSuccess = 200;
	private final int InternalServerError = 500;
	private static String BadRequestMessage = "Request parameters are invalid";
	private final int BadRequest = 400;

	/**
	 * Write your custom code inside this method
	 */
	public HttpResponseObject execute(HttpRequestObject request) {
		ServiceAPI sp = new ServiceAPI(Constants.API_KEY, Constants.SECRET_KEY);
		String accessToken = null;
		JSONObject body = request.getBody();
		CustomCodeLog.debug("Request Params : " + body);
		SessionService mSessionService = sp.buildSessionManager();
		UserService mUserService = sp.buildUserService();
		StorageService mStorageService = sp.buildStorageService();
		JSONObject jsonResponse = new JSONObject();
		try {
			/**
			 * Getting information from LinkedIn through access token
			 */
			if (body.has("linkedIn" + Constants.ACCESS_TOKEN)) {
				accessToken = body
						.getString("linkedIn" + Constants.ACCESS_TOKEN);
				jsonResponse = new LinkedInService()
						.sessionCreationAndProfileSave(accessToken,
								mUserService, mSessionService, mStorageService);
				if (jsonResponse.has(Constants.FAULT)) {
					return new HttpResponseObject(BadRequest, jsonResponse.put(
							Constants.MESSAGE,
							jsonResponse.getString(Constants.FAULT)));
				} else {
					return new HttpResponseObject(HttpResultSuccess,
							getResponse(jsonResponse));
				}
			} 
			/**
			 * Getting information from Google through access token
			 */
			else if (body.has("google" + Constants.ACCESS_TOKEN)) {
				accessToken = body.getString("google" + Constants.ACCESS_TOKEN);
				jsonResponse = new GoogleService()
								.sessionCreationAndProfileSave(accessToken,
										mUserService, mSessionService,
										mStorageService);
				if (jsonResponse.has(Constants.FAULT)) {
					return new HttpResponseObject(BadRequest, jsonResponse.put(
							Constants.MESSAGE,
							jsonResponse.getString(Constants.FAULT)));
				} else {
					return new HttpResponseObject(HttpResultSuccess,
							getResponse(jsonResponse));
				}
			}
			/**
			 * Getting information from FaceBook through access token
			 */
			else if (body.has("facebook" + Constants.ACCESS_TOKEN)) {
				accessToken = body.getString("facebook" + Constants.ACCESS_TOKEN);
				jsonResponse = new FBService().sessionCreationAndProfileSave(
						accessToken, mUserService, mSessionService,
						mStorageService);
				if (jsonResponse.has(Constants.FAULT)) {
					return new HttpResponseObject(BadRequest, jsonResponse.put(
							Constants.MESSAGE,
							jsonResponse.getString(Constants.FAULT)));
				} else {
					return new HttpResponseObject(HttpResultSuccess,
							getResponse(jsonResponse));
				}
			} 
			/**
			 * Getting information from Github through access token
			 */
			else if (body.has("github" + Constants.ACCESS_TOKEN)) {
				accessToken = body.getString("github" + Constants.ACCESS_TOKEN);
				jsonResponse = new GitHubService().sessionCreationAndProfileSave(
						accessToken, mUserService, mSessionService,
						mStorageService);
				if (jsonResponse.has(Constants.FAULT)) {
					return new HttpResponseObject(BadRequest, jsonResponse.put(
							Constants.MESSAGE,
							jsonResponse.getString(Constants.FAULT)));
				} else {
					return new HttpResponseObject(HttpResultSuccess,
							getResponse(jsonResponse));
				}
			} else {
				return new HttpResponseObject(BadRequest, jsonResponse.put(
						Constants.MESSAGE, BadRequestMessage));
			}
		} catch (Exception e) {
			return getHandledResponse(e);
		}
	}

	/**
	 * Handle response in case of Exception
	 * 
	 * @param exception
	 * @return
	 */
	public HttpResponseObject getHandledResponse(Exception exception) {
		CustomCodeLog.debug("Cutom code exception " + exception);
		try {
			if (exception instanceof App42Exception) {
				App42Exception app42Exception = (App42Exception) exception;
				return new HttpResponseObject(
						app42Exception.getHttpErrorCode(),
						getJsonMessage(app42Exception.getMessage()));
			} else {
				return new HttpResponseObject(InternalServerError,
						new JSONObject().put(Constants.MESSAGE,
								exception.getMessage()));
			}
		} catch (JSONException jsonException) {
			return new HttpResponseObject(InternalServerError, null);
		}
	}
	/**
	 * Handle to create response JSON
	 * 
	 * @param jsonResponse
	 * @return
	 * @throws JSONException
	 */
	public JSONObject getResponse(JSONObject jsonResponse) throws JSONException {
		JSONObject responseObject = new JSONObject();
		JSONObject app42object = new JSONObject();
		responseObject.put(Constants.RESPONSE, jsonResponse);
		app42object.put(Constants.KEY_APP42, responseObject);
		CustomCodeLog.debug("Response : " + app42object);
		return app42object;
	}

	/**
	 * @param messageJson
	 * @return
	 * @throws JSONException
	 */
	private JSONObject getJsonMessage(String messageJson) throws JSONException {
		JSONObject json = new JSONObject(messageJson);
		String message = (json.getJSONObject(Constants.KEY_APP42_FAULT))
				.getString(Constants.DETAILS);
		return new JSONObject().put(Constants.MESSAGE, message);
	}
}
