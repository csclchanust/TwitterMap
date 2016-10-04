package chan.eddie.twittermap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import junit.framework.Assert;
import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import winterwell.jtwitter.OAuthSignpostClient;
import winterwell.jtwitter.Twitter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;

@SuppressLint("NewApi")
public class AuthenticateActivity extends Activity {
	private static final String TWITTER_REQUEST_TOKEN_URL = "https://api.twitter.com/oauth/request_token";
	private static final String TWITTER_ACCESS_TOKEN_URL = "https://api.twitter.com/oauth/access_token";
	private static final String TWITTER_AUTHORIZE_URL = "https://api.twitter.com/oauth/authorize";
	private static final String TWITTER_VERIFIER_URL ="https://api.twitter.com/1/account/verify_credentials.json";
	private static Uri CALLBACK_URI = Uri.parse("eddie://simplemap");

	private static String twitterUser = "any";

	public static final String USER_TOKEN = "user_token";
	public static final String USER_SECRET = "user_secret";
	public static final String REQUEST_TOKEN = "request_token";
	public static final String REQUEST_SECRET = "request_secret";
	public static final String PREFS = "MyPrefsFile";

	private static OAuthConsumer mConsumer = null;
	private static OAuthProvider mProvider = null;
	private static OAuthSignpostClient oauthClient;

	private static SharedPreferences mSettings;
	private static Twitter twitter;

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		// Instantiate the OAuthConsumer with the consumer key and secret. 
		// It's empty in this instance because we don't have our access token and access token secret.
		mConsumer = new CommonsHttpOAuthConsumer(
				getString(R.string.consumer_key), getString(R.string.consumer_secret));
		// Instantiate the CommonsHttpOAuthProvider object with the request token, access token and authorize URLs.
		mProvider = new CommonsHttpOAuthProvider (
				TWITTER_REQUEST_TOKEN_URL, 
				TWITTER_ACCESS_TOKEN_URL,
				TWITTER_AUTHORIZE_URL);

		mProvider.setOAuth10a(true); // Make it to support OAuth 1.0a.

		mSettings = this.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
		
		// Force to wait until the network gets response
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
	    StrictMode.setThreadPolicy(policy);

		Intent i = this.getIntent();

