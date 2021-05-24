package com.example.visitme;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;

public class MainAdapter extends FirebaseRecyclerAdapter<Contact,MainAdapter.myViewHolder> {

    /**
     * Initialize a {@link RecyclerView.Adapter} that listens to a Firebase query. See
     * {@link FirebaseRecyclerOptions} for configuration options.
     *
     * @param options
     */
    public MainAdapter(@NonNull FirebaseRecyclerOptions<Contact> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull MainAdapter.myViewHolder holder, int position, @NonNull  Contact model) {
        holder.category.setText(model.getCategory());
        if(model.getCompany().equals("") )
        holder.fullName.setText(model.getFullName());
        else
            holder.company.setText(model.getCompany());
    }

    @Override
    public myViewHolder onCreateViewHolder(@NonNull  ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.dynamic_rv_item_layout,parent,false);
        return new myViewHolder(view);
    }

    class myViewHolder extends RecyclerView.ViewHolder{
       // ImageView img;
        TextView category,company,fullName;

        public myViewHolder(@NonNull View itemView) {
            super(itemView);

            //img=(ImageView)itemView.findViewById(R.id.person);
            category=(TextView)itemView.findViewById(R.id.category);
            company=(TextView)itemView.findViewById(R.id.details);
            fullName=(TextView)itemView.findViewById(R.id.details);
        }
    }

}
