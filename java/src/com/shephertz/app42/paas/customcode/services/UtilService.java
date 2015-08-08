package com.shephertz.app42.paas.customcode.services;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.json.JSONException;
import org.json.JSONObject;

import com.shephertz.app42.paas.customcode.sample.Constants;
import com.shephertz.app42.paas.sdk.java.ServiceAPI;
import com.shephertz.app42.paas.sdk.java.email.EmailMIME;
import com.shephertz.app42.paas.sdk.java.email.EmailService;
import com.shephertz.app42.paas.sdk.java.session.SessionService;
import com.shephertz.app42.paas.sdk.java.storage.Storage;
import com.shephertz.app42.paas.sdk.java.storage.StorageService;
import com.shephertz.app42.paas.sdk.java.user.User;
import com.shephertz.app42.paas.sdk.java.user.UserService;

public class UtilService {

	public static String getSessionId(SessionService mSessionService,
			String emailAddress) throws Exception {
		return mSessionService.getSession(emailAddress).getSessionId();
	}

	public static Storage userExistInDataBase(StorageService mStorageService,
			String email) {
		return mStorageService.findDocumentByKeyValue(Constants.DATABASE_NAME,
				Constants.COLLECTION_NAME,Constants.EMAIL_ID, email);
	}

	public static User registerUser(UserService mUserService,
			String email,String password) throws Exception {
		User user = mUserService.createUser(email, password, email);
		return user;
	}

	public static String encrypt(String plainText) throws Exception {
		KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
		keyGenerator.init(128);
		SecretKey secretKey = keyGenerator.generateKey();
		byte[] plainTextByte = plainText.getBytes();
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, secretKey);
		byte[] encryptedByte = cipher.doFinal(plainTextByte);
		String encryptedText = com.shephertz.app42.paas.sdk.java.util.Base64
				.encodeBytes(encryptedByte).substring(0, 6);
		return encryptedText;
	}

	public static void sendCutomEmail(JSONObject profileObject, String password)
			throws JSONException {
		JSONObject object = new JSONObject();
		String fromEmail = Constants.SENDER_EMAIL_ID;
		object.put(
				"emailBody",
				"<p>Dear&nbsp;"
						+ profileObject.getString("fName")
						+ ",</p><p>Congratulations!&nbsp;Your&nbsp;account&nbsp;has&nbsp;been&nbsp;successfully&nbsp;created&nbsp;with&nbsp;us.&nbsp;<span style='line-height:1.6'>Your&nbsp;registered&nbsp;E-mail&nbsp;address&nbsp;will&nbsp;be&nbsp;used&nbsp;as&nbsp;the&nbsp;username&nbsp;for&nbsp;Login&nbsp;purpose.</span></p><p>Please&nbsp;refer&nbsp;to&nbsp;the&nbsp;details&nbsp;for&nbsp;your&nbsp;account&nbsp;:</p><p><strong>UserName&nbsp;</strong>:&nbsp;"
						+ profileObject.getString("emailId")
						+ "</p><p><strong>Password&nbsp;</strong>:&nbsp;"
						+ password
						+ "</p><p>&nbsp;</p><p>Thanks&nbsp;&amp;&nbsp;Regards</p><p><strong>Himanshu&nbsp;Sharma</strong></p><p>For&nbsp;further&nbsp;assistance&nbsp;or&nbsp;support,&nbsp;send&nbsp;us&nbsp;your&nbsp;queries&nbsp;at&nbsp;"
						+ fromEmail + "</p>");

		EmailService mEmailService = new ServiceAPI(Constants.API_KEY,
				Constants.SECRET_KEY).buildEmailService();
		mEmailService.sendMail(profileObject.getString("emailId"),
				Constants.EMAIL_SUBJECT, getHtmlBody(object), fromEmail,
				EmailMIME.HTML_TEXT_MIME_TYPE);

	}

	private static String getHtmlBody(JSONObject body) throws JSONException {
		String emailBody = "<html><body>" + body.getString("emailBody")
				+ "</body></html>";
		return emailBody;

	}
	public static Object getValue(String prop, JSONObject obj) {
		if (obj.has(prop)) {
			try {
				return obj.get(prop);
			} catch (JSONException e) {
				return null;
			}
		}
		return "";
	}

}
