package chan.eddie.twittermap;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import winterwell.jtwitter.Message;
import winterwell.jtwitter.Status;
import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.User;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements
OnClickListener, UserLocator.OnLocationListener,
OnMarkerClickListener, OnMapClickListener{

	public static final String TAG_POST_FRAGMENT = "post";
	private GoogleMap mMap;
	private SupportMapFragment mapFrag;
	private Marker myMarker, prevMarker;
	private List<Marker> friendMarker = new LinkedList<Marker>();
	private Twitter twitter;
	private User myself;
	private LatLng ownPoint;
	private UserLocator locator;
	private Message lastMsg;

	private CheckBox locateCB, previousCB, threeCB, satelliteCB, friendCB;
	private List<Status> sList;
	private List<Message> mList;
	private PostFragment postFragment;
	protected boolean isShowList = false;

	Timer regularUpdater;
	String result;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		twitter = AuthenticateActivity.getTwitter(this);
		myself = twitter.getSelf();
		List<Message> mList = twitter.getDirectMessages();
		if (mList.size() > 0)
			lastMsg = mList.get(0);

		// Initializes the Google Maps Android API so that its classes are ready for use
		try {
			MapsInitializer.initialize(this);
		} catch (GooglePlayServicesNotAvailableException e) {
			e.printStackTrace();
		}

		setUpMapIfNeeded();
		mapFrag.onCreate(savedInstanceState);

		locateCB = (CheckBox) findViewById(R.id.locateCB);
		previousCB = (CheckBox) findViewById(R.id.previousCB);
		threeCB= (CheckBox) findViewById(R.id.threeCB);
		satelliteCB = (CheckBox) findViewById(R.id.satelliteCB);
		friendCB = (CheckBox) findViewById(R.id.friendCB);

		locateCB.setOnClickListener ((OnClickListener) this);
		previousCB.setOnClickListener ((OnClickListener) this);
		threeCB.setOnClickListener ((OnClickListener) this);
		satelliteCB.setOnClickListener ((OnClickListener) this);  
		friendCB.setOnClickListener ((OnClickListener) this);

		// init the GPS / network positioning
		locator = new UserLocator(this);
		locator.setOnLocationListener(this);
	}

	@Override
	protected void onResumeFragments() {
		super.onResumeFragments();
		setUpMapIfNeeded();
		regularUpdater = new Timer();
		// Trigger the regular update for 10 minutes
		//regularUpdater.schedule(new Updater(), 6*10*10000);
		regularUpdater.scheduleAtFixedRate(new Updater(), 0, 20*1000); // 20 seconds

		if (locateCB.isChecked())
			locator.enableLocationUpdate();

		updateLayers();
		if (hasFragmentStack()) {
			getFragmentManager().popBackStack();
			getFragmentManager().executePendingTransactions();
		}
		mapFrag.onResume();
	}


	@Override
	protected void onPause() {
		Log.d("MainActivity", "onStop");
		super.onPause();

		regularUpdater.cancel();
		locator.disableLocationUpdate();
		mapFrag.onPause();
	}

	@Override
	protected void onDestroy() {
		Log.d("MainActivity", "onDestroy");
		super.onDestroy();

		// Clear everything in the memory
		SharedPreferences mSettings = this.getSharedPreferences(AuthenticateActivity.PREFS, Context.MODE_PRIVATE);
		AuthenticateActivity.saveRequestInformation(mSettings, null, null);
		AuthenticateActivity.saveAuthInformation(mSettings, null, null);

		regularUpdater.cancel();
		locator.disableLocationUpdate();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		locateCB.onSaveInstanceState();
		threeCB.onSaveInstanceState();
		satelliteCB.onSaveInstanceState();
		previousCB.onSaveInstanceState();
		friendCB.onSaveInstanceState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		Log.d("MainActivity", "onConfigurationChanged");

		// Checks the orientation of the screen
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			Toast.makeText(this, "Landscape view", Toast.LENGTH_SHORT).show();
		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
			Toast.makeText(this, "Portrait view", Toast.LENGTH_SHORT).show();
		}
	}

	private void setUpMapIfNeeded() {
		// Do a null check to confirm that we have not already instantiated the map.
		if (mMap == null) {
			mapFrag = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment));
			// Try to obtain the map from the SupportMapFragment.
			mMap = mapFrag.getMap();
		}
		mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(this));
		mMap.setOnMapClickListener(this);
		mMap.setOnMarkerClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if(v==locateCB){
			if(locateCB.isChecked()){
				Toast.makeText(this,"Retreiving Location ...",Toast.LENGTH_SHORT).show();
				locator.enableLocationUpdate();
				previousCB.setChecked(false);
			}else{
				Toast.makeText(this,"Disable GPS ...",Toast.LENGTH_SHORT).show();
				locator.disableLocationUpdate();
				if(myMarker!=null)
					myMarker.remove();
			}
		}

		if(v==previousCB){
			if(previousCB.isChecked()){
				Toast.makeText(this,"Capturing previous position ...",Toast.LENGTH_SHORT).show();
				locateCB.setChecked(false);
				locator.disableLocationUpdate();
				showPrevSelfStatus();
				// Move the camera instantly to your current location with a zoom of 17.
				if(ownPoint!=null)
					mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ownPoint, 17));
				else{
					Toast.makeText(this,"Unable to retreive the previous location ...",Toast.LENGTH_SHORT).show();
					previousCB.setChecked(false);
				}
			}else{
				if(prevMarker!=null)
					prevMarker.remove();
			}
		}


		if(v==threeCB){
			if(threeCB.isChecked())        
				display3DMap(true);
			else
				display3DMap(false);
		}

		if(v==satelliteCB){
			if(satelliteCB.isChecked())        
				displaySatelliteMap(true);
			else
				displaySatelliteMap(false);
		}

		if(v==friendCB){
			if(!friendCB.isChecked()){
				removeFriendMarker();
			}else{
				new GetFriendsStatusTask().execute();
			}
		}
	}

	public void showSelfStatus(){
		if(ownPoint!=null){
			// Clear the previous marker
			if(prevMarker!=null)
				prevMarker.remove();

			// Clear the user marker
			if(myMarker!=null)
				myMarker.remove();

			myMarker= mMap.addMarker(new MarkerOptions().position(ownPoint)
					.title(myself.getScreenName())
					.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
		}	
	}

	public void showPrevSelfStatus(){
		// Clear the GPS marker
		if(myMarker!=null)
			myMarker.remove();

		if(twitter!=null)
			if(twitter.getStatus()!=null)
				if(twitter.getStatus().getLatLng()!=null){
					double[] latlng = twitter.getStatus().getLatLng();
					if(latlng!=null){
						ownPoint = new LatLng(latlng[0],latlng[1]);
					}
				}

		if(ownPoint!=null){
			// Clear the user marker
			if(prevMarker!=null)
				prevMarker.remove();

			prevMarker= mMap.addMarker(new MarkerOptions().position(ownPoint)
					.title(myself.getScreenName())
					.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
		}


	}

	private void display3DMap(boolean enabled){
		// Retrieve the current camera position
		CameraPosition currentPos = mMap.getCameraPosition(); 
		CameraPosition cameraPosition = currentPos;
		if(enabled){
			// Creates a CameraPosition from the builder
			cameraPosition = new CameraPosition.Builder().target(currentPos.target)  // Sets the center of the map to Mountain View
					.zoom(17)                   // Sets the zoom
					.bearing(currentPos.bearing) // Sets the orientation of the camera to the original bearing
					.tilt(45)                   // Sets the tilt of the camera to 45 degrees
					.build(); 
		}else{
			cameraPosition = new CameraPosition.Builder().target(currentPos.target)  // Sets the center of the map to Mountain View
					.zoom(currentPos.zoom)                   // Sets the zoom
					.bearing(currentPos.bearing) // Sets the orientation of the camera to the original bearing
					.tilt(0)                   // Sets the tilt of the camera to 0 degree
					.build(); 
		}                 
		mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
	}

	private void displaySatelliteMap(boolean enabled){
		if(enabled){
			mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE); // Satellite maps with no labels
		}else{
			mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL); // Basic maps
		}                 
	}

	// This method is triggered when the location is updated.
	public void onLocationChanged(Location location) {
		// We're sending the update to a handler which then 
		// updates the UI with the new location.
		ownPoint = new LatLng(location.getLatitude(),location.getLongitude());
		// Move the camera instantly to your current location with a zoom of 17.
		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ownPoint, 17));
		showSelfStatus();
	}

	@Override
	public void onMapClick(LatLng arg0) {
		Log.d("MainActivity", "onMapClick");
		if (hasFragmentStack()) {
			getFragmentManager().popBackStack();
			getFragmentManager().executePendingTransactions();
		}
		isShowList= false;
	}

	public boolean hasFragmentStack() {
		if (getFragmentManager().findFragmentByTag(TAG_POST_FRAGMENT) != null) {
			Log.d("MainActivity", "Post fragment found");
			return true;
		}
		return false;
	}

	@Override
	public boolean onMarkerClick(Marker m) {
		if(m.equals(myMarker)||m.equals(prevMarker)){
			Log.d("MainActivity", "onMarkerClick my marker clicked");
			if (hasFragmentStack()) {
				getFragmentManager().popBackStack();
				getFragmentManager().executePendingTransactions();
			}

			postFragment = new PostFragment();
			sList = twitter.getHomeTimeline();
			postFragment.setTwitterLatlng(twitter, new double[]{ownPoint.latitude, ownPoint.longitude}, sList, m);
			isShowList= true;
			Log.d("myMarker",ownPoint.latitude+" "+ownPoint.longitude);

			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.add(R.id.container, postFragment, TAG_POST_FRAGMENT);
			ft.addToBackStack(null);
			ft.commit();
			getFragmentManager().executePendingTransactions();
		}else{
			for(int i=0;i<friendMarker.size();i++){
				Marker friend = (Marker)friendMarker.get(i);
				if(m.equals(friend)){
					Log.d("MainActivity", "onMarkerClick friend marker clicked");

					if (hasFragmentStack()) {
						getFragmentManager().popBackStack();
						getFragmentManager().executePendingTransactions();
					}
					postFragment = new PostFragment();
					mList = twitter.getDirectMessages();
					postFragment.setTwitterFriend(twitter, friend.getTitle(), mList, m);
					isShowList= false;
					FragmentTransaction ft = getFragmentManager().beginTransaction();
					ft.add(R.id.container, postFragment, TAG_POST_FRAGMENT);
					ft.addToBackStack(null);
					ft.commit();
					getFragmentManager().executePendingTransactions();
					break;
				}
			}
		}
		return false;
	}

	public void updateLayers(){
		mMap.clear();
		if(locateCB.isChecked())
			showSelfStatus();

		if(previousCB.isChecked())
			showPrevSelfStatus();

		if(friendCB.isChecked())
			new GetFriendsStatusTask().execute();
	}

	public void removeFriendMarker(){
		for(int i=0;i<friendMarker.size();i++){
			Marker temp =(Marker) friendMarker.get(i);
			temp.remove();
		}
		friendMarker.clear();
	}

	// Asynchronously get the status from twitter
	class GetFriendsStatusTask extends AsyncTask<Void, Void, String> {
		List<MarkerOptions> fMOList= new LinkedList<MarkerOptions>();

		@SuppressWarnings("deprecation")
		@Override
		protected String doInBackground(Void... params) {
			runOnUiThread(new Runnable() {public void run() {
				try{
					List<User> friendList = new LinkedList<User>();
					friendMarker= new LinkedList<Marker>();
					friendList = twitter.getFriends();
					for(int i=0; i<friendList.size();i++){
						User u = friendList.get(i);				
						winterwell.jtwitter.Status s = u.getStatus();	
						double[] latlng = new double[2];
						if(s!=null){
							if(s.getLatLng()!=null){
								latlng= s.getLatLng();	
								MarkerOptions temp = new MarkerOptions().position(new LatLng(latlng[0],latlng[1]))
										.title(u.getScreenName())
										.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

								fMOList.add(temp);
							}
						}
					}
					result = "Success to retrieve the friends' status";				 
				}catch(Exception e){
					result = "Fail to retrieve friends' status";
				}
			}});
			return result;
		}

		protected void onPostExecute(String result) {
			runOnUiThread(new Runnable() {public void run() {
				// Clear the friend marker
				removeFriendMarker();
				for(int i=0;i<fMOList.size();i++){
					Marker temp = mMap.addMarker((MarkerOptions)fMOList.get(i));
					friendMarker.add(temp);
				}
			}});		
		}
	}

	// A thread checks the updates for every minute
	class Updater extends TimerTask{
		@Override
		public void run(){
			// Updating the information
			runOnUiThread(new Runnable() {public void run() {
				//updateLayers();

				List<Message> mList = twitter.getDirectMessages();
				if (mList.size() > 0) {
					Message curMsg = mList.get(0);
					if(lastMsg!=null)
						if (curMsg.getCreatedAt().compareTo(lastMsg.getCreatedAt()) > 0) {
							Toast.makeText(MainActivity.this, "New message from "+curMsg.getUser(), Toast.LENGTH_LONG).show();
							lastMsg = curMsg;
						}
				}

				Log.d("MainActivity", "Check message status...");
			}});
		}
	}
}
