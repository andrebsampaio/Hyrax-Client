package edu.thesis.fct.client;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class GridViewAdapter extends BaseAdapter {
    private List<Item> items = new ArrayList<Item>();
    private LayoutInflater inflater;

    public GridViewAdapter(Activity context)
    {
        inflater = LayoutInflater.from(context);
        items.add(new Item("http://10.22.107.60:8080/hyrax-server/rest/images/potato1457456814756"));
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int i)
    {
        return items.get(i);
    }

    @Override
    public long getItemId(int i)
    {
        return items.get(i).id;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup)
    {
        View v = view;
        WebImageView picture;

        if(v == null)
        {
            v = inflater.inflate(R.layout.grid_item_layout, viewGroup, false);
            v.setTag(R.id.picture, v.findViewById(R.id.picture));
        }

        picture = (WebImageView)v.getTag(R.id.picture);

        Item item = (Item)getItem(i);

        picture.setPlaceholderImage(android.R.drawable.gallery_thumb);
        picture.setImageUrl(item.url);


        return v;
    }

    private class Item
    {
        final String url;
        final long id;

        Item(String url)
        {
            this.url = url;
            this.id = hash(url);
        }
    }

    public static int hash(String s) {
        int h = 0;
        for (int i = 0; i < s.length(); i++) {
            h = 31 * h + s.charAt(i);
        }
        return h;
    }
}
