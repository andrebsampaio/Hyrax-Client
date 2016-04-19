package edu.thesis.fct.client;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context context;
    List<Object> data = new ArrayList<Object>();
    String url;
    List<Boolean> selected =new ArrayList<Boolean>(Arrays.asList(new Boolean[8]));



    public GalleryAdapter(Context context, String url, List<File> images) {
        this.context = context;
        Collections.fill(selected, Boolean.FALSE);
        this.url = url;
        if (this.url == null){
            data.addAll(images);
        }
    }

    public void setData(List<Object> data){
            this.data.clear();
            this.data.addAll(data);
            this.notifyItemRangeInserted(0,this.data.size()-1);
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        final RecyclerView.ViewHolder viewHolder;
        View v;
        v = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.grid_item_layout, parent, false);
        viewHolder = new MyItemHolder(v);

        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int itemPosition = parent.indexOfChild(v);

                if (selected.get(itemPosition)){
                    selected.set(itemPosition,false);
                    v.findViewById(R.id.selectedIndicator).setVisibility(View.INVISIBLE);
                } else {
                    selected.set(itemPosition,true);
                    v.findViewById(R.id.selectedIndicator).setVisibility(View.VISIBLE);
                }

                Log.d("GRID", "selected " + itemPosition);

            }
        });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {


        if (url != null){
            Glide.with(context).load(url + (int)data.get(position))
                    .thumbnail(0.5f)
                    .placeholder(R.drawable.ic_image_photo_camera)
                    .diskCacheStrategy( DiskCacheStrategy.NONE )
                    .skipMemoryCache( true )
                    .into(((MyItemHolder) holder).mImg);

        } else {
            Glide.with(context).load((File)data.get(position))
                    .thumbnail(0.5f)
                    .placeholder(R.drawable.ic_image_photo_camera)
                    .diskCacheStrategy( DiskCacheStrategy.NONE )
                    .skipMemoryCache(true)
                    .into(((MyItemHolder) holder).mImg);
        }

    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class MyItemHolder extends RecyclerView.ViewHolder {
        ImageView mImg;

        public MyItemHolder(View itemView) {
            super(itemView);

            mImg = (ImageView) itemView.findViewById(R.id.item_img);
        }
    }



}

