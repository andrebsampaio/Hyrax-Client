package edu.thesis.fct.client;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context context;
    List<Object> data = new ArrayList<Object>();
    String URL;
    boolean isRegistration;
    SparseBooleanArray selected;

    public GalleryAdapter(Context context, String URL, List<File> images, boolean isRegistration) {
        this.context = context;
        this.URL = URL;
        selected = new SparseBooleanArray();
        this.isRegistration = isRegistration;
        if (this.URL == null){
            data.addAll(images);
        }
    }

    public void setData(List<Object> data){
        this.data = data;
        this.notifyDataSetChanged();
    }

    public void addData(Object data){
        this.data.add(data);
        this.notifyDataSetChanged();
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
            Glide.with(context).load(URL + ((ImageModel) data.get(position)).getId())
                    .thumbnail(0.5f)
                    .placeholder(R.drawable.ic_image_photo_camera)
                    .diskCacheStrategy( DiskCacheStrategy.NONE )
                    .skipMemoryCache(true)
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

    public class MyItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        ImageView mImg;

        public MyItemHolder(View itemView) {
            super(itemView);

            mImg = (ImageView) itemView.findViewById(R.id.item_img);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (URL == null && isRegistration){
                if (selected.get(getLayoutPosition(), false)) {
                    selected.delete(getLayoutPosition());
                    v.findViewById(R.id.selectedIndicator).setVisibility(View.INVISIBLE);
                }
                else {
                    selected.put(getLayoutPosition(), true);
                    v.findViewById(R.id.selectedIndicator).setVisibility(View.VISIBLE);
                }
            } else {
                File i = (File) data.get(getLayoutPosition());
                Intent intent = new Intent(context, ImageDetailActivity.class);
                intent.putExtra("image", i.getAbsolutePath());
                context.startActivity(intent);
            }

        }
    }
}

