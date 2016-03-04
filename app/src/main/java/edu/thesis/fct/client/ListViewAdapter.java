package edu.thesis.fct.client;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;


/**
 * Created by abs on 02-03-2016.
 */
public class ListViewAdapter extends ArrayAdapter<LogItem> {

    Context context;
    int resource;
    LogItem [] objects;

    public ListViewAdapter(Context context, int resource, LogItem [] objects) {
        super(context, resource, objects);
        this.resource = resource;
        this.objects = objects;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LogHolder holder;

        if(convertView == null)
        {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.list_item_layout, parent,false);

            holder = new LogHolder();
            holder.thumbnail = (ImageView)convertView.findViewById(R.id.thumbnail);
            holder.foundImages = (TextView)convertView.findViewById(R.id.photos_found);
            holder.peersHit = (TextView) convertView.findViewById(R.id.hits);
            holder.location = (TextView) convertView.findViewById(R.id.location);

            convertView.setTag(holder);
        }
        else
        {
            holder = (LogHolder)convertView.getTag();
        }

        LogItem log = objects[position];
        holder.foundImages.setText(Integer.toString(log.getPhotosFound()));
        holder.peersHit.setText(Integer.toString(log.getPeersHit()));
        holder.location.setText(log.getLocation());
        holder.thumbnail.setImageResource(R.drawable.image1);

        return convertView;
    }

    static class LogHolder
    {
        ImageView thumbnail;
        TextView foundImages;
        TextView peersHit;
        TextView location;
    }
}
