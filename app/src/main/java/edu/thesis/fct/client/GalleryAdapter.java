package edu.thesis.fct.client;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context context;
    List<Object> data = new ArrayList<Object>();
    String URL;
    SparseBooleanArray selected;
    SparseBooleanArray downloaded;
    boolean takenTime = false;
    boolean done = false;
    InstrumentationUtils iu;

    public GalleryAdapter(Context context, String URL, List<File> images, InstrumentationUtils iu) {
        this.context = context;
        this.iu = iu;
        this.URL = URL;
        selected = new SparseBooleanArray();
        downloaded = new SparseBooleanArray();
        if (this.URL == null){
            data.addAll(images);
        }
    }

    public void setData(List<Object> data){
        this.data = data;
        this.notifyDataSetChanged();
    }

    public void setIU(InstrumentationUtils iu){
        this.iu = iu;
        done = false;
        takenTime = false;
        downloaded = new SparseBooleanArray();
    }

    public List<File> getSelectedImages (){
        List<File> aux = new ArrayList<>();
        for (int i = 0; i < data.size(); i++){
            if (selected.get(i)){
                aux.add((File)data.get(i));
            }
        }
        return aux;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        final RecyclerView.ViewHolder viewHolder;
        View v;
        v = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.grid_item_layout, parent, false);
        viewHolder = new MyItemHolder(v);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {

        if (URL != null){
            if (!takenTime){
                iu.registerLatency(InstrumentationUtils.DOWNLOAD_RQ);
                takenTime = true;
            }
            Glide.with(context).load(URL + ((ImageModel) data.get(position)).getId())
                    .thumbnail(0.5f)
                    .placeholder(R.drawable.ic_image_photo_camera)
                    .diskCacheStrategy( DiskCacheStrategy.NONE )
                    .skipMemoryCache(true)
                    .into(new GlideDrawableImageViewTarget(((MyItemHolder) holder).mImg) {
                        @Override public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> animation) {
                            // here it's similar to RequestListener, but with less information (e.g. no model available)
                            super.onResourceReady(resource, animation);
                            System.out.println(downloaded.size());
                            downloaded.put(position, true);
                            if (downloaded.size() == data.size() && !done){
                                iu.calculateLatency(InstrumentationUtils.DOWNLOAD_RQ);
                                iu.endTest();
                                done = true;
                            }
                        }
                    });

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

    public class MyItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        ImageView mImg;

        public MyItemHolder(View itemView) {
            super(itemView);

            mImg = (ImageView) itemView.findViewById(R.id.item_img);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (URL == null){
                if (selected.get(getLayoutPosition(), false)) {
                    selected.delete(getLayoutPosition());
                    v.findViewById(R.id.selectedIndicator).setVisibility(View.INVISIBLE);
                }
                else {
                    selected.put(getLayoutPosition(), true);
                    v.findViewById(R.id.selectedIndicator).setVisibility(View.VISIBLE);
                }
            } else {
                ImageModel i = (ImageModel) data.get(getLayoutPosition());
                String link = URL + i.getId();
                Intent intent = new Intent(context, ImageDetailActivity.class);
                intent.putExtra("image", link);
                intent.putExtra("location", i.getLocation());
                intent.putExtra("time", i.getTime());
                context.startActivity(intent);
            }

        }
    }
}

