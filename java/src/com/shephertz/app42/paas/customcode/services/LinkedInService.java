package com.shephertz.app42.paas.customcode.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

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
 * This Service has been return to get the information of User from LinkedIn
 * through Rest API and register them to App42 to maintain single account for an
 * App.
 * 
 * @author Himanshu Sharma
 *
 */
public class LinkedInService {
	private static String contentType = "application/json";
	private static String acceptType = "application/json";
	private static String GET = "GET";

	/**
	 * This method will return the user profile to custom code response along
	 * with session Id. Operation perform in this method, to check if the user
	 * exist than update LinkedIn information into Storage otherwise create new
	 * user with LinkedIn profile.
	 * 
	 * @param accessToken
	 *            - LinkedIn Access token
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
		JSONObject linkedInResponse = getLinkedInUserProfile(accessToken);
		if (linkedInResponse.has(Constants.FAULT)) {
			return linkedInResponse;
		} else {
			if (Constants.ADMIN_KEY != null)
				mSessionService.setAdminKey(Constants.ADMIN_KEY);
			String emailAddress = UtilService.getValue("emailAddress",
					linkedInResponse).toString();
			String sessionId = UtilService.getSessionId(mSessionService,
					emailAddress);
			mStorageService.setSessionId(sessionId);
			profile = checkUserExists(sessionId, emailAddress, mUserService,
					mStorageService, linkedInResponse);
			return profile;
		}
	}

	private JSONObject getLinkedInUserProfile(String accessToken)
			throws MalformedURLException, IOException, JSONException {
		String uri = "https://api.linkedin.com/v1/people/~:(id,first-name,last-name,email-address,public-profile-url,picture-url)?oauth2_access_token="
				+ accessToken + "&format=json";
		CustomCodeLog.debug("LinkedIn Rest URI : " + uri);
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
		CustomCodeLog.debug("LinkedIn response : " + sb.toString());
		JSONObject responseJSON = new JSONObject(sb.toString());
		if (responseJSON.has(Constants.ERROR)) {
			JSONObject errorJSON = new JSONObject();
			errorJSON.put(
					Constants.FAULT,
					responseJSON.getJSONObject(Constants.ERROR).getString(
							Constants.MESSAGE));
			return errorJSON;
		} else {
			return responseJSON;
		}
	}

	private JSONObject getProfileBuilder(JSONObject linkedInResponse)
			throws JSONException {
		JSONObject profile = new JSONObject();
		String userName = UtilService.getValue("firstName", linkedInResponse)
				+ " " + UtilService.getValue("lastName", linkedInResponse);
		profile.put(Constants.LAST_NAME,
				(String) UtilService.getValue("lastName", linkedInResponse));
		profile.put(Constants.FIRST_NAME,
				(String) UtilService.getValue("firstName", linkedInResponse));
		profile.put(Constants.EMAIL_ID,
				(String) UtilService.getValue("emailAddress", linkedInResponse));
		profile.put(Constants.USER_NAME, userName);
		profile.put("linkedInId",
				(String) UtilService.getValue(Constants.ID, linkedInResponse));
		profile.put("linkedInProfile", (String) UtilService.getValue(
				"publicProfileUrl", linkedInResponse));
		profile.put("linkedInPhotoUrl",
				(String) UtilService.getValue("pictureUrl", linkedInResponse));
		return profile;
	}

	private JSONObject checkUserExists(String sessionId, String email,
			UserService mUserService, StorageService mStorageService,
			JSONObject linkedInResponse) throws Exception {
		JSONObject profile = new JSONObject();
		JSONObject profileJSON = new JSONObject();

		try {
			Storage mStorage = UtilService.userExistInDataBase(mStorageService,
					email);
			profileJSON = new JSONObject(mStorage.getJsonDocList().get(0)
					.getJsonDoc());
			profileJSON.put("linkedInId",
					UtilService.getValue(Constants.ID, linkedInResponse));
			profileJSON.put("linkedInProfile",
					UtilService.getValue("publicProfileUrl", linkedInResponse));
			profileJSON.put("linkedInPhotoUrl",
					UtilService.getValue("pictureUrl", linkedInResponse));
			mStorageService.addOrUpdateKeys(Constants.DATABASE_NAME,
					Constants.COLLECTION_NAME, mStorage.getJsonDocList().get(0)
							.getDocId(), profileJSON);
			profileJSON.put(Constants.SESSION_ID, sessionId);
			profile.put(Constants.RESPONSE_JSON_KEY, profileJSON);

		} catch (App42Exception e) {
			if (e.getAppErrorCode() == 2601) {
				profile = registerLinkedInUser(linkedInResponse, mUserService,
						mStorageService);

			}
		} catch (Exception e) {

		}
		return profile;
	}

	private JSONObject registerLinkedInUser(JSONObject linkedInResponse,
			UserService mUserService, StorageService mStorageService)
			throws Exception {
		String email = linkedInResponse.getString("emailAddress");
		String password = UtilService.encrypt(linkedInResponse
				.getString("firstName"));
		User user = UtilService.registerUser(mUserService, email, password);
		mStorageService.setSessionId(user.getSessionId());
		JSONObject profile = new JSONObject();
		JSONObject profileJSON = getProfileBuilder(linkedInResponse);
		mStorageService.insertJSONDocument(Constants.DATABASE_NAME,
				Constants.COLLECTION_NAME, profileJSON);
		UtilService.sendCutomEmail(profileJSON, password);
		profileJSON.put(Constants.SESSION_ID, user.getSessionId());
		profile.put(Constants.RESPONSE_JSON_KEY, profileJSON);

		return profile;
	}

}
