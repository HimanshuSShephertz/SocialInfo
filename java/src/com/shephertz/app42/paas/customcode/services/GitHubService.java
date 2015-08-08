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
 * This Service has been return to get the information of User from GitHub
 * through Rest API and register them to App42 to maintain single account for an
 * App.
 * 
 * @author Himanshu Sharma
 *
 */
public class GitHubService {
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
	 *         name,email id, GitHub Id etc.
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws JSONException
	 */
	private JSONObject getGitHubUserProfile(String accessToken)
			throws MalformedURLException, IOException, JSONException {
		String uri = "https://api.github.com/user?access_token=" + accessToken;
		CustomCodeLog.debug("GitHub Rest URI : " + uri);
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
		CustomCodeLog.debug("GitHub response : " + sb.toString());
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
	 * Converting GitHub profile into JSONObject to maintain single format for
	 * all social login
	 * 
	 * @param githubResponse
	 *            - JSONObject which hold the response coming from GitHub
	 * @return - JSONObject to Save into Storage Service
	 * @throws JSONException
	 */
	private JSONObject getProfileBuilder(JSONObject githubResponse)
			throws JSONException {
		JSONObject profile = new JSONObject();
		String firstName = (String) githubResponse.getString("name")
				.replaceAll(" .+$", "");
		String str  = (String) githubResponse.getString("name");
		String lastName = null;
		String[] vals = str.split(" ");
		if (vals.length > 1)
		    lastName = vals[1];
		profile.put(Constants.FIRST_NAME, firstName);
		profile.put(Constants.LAST_NAME, lastName);
		profile.put(Constants.EMAIL_ID, (String) UtilService.getValue(
				Constants.EMAIL_KEY, githubResponse));
		profile.put(Constants.USER_NAME,
				(String) UtilService.getValue("name", githubResponse));
		profile.put("githubId", 
				UtilService.getValue(Constants.ID, githubResponse).toString());
		profile.put("githubProfile",
				UtilService.getValue("html_url", githubResponse));
		profile.put("githubPhotoUrl",
				UtilService.getValue("avatar_url", githubResponse));
		return profile;
	}

	/**
	 * This method will return the user profile to custom code response along
	 * with session Id. Operation perform in this method, to check if the user
	 * exist than update GitHub information into Storage otherwise create new
	 * user with GitHub profile.
	 * 
	 * @param accessToken
	 *            - GitHub Access token
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
		JSONObject githubResponse = getGitHubUserProfile(accessToken);
		if (githubResponse.has(Constants.FAULT)) {
			return githubResponse;
		} else {
			if (Constants.ADMIN_KEY != null)
				mSessionService.setAdminKey(Constants.ADMIN_KEY);
			String emailAddress = UtilService.getValue(Constants.EMAIL_KEY,
					githubResponse).toString();
			String sessionId = UtilService.getSessionId(mSessionService,
					emailAddress);
			mStorageService.setSessionId(sessionId);
			profile = checkUserExists(sessionId, emailAddress, mUserService,
					mStorageService, githubResponse);
			return profile;
		}
	}

	private JSONObject checkUserExists(String sessionId, String email,
			UserService mUserService, StorageService mStorageService,
			JSONObject githubResponse) throws Exception {
		JSONObject profile = new JSONObject();
		JSONObject profileJSON = new JSONObject();
		try {
			Storage mStorage = UtilService.userExistInDataBase(mStorageService,
					email);
			profileJSON = new JSONObject(mStorage.getJsonDocList().get(0)
					.getJsonDoc());
			profileJSON.put("githubId",
					UtilService.getValue(Constants.ID, githubResponse));
			profileJSON.put("githubProfile",
					UtilService.getValue("html_url", githubResponse));
			profileJSON.put("githubPhotoUrl",
					UtilService.getValue("avatar_url", githubResponse));
			mStorageService.addOrUpdateKeys(Constants.DATABASE_NAME,
					Constants.COLLECTION_NAME, mStorage.getJsonDocList().get(0)
							.getDocId(), profileJSON);
			profileJSON.put(Constants.SESSION_ID, sessionId);
			profile.put(Constants.RESPONSE_JSON_KEY, profileJSON);

		} catch (App42Exception e) {
			if (e.getAppErrorCode() == 2601) {
				profile = registerGitHubUser(githubResponse, mUserService,
						mStorageService);

			}
		} catch (Exception e) {

		}
		return profile;
	}

	private JSONObject registerGitHubUser(JSONObject githubResponse,
			UserService mUserService, StorageService mStorageService)
			throws Exception {
		JSONObject profile = new JSONObject();
		String email = githubResponse.getString(Constants.EMAIL_KEY);
		String firstName = (String) githubResponse.getString("name")
				.replaceAll(" .+$", "");
		String password = UtilService.encrypt(firstName);
		User user = UtilService.registerUser(mUserService, email, password);
		mStorageService.setSessionId(user.getSessionId());
		JSONObject profileJSON = getProfileBuilder(githubResponse);
		mStorageService.insertJSONDocument(Constants.DATABASE_NAME,
				Constants.COLLECTION_NAME, profileJSON);
		UtilService.sendCutomEmail(profileJSON, password);
		profileJSON.put(Constants.SESSION_ID, user.getSessionId());
		profile.put(Constants.RESPONSE_JSON_KEY, profileJSON);

		return profile;
	}

}
