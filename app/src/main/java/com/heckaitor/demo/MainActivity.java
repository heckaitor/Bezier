package com.heckaitor.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.heckaitor.bezier.BezierView;

import java.util.Random;

public class MainActivity extends AppCompatActivity
        implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    private BezierView mBezierView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBezierView = (BezierView) findViewById(R.id.bv_plot_view);

        ((CheckBox) findViewById(R.id.cb_draw_value)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.cb_draw_control)).setOnCheckedChangeListener(this);
        findViewById(R.id.btn_plot).setOnClickListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        final int id = compoundButton.getId();
        if (R.id.cb_draw_value == id) {
            mBezierView.setDrawValuePoints(checked);
            mBezierView.invalidate();
        } else if (R.id.cb_draw_control == id) {
            mBezierView.setDrawControlPoints(checked);
            mBezierView.invalidate();
        }
    }

    @Override
    public void onClick(View view) {
        final int id = view.getId();
        if (R.id.btn_plot == id) {
            plot();
        }
    }

    private void plot() {
        final int length = 10;
        final float[] values = new float[length];
        final Random random = new Random();
        for (int i = 0; i < length; i++) {
            values[i] = random.nextFloat() * 1000;
        }

        mBezierView.setValues(values);
    }
}
