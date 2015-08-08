package com.shephertz.app42.paas.customcode.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.shephertz.app42.paas.customcode.sample.Constants;
import com.shephertz.app42.paas.sdk.java.App42Exception;
import com.shephertz.app42.paas.sdk.java.session.SessionService;
import com.shephertz.app42.paas.sdk.java.storage.Storage;
import com.shephertz.app42.paas.sdk.java.storage.StorageService;
import com.shephertz.app42.paas.sdk.java.user.User;
import com.shephertz.app42.paas.sdk.java.user.UserService;

/**
 * This Service has been return to get the information of User from Google
 * through Rest API and register them to App42 to maintain single account for an
 * App.
 * 
 * @author Himanshu Sharma
 *
 */
public class GoogleService {
	private static String contentType = "application/json";
	private static String acceptType = "application/json";
	private static String GET = "GET";

	/**
	 * This method will return the user profile to custom code response along
	 * with session Id. Operation perform in this method, to check if the user
	 * exist than update Google information into Storage otherwise create new
	 * user with Google profile.
	 * 
	 * @param accessToken
	 *            - Google Access token
	 * @param mUserService
	 *            - Instance of User Service
	 * @param mSessionService
	 *            - Instance of Session Service
	 * @param mStorageService
	 *            - Instance of Storage Service
	 * @return - User profile along with sessionId of User
	 * @throws Exception
	 */
	public JSONObject sessionCreationAndProfileSave(String accessToken,
			UserService mUserService, SessionService mSessionService,
			StorageService mStorageService) throws Exception {
		JSONObject profile = new JSONObject();
		JSONObject googleResponse = getGoogleUserProfile(accessToken);
		if (googleResponse.has(Constants.FAULT)) {
			return googleResponse;
		} else {
			if (Constants.ADMIN_KEY != null)
				mSessionService.setAdminKey(Constants.ADMIN_KEY);
			String emailAddress = (String) getEmailFromArray("value",
					googleResponse);
			String sessionId = UtilService.getSessionId(mSessionService,
					emailAddress);
			mStorageService.setSessionId(sessionId);
			profile = checkUserExists(sessionId, emailAddress, mUserService,
					mStorageService, googleResponse);
			return profile;
		}
	}
	
	private JSONObject getGoogleUserProfile(String accessToken)
			throws JSONException {
		try {
			String uri = "https://www.googleapis.com/plus/v1/people/me?access_token="
					+ accessToken;
			CustomCodeLog.debug("Google Rest URI : " + uri);
			HttpURLConnection connection = (HttpURLConnection) new URL(uri)
					.openConnection();
			connection.setRequestProperty("Content-Type", contentType);
			connection.setRequestProperty("Accept", acceptType);
			connection.setRequestMethod(GET);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));
			String input;
			StringBuilder sb = new StringBuilder();
			while ((input = br.readLine()) != null) {
				sb.append(input);
			}
			br.close();
			CustomCodeLog.debug("Google response : " + sb.toString());
			JSONObject responseJSON = new JSONObject(sb.toString());
			return responseJSON;
		} catch (Exception ex) {
			if (ex.getMessage().contains("401")) {
				JSONObject errorJSON = new JSONObject();
				errorJSON
						.put(Constants.FAULT,
								"Client is not authorized. Please validate your access token");
				return errorJSON;
			} else {
				JSONObject errorJSON = new JSONObject();
				errorJSON.put(Constants.FAULT, ex.getMessage());

				return errorJSON;
			}
		}
	}

	private JSONObject getProfileBuilder(JSONObject googleResponse)
			throws JSONException {
		JSONObject profile = new JSONObject();
		String userName = (String) UtilService.getValue("displayName", googleResponse);
		profile.put(
				Constants.LAST_NAME,
				(String) UtilService.getValue("familyName",
						googleResponse.getJSONObject("name")));
		profile.put(
				Constants.FIRST_NAME,
				(String) UtilService.getValue("givenName",
						googleResponse.getJSONObject("name")));
		profile.put(Constants.EMAIL_ID,
				(String) getEmailFromArray("value", googleResponse));
		profile.put(Constants.USER_NAME, userName);
		profile.put("googleId", (String) UtilService.getValue(Constants.ID, googleResponse));
		profile.put("googleProfile", (String) UtilService.getValue("url", googleResponse));
		profile.put("googlePhotoUrl",
				(String) UtilService.getValue("url", googleResponse.getJSONObject("image")));
		return profile;
	}


	private JSONObject checkUserExists(String sessionId, String email,
			UserService mUserService, StorageService mStorageService,
			JSONObject googleResponse) throws Exception {
		JSONObject profile = new JSONObject();
		JSONObject profileJSON = new JSONObject();

		try {
			Storage mStorage = UtilService.userExistInDataBase(mStorageService,
					email);
			profileJSON = new JSONObject(mStorage.getJsonDocList().get(0)
					.getJsonDoc());
			profileJSON.put("googleId",
					(String) UtilService.getValue(Constants.ID, googleResponse));
			profileJSON.put("googleProfile",
					(String) UtilService.getValue("url", googleResponse));
			profileJSON.put(
					"googlePhotoUrl",
					(String) UtilService.getValue("url",
							googleResponse.getJSONObject("image")));
			mStorageService.addOrUpdateKeys(Constants.DATABASE_NAME,
					Constants.COLLECTION_NAME, mStorage.getJsonDocList().get(0)
							.getDocId(), profileJSON);
			profileJSON.put(Constants.SESSION_ID, sessionId);
			profile.put(Constants.RESPONSE_JSON_KEY, profileJSON);

		} catch (App42Exception e) {
			if (e.getAppErrorCode() == 2601) {
				profile = registerGoogleUser(googleResponse, mUserService,
						mStorageService);

			}
		} catch (Exception e) {

		}
		return profile;
	}
	private Object getEmailFromArray(String prop, JSONObject obj) {
		if (obj.has("emails")) {
			try {
				JSONArray jsonO = obj.getJSONArray("emails");
				JSONObject emailObject = jsonO.getJSONObject(0);
				return emailObject.get(prop);
			} catch (JSONException e) {
				return null;
			}
		}
		return "";
	}
	private JSONObject registerGoogleUser(JSONObject googleResponse,
			UserService mUserService, StorageService mStorageService)
			throws Exception {
		String email = (String) getEmailFromArray("value", googleResponse);
		;
		String password = UtilService.encrypt(googleResponse.getJSONObject(
				"name").getString("givenName"));
		User user = UtilService.registerUser(mUserService, email, password);
		mStorageService.setSessionId(user.getSessionId());
		JSONObject profile = new JSONObject();
		JSONObject profileJSON = getProfileBuilder(googleResponse);
		mStorageService.insertJSONDocument(Constants.DATABASE_NAME,
				Constants.COLLECTION_NAME, profileJSON);
		UtilService.sendCutomEmail(profileJSON, password);
		profileJSON.put(Constants.SESSION_ID, user.getSessionId());
		profile.put(Constants.RESPONSE_JSON_KEY, profileJSON);

		return profile;
	}
}
