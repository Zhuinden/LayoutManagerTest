package hu.zhu.vga.layoutmanagertest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity
        extends AppCompatActivity {

    @Bind(R.id.root)
    RelativeLayout relativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        RowSpecifierAdapter rowSpecifierAdapter = new RowSpecifierAdapter();
        recyclerView.setAdapter(rowSpecifierAdapter);
        //recyclerView.setLayoutManager(new TestLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        //recyclerView.setLayoutManager(new TestGridLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        //recyclerView.setLayoutManager(new FixedGridLayoutManager()); //FAIL
        //recyclerView.setLayoutManager(new MyStaggeredGridLayoutManager(1440, MyStaggeredGridLayoutManager.HORIZONTAL)); //FAIL
        recyclerView.setLayoutManager(new TestLayoutManager(rowSpecifierAdapter));
        relativeLayout.addView(recyclerView);
    }

    @Override
    protected void onDestroy() {
        ButterKnife.unbind(this);
        super.onDestroy();
    }


}
