package com.mycroft.flowlayoutmanager.demo;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.mycroft.flowlayoutmanager.CardLayoutManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author mycroft
 */
public class CardActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card);
        mRecyclerView = findViewById(R.id.recycler_view);

        mRecyclerView.setLayoutManager(new CardLayoutManager());

        mRecyclerView.setAdapter(new Adapter());
    }

    private final List<String> mData = new ArrayList<>();

    public static final Random RANDOM = new Random(500);

    {
        for (int i = 0; i < 200; i++) {
            mData.add("item: " + i);
        }
    }

    final class Adapter extends RecyclerView.Adapter<VH> {
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(getLayoutInflater().inflate(R.layout.item_card, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.mTextView.setText(mData.get(position));
            holder.mTextView.setBackgroundColor(Color.rgb(RANDOM.nextInt(0xFF), RANDOM.nextInt(0xFF), RANDOM.nextInt(0xFF)));
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }

    static class VH extends RecyclerView.ViewHolder {

        TextView mTextView;

        public VH(View itemView) {
            super(itemView);
            mTextView = (TextView) itemView;
        }
    }
}
