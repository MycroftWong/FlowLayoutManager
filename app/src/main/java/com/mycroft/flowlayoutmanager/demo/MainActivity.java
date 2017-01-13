package com.mycroft.flowlayoutmanager.demo;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mycroft.flowlayoutmanager.FlowLayoutManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new FlowLayoutManager());
//        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mRecyclerView.setAdapter(new Adapter());
    }

    private final List<String> mData = new ArrayList<>();

    public static final Random RANDOM = new Random(500);

    {
        for (int i = 0; i < 200; i++) {
            mData.add("item: " + (int) (RANDOM.nextDouble() * Math.pow(10, RANDOM.nextInt(5))));
        }

    }

    public void changeData(View view) {
        final RecyclerView.Adapter adapter = mRecyclerView.getAdapter();

/**/
        mData.add(5, "new item: " + RANDOM.nextInt(99999));
        adapter.notifyItemInserted(5);
        /*

        mData.remove(10);
        adapter.notifyItemRemoved(10);

        mData.set(15, "change item: " + RANDOM.nextDouble());
        adapter.notifyItemChanged(15);
        mData.clear();
        for (int i = 0; i < 150; i++) {
            mData.add("new item: " + i);
        }
        mRecyclerView.getAdapter().notifyDataSetChanged();
*/
    }

    final class Adapter extends RecyclerView.Adapter<VH> {
        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VH(getLayoutInflater().inflate(R.layout.item_tag, parent, false));
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
            params.width = RecyclerView.LayoutParams.WRAP_CONTENT;
            params.height = 80 + 20 * ((position + 5) % 7);

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
