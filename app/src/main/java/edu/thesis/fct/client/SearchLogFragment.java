package edu.thesis.fct.client;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class SearchLogFragment extends ListFragment {

    Context context;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LogItem [] items = {new LogItem(1,2,3,"potato")};
       ArrayAdapter<LogItem> adapter = new ListViewAdapter(inflater.getContext(),R.layout.list_item_layout, items);
       setListAdapter(adapter);

        return inflater.inflate(R.layout.search_log_layout,container,false);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent myIntent = new Intent(getActivity(), GalleryActivity.class);
        myIntent.putExtra("face", "IMAGE"); //Optional parameters
        getActivity().startActivity(myIntent);
    }
}