		if (i.getData() == null) {
			try {
				// Pass the consumer key and secret as the parameters to the authentication URL.
				// Store the consumer key and secret to the shared preference.
				// Open a new WebView activity with the authentication URL to start the authentication process.
				String authUrl = mProvider.retrieveRequestToken(mConsumer, CALLBACK_URI.toString());
				saveRequestInformation(mSettings, mConsumer.getToken(), mConsumer.getTokenSecret());
				this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)));
			} catch (OAuthMessageSignerException e) {
				e.printStackTrace();
			} catch (OAuthNotAuthorizedException e) {
				e.printStackTrace();
			} catch (OAuthExpectationFailedException e) {
				e.printStackTrace();
			} catch (OAuthCommunicationException e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized static Twitter getTwitter(Context c){
		if(twitter==null){ // Restart the authentication again if it is null
			// Force to wait until the network gets response
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		    StrictMode.setThreadPolicy(policy);

			mConsumer = new CommonsHttpOAuthConsumer(
					c.getString(R.string.consumer_key), c.getString(R.string.consumer_secret));

			try {
				mSettings = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
				
				// Read the saved userToken and userSecret from Shared Preferences
				String userToken = mSettings.getString(USER_TOKEN , null);
				String userSecret = mSettings.getString(USER_SECRET , null);
				oauthClient = new OAuthSignpostClient(c.getString(R.string.consumer_key), c.getString(R.string.consumer_secret), userToken, userSecret);
				HttpGet request = new HttpGet(TWITTER_VERIFIER_URL);
				
				// sign the request to authenticate
				mConsumer.sign(request);
				HttpClient mClient = new DefaultHttpClient();
				HttpResponse response = mClient.execute(request);

				// Get the output from the https page
				String content = inputStreamToString(response.getEntity().getContent());

				JSONObject jso = new JSONObject(content);
				twitterUser=jso.optString("name");
				
				// Make a Twitter object
				twitter = new Twitter(twitterUser, oauthClient);
			} catch (OAuthMessageSignerException e) {
				e.printStackTrace();
			} catch (OAuthExpectationFailedException e) {
				e.printStackTrace();
			} catch (OAuthCommunicationException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return twitter;			
	}
	
	public static void setCallbackURL(String url){
		CALLBACK_URI = Uri.parse(url);
	}
	
	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		String userToken = null, userSecret = null;
		Uri uri = intent.getData();

		if (uri != null && CALLBACK_URI.getScheme().equals(uri.getScheme())) {
			try {
				// Get the verifier string from the URI 
				String verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);
				String otoken = uri.getQueryParameter(OAuth.OAUTH_TOKEN);

				// Apparently, the verifier is decoded to get the secret, which is then compared - crafty
				// This is a sanity check which should never fail - hence the assertion
				Assert.assertEquals(otoken, mConsumer.getToken());

				// Retrieve the access token with the consumer object using the verifier
				mProvider.retrieveAccessToken(mConsumer, verifier);
				// Retrieve the user token and secret
				userToken = mConsumer.getToken();
				userSecret = mConsumer.getTokenSecret();

				// Save the user token and secret
				saveAuthInformation(mSettings, userToken, userSecret);
				// Clear the request token and secret
				saveRequestInformation(mSettings, null, null);

				// We have the token and use it to create a Twitter instance
				mConsumer.setTokenWithSecret(userToken, userSecret);
				oauthClient = new OAuthSignpostClient(getString(R.string.consumer_key), getString(R.string.consumer_secret), userToken, userSecret);

				HttpGet request = new HttpGet(TWITTER_VERIFIER_URL);
				
				// sign the request to authenticate
				mConsumer.sign(request);
				HttpClient mClient = new DefaultHttpClient();
				HttpResponse response = mClient.execute(request);

				// Get the output from the https page
				String content = inputStreamToString(response.getEntity().getContent());

				JSONObject jso = new JSONObject(content);
				twitterUser=jso.optString("name");
				// Make a Twitter object
				
				twitter = new Twitter(twitterUser, oauthClient);

				// Currently, we get back to the main activity that required to use the Twitter service.
				Intent i = new Intent(this, MainActivity.class); 
				startActivity(i);
			} catch (OAuthMessageSignerException e) {
				e.printStackTrace();
			} catch (OAuthNotAuthorizedException e) {
				e.printStackTrace();
			} catch (OAuthExpectationFailedException e) {
				e.printStackTrace();
			} catch (OAuthCommunicationException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				finish();
			}
		}
	}

	// Slow Implementation
	private static String inputStreamToString(InputStream is) throws IOException {
		String s = "";
		String line = "";

		// Wrap a BufferedReader around the InputStream
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));

		// Read response until the end
		while ((line = rd.readLine()) != null) { s += line; }

		// Return full string
		return s;
	}

	public static void saveRequestInformation(SharedPreferences settings, String token, String secret) {
		// null means to clear the old values
		SharedPreferences.Editor editor = settings.edit();
		if(token == null) {
			editor.remove(AuthenticateActivity.REQUEST_TOKEN);
		}
		else {
			editor.putString(AuthenticateActivity.REQUEST_TOKEN, token);
		}
		if (secret == null) {
			editor.remove(AuthenticateActivity.REQUEST_SECRET);
		}
		else {
			editor.putString(AuthenticateActivity.REQUEST_SECRET, secret);
		}
		editor.commit();

	}

	public static void saveAuthInformation(SharedPreferences settings, String token, String secret) {
		// null means to clear the old values
		SharedPreferences.Editor editor = settings.edit();
		if(token == null) {
			editor.remove(AuthenticateActivity.USER_TOKEN);
		}
		else {
			editor.putString(AuthenticateActivity.USER_TOKEN, token);
		}
		if (secret == null) {
			editor.remove(AuthenticateActivity.USER_SECRET);
		}
		else {
			editor.putString(AuthenticateActivity.USER_SECRET, secret);
		}
		editor.commit();
	}

}