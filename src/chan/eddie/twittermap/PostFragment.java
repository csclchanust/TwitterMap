package chan.eddie.twittermap;
import java.util.List;

import com.google.android.gms.maps.model.Marker;
import winterwell.jtwitter.Message;
import winterwell.jtwitter.Status;
import winterwell.jtwitter.Twitter;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PostFragment extends Fragment implements OnClickListener {
	Twitter twitter;
	double[] latlng = null;
	String friend;
	Button postBtn;
	EditText message;
	String buttonText, headerText;
	TextView txtHeader;
	View view;
	ListView listView;
	List<Status> sList;
	List<Message> mList;
	MyArrayAdapter<Status> sAdapter;
	MyArrayAdapter<Message> mAdapter;
	Marker marker;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// onCreateView will be called when this fragment is being added
    	// by FragmentManager, so no need to re-initiate all the view
    	// component
    	if (view == null) {
			view = inflater.inflate(R.layout.post_fragment, container, false);
			message = (EditText) view.findViewById(R.id.message);
			postBtn = (Button) view.findViewById(R.id.postBtn);
			listView = (ListView) view.findViewById(R.id.listView);
			txtHeader = (TextView) view.findViewById(R.id.listHeader);
	
			setListView();
			txtHeader.setText(headerText);
			postBtn.setText(buttonText);
			postBtn.setOnClickListener(this);
		}
		return view;
	}

	public void setTwitterLatlng(Twitter t, double[] l, List<Status> s, Marker m) {
		twitter = t;
		latlng=l;
		sList = s;
		headerText = "Post Twitter Status";
		buttonText = "Post";
		marker = m;
	}

	public void setTwitterFriend(Twitter t, String f, List<Message> m, Marker mr) {
		twitter = t;
		friend = f;
		mList = m;
		headerText = "Send Message to "+friend;
		buttonText = "Send";
		marker = mr;
	}


	public void setListView(){ 
		if(sList!=null){
			sAdapter = new MyArrayAdapter<Status>(getActivity(), 
					android.R.layout.two_line_list_item, 
					android.R.id.text1, sList);

			listView.setAdapter(sAdapter);
		}
		if(mList!=null){
			mAdapter = new MyArrayAdapter<Message>(getActivity(), 
					android.R.layout.two_line_list_item, 
					android.R.id.text1, mList);

			listView.setAdapter(mAdapter);
		}
	}

	public void updateSList(List<Status> sData) {
		sList.clear();
		sList.addAll(sData);
		// tell the list view to refresh after data changed
		sAdapter.notifyDataSetChanged();
		// move the view to the top of the list
		listView.setAdapter(sAdapter);
		listView.setSelectionAfterHeaderView();
	}
	
	public void updateMList(List<Message> mData) {
		mList.clear();
		mList.addAll(mData);
		// tell the list view to refresh after data changed
		mAdapter.notifyDataSetChanged();
		// move the view to the top of the list
		listView.setAdapter(mAdapter);
		listView.setSelectionAfterHeaderView();
	}
	
	public void updateButton(String t) {
		postBtn.setText(t);
	}

	@Override
	public void onClick(View v) {
		if (v == postBtn) {
			try {
				if (latlng != null) {
					twitter.setMyLocation(latlng);
					twitter.updateStatus(message.getText().toString());
					message.setText("");
					Toast.makeText(getActivity(), "Post Successfully!",
							Toast.LENGTH_SHORT).show();
				} else if (friend != null) {
					twitter.sendMessage(friend, message.getText().toString());
					message.setText("");
					Toast.makeText(getActivity(), "Message sent successfully!",
							Toast.LENGTH_SHORT).show();
				}
			} catch (Exception e) {
				Toast.makeText(getActivity(),
						"Fail to post the message! The error: " + e.toString(),
						Toast.LENGTH_LONG).show();
			}
			marker.hideInfoWindow();
			getFragmentManager().popBackStack();
		}
		
	}
}
