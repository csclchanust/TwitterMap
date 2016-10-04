package chan.eddie.twittermap;


import java.io.InputStream;
import java.net.MalformedURLException;
import winterwell.jtwitter.User;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;

/** Demonstrates customizing the info window and/or its contents. */
public class CustomInfoWindowAdapter implements InfoWindowAdapter {

	// These a both viewgroups containing an ImageView with id "badge" and two TextViews with id
	// "title" and "snippet".
	private final View mWindow;
	private final View mContents;
	private Context context;

	CustomInfoWindowAdapter(Context context) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mWindow = inflater.inflate(R.layout.custom_info_window, null);
		mContents = inflater.inflate(R.layout.custom_info_contents, null);
		this.context = context;
	}

	@Override
	public View getInfoWindow(Marker marker) {
		render(marker, mWindow);
		return mWindow;
	}

	@Override
	public View getInfoContents(Marker marker) {
		render(marker, mContents);
		return mContents;
	}

	private void render(Marker marker, View view) {
		String title = marker.getTitle();
		TextView titleUi = ((TextView) view.findViewById(R.id.title));

		String snippet ="";
		ImageView imageView = ((ImageView) view.findViewById(R.id.badge));
		imageView.setImageBitmap(null);

		if (title != null) {
			// Spannable string allows us to edit the formatting of the text.
			SpannableString titleText = new SpannableString(title);
			titleText.setSpan(new ForegroundColorSpan(Color.RED), 0, titleText.length(), 0);
			titleUi.setText(titleText);
			try {
				@SuppressWarnings("deprecation")
				User user = AuthenticateActivity.getTwitter(context).getUser(title);
				if(user!=null){
					InputStream content = (InputStream) user.
							getProfileImageUrl().toURL().getContent();
					Drawable profile = Drawable.createFromStream(
							content , user.getScreenName()); 
					Bitmap d= ((BitmapDrawable) profile).getBitmap();
					Bitmap userImage = Bitmap.createScaledBitmap(d, 50, 50, false);
					if(userImage!=null){
						imageView.setImageBitmap(userImage);
					}
					if(user.getStatus()!=null)
						snippet = user.getStatus().toString();
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}

		} else {
			titleUi.setText("");
		}

		if(marker.getSnippet()!=null){
			snippet = marker.getSnippet();
		}
		
		TextView snippetUi = ((TextView) view.findViewById(R.id.snippet));
		if (snippet != null) {
			SpannableString snippetText = new SpannableString(snippet);
			snippetUi.setText(snippetText);
		} else {
			snippetUi.setText("");
		}

	}
}