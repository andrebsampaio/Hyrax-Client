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

import java.util.ArrayList;
import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context context;
    List<Integer> data = new ArrayList<>();
    String url;

    public GalleryAdapter(Context context, String url) {
        NetworkInfoHolder nih = NetworkInfoHolder.getInstance();
        this.context = context;
        data = new ArrayList<>();
        this.url = url;
    }

    public void setData(List<Integer> data){
        this.data.clear();
        this.data.addAll(data);
        this.notifyItemRangeInserted(0,this.data.size()-1);
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder;
        View v;
        v = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.grid_item_layout, parent, false);
        viewHolder = new MyItemHolder(v);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        System.out.println(url + data.get(position));
        Glide.with(context).load(url + data.get(position))
                .thumbnail(0.5f)
                .crossFade()
                .placeholder(R.drawable.ic_image_photo_camera)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(((MyItemHolder) holder).mImg);

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

