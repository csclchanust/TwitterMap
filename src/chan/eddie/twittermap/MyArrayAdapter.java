package chan.eddie.twittermap;

import java.util.List;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import winterwell.jtwitter.Message;
import winterwell.jtwitter.Status;

public class MyArrayAdapter<T> extends ArrayAdapter<T> {
	
	LayoutInflater mInflater;
	
	public MyArrayAdapter(Context context, int resource, 
			int textViewResourceId, List<T> objects) {		
		super(context, resource, textViewResourceId, objects);
        mInflater = LayoutInflater.from(context);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public View getView(int pos, View reUse, ViewGroup parent) {
		String t;
		ViewHolder holder;
		if(reUse == null) {
			reUse = mInflater.inflate(android.R.layout.two_line_list_item, null);
			holder = new ViewHolder();
			holder.text1 = (TextView) reUse.findViewById(android.R.id.text1);
			holder.text2 = (TextView) reUse.findViewById(android.R.id.text2);
			reUse.setTag(holder);
		} else {
			holder = (ViewHolder) reUse.getTag();
		}
		
		T us = this.getItem(pos);
		if(us instanceof Status){
			t = ((Status) us).getCreatedAt().toString();
			holder.text1.setText(((Status) us).getUser().toString());
			holder.text2.setText(((Status) us).getText() + " @" + t);
		}else if(us instanceof Message){
			t = ((Message) us).getCreatedAt().toString();
			holder.text1.setText(((Message) us).getUser().toString());
			holder.text2.setText(((Message) us).getText() + " @" + t);
		}
		return reUse;
	}

    private class ViewHolder {
        TextView text1;
        TextView text2;
    }
}
