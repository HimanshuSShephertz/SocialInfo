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
 * This Service has been return to get the information of User from FaceBook
 * through Rest API and register them to App42 to maintain single account for an
 * App.
 * 
 * @author Himanshu Sharma
 *
 */
public class FBService {
	private static String contentType = "application/json";
	private static String acceptType = "application/json";
	private static String GET = "GET";

	/**
	 * Getting User information from FacebBook
	 * 
	 * @param accessToken
	 *            - Access token of User which contain the acccess to fetch the
	 *            user profile
	 * @return - JSONObject which contain the User profile like first & last
	 *         name,email id, FaceBook Id etc.
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws JSONException
	 */
	private JSONObject getFBUserProfile(String accessToken)
			throws MalformedURLException, IOException, JSONException {
		String uri = "https://graph.facebook.com/v2.4/me?access_token="
				+ accessToken
				+ "&debug=all&fields=id%2Cname%2Cfirst_name%2Cgender%2Clink%2Cpicture%7Burl%7D%2Clast_name%2Cemail"
				+ "&format=json&method=get&pretty=0&suppress_http_code=1";
		CustomCodeLog.debug("Facebook Rest URI : " + uri);
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
		CustomCodeLog.debug("Facebook response : " + sb.toString());
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

	/**
	 * Converting FaceBook profile into JSONObject to maintain single format for
	 * all social login
	 * 
	 * @param fbResponse
	 *            - JSONObject which hold the response coming from FaceBook
	 * @return - JSONObject to Save into Storage Service
	 * @throws JSONException
	 */
	private JSONObject getProfileBuilder(JSONObject fbResponse)
			throws JSONException {
		JSONObject profile = new JSONObject();
		profile.put(Constants.LAST_NAME, UtilService.getValue("last_name", fbResponse));
		profile.put(Constants.FIRST_NAME, UtilService.getValue("first_name", fbResponse));
		profile.put(Constants.EMAIL_ID,
				(String) UtilService.getValue(Constants.FB_EMAIL_ADDRESS, fbResponse));
		profile.put(Constants.USER_NAME, (String) UtilService.getValue("name", fbResponse));
		profile.put("facebookId", UtilService.getValue(Constants.ID, fbResponse));
		profile.put("facebookProfile", UtilService.getValue("link", fbResponse));
		if (fbResponse.has("picture")) {
			if (fbResponse.getJSONObject("picture").has("data"))
				profile.put(
						"facebookPhotoUrl",
						UtilService.getValue("url", fbResponse.getJSONObject("picture")
								.getJSONObject("data")));
		}
		return profile;
	}

	/**
	 * This method will return the user profile to custom code response along
	 * with session Id. Operation perform in this method, to check if the user
	 * exist than update FaceBook information into Storage otherwise create new
	 * user with FaceBook profile.
	 * 
	 * @param accessToken
	 *            - FaceBook Access token
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
		JSONObject fbResponse = getFBUserProfile(accessToken);
		if (fbResponse.has(Constants.FAULT)) {
			return fbResponse;
		} else {
			if (Constants.ADMIN_KEY != null)
				mSessionService.setAdminKey(Constants.ADMIN_KEY);
			String emailAddress = UtilService.getValue(Constants.FB_EMAIL_ADDRESS,
					fbResponse).toString();
			String sessionId = UtilService.getSessionId(mSessionService,
					emailAddress);
			mStorageService.setSessionId(sessionId);
			profile = checkUserExists(sessionId, emailAddress, mUserService,
					mStorageService, fbResponse);
			return profile;
		}
	}

	private JSONObject checkUserExists(String sessionId, String email,
			UserService mUserService, StorageService mStorageService,
			JSONObject fbResponse) throws Exception {
		JSONObject profile = new JSONObject();
		JSONObject profileJSON = new JSONObject();

		try {
			Storage mStorage = UtilService.userExistInDataBase(mStorageService,
					email);
			profileJSON = new JSONObject(mStorage.getJsonDocList().get(0)
					.getJsonDoc());
			profileJSON.put("facebookId", UtilService.getValue(Constants.ID, fbResponse));
			profileJSON.put("facebookProfile", UtilService.getValue("link", fbResponse));
			if (fbResponse.has("picture")) {
				if (fbResponse.getJSONObject("picture").has("data"))
					profileJSON.put(
							"facebookPhotoUrl",
							UtilService.getValue("url", fbResponse.getJSONObject("picture")
									.getJSONObject("data")));
			}
			mStorageService.addOrUpdateKeys(Constants.DATABASE_NAME,
					Constants.COLLECTION_NAME, mStorage.getJsonDocList().get(0)
							.getDocId(), profileJSON);
			profileJSON.put(Constants.SESSION_ID, sessionId);
			profile.put(Constants.RESPONSE_JSON_KEY, profileJSON);

		} catch (App42Exception e) {
			if (e.getAppErrorCode() == 2601) {
				profile = registerFacebookUser(fbResponse, mUserService,
						mStorageService);

			}
		} catch (Exception e) {

		}
		return profile;
	}

	private JSONObject registerFacebookUser(JSONObject fbResponse,
			UserService mUserService, StorageService mStorageService)
			throws Exception {
		String email = fbResponse.getString(Constants.FB_EMAIL_ADDRESS);
		String password = UtilService.encrypt(fbResponse
				.getString("first_name"));
		User user = UtilService.registerUser(mUserService, email, password);
		mStorageService.setSessionId(user.getSessionId());
		JSONObject profile = new JSONObject();
		JSONObject profileJSON = getProfileBuilder(fbResponse);
		mStorageService.insertJSONDocument(Constants.DATABASE_NAME,
				Constants.COLLECTION_NAME, profileJSON);
		UtilService.sendCutomEmail(profileJSON, password);
		profileJSON.put(Constants.SESSION_ID, user.getSessionId());
		profile.put(Constants.RESPONSE_JSON_KEY, profileJSON);

		return profile;
	}

}
