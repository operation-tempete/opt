package com.example.lysias.opt;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.androidplot.Plot;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;

import junit.framework.Test;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;


public class DisplayMessageActivity extends ActionBarActivity {

        // redraws a plot whenever an update is received:
        private class MyPlotUpdater implements Observer {
            Plot plot;

            public MyPlotUpdater(Plot plot) {
                this.plot = plot;
            }

            @Override
            public void update(Observable o, Object arg) {
                plot.redraw();
            }
        }

        private XYPlot dynamicPlot;
        private MyPlotUpdater plotUpdater;
        SampleDynamicXYDatasource data;
        private Thread myThread;

        @Override
        public void onCreate(Bundle savedInstanceState) {

            // android boilerplate stuff
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_display_message);

            // get handles to our View defined in layout.xml:
            dynamicPlot = (XYPlot) findViewById(R.id.dynamicXYPlot);

            plotUpdater = new MyPlotUpdater(dynamicPlot);

            // only display whole numbers in domain labels
            dynamicPlot.getGraphWidget().setDomainValueFormat(new DecimalFormat("0"));

            // getInstance and position datasets:
            data = new SampleDynamicXYDatasource();
            SampleDynamicSeries sine1Series = new SampleDynamicSeries(data, 0, "Sine 1");
            SampleDynamicSeries sine2Series = new SampleDynamicSeries(data, 1, "Sine 2");

            LineAndPointFormatter formatter1 = new LineAndPointFormatter(
                    Color.rgb(0, 0, 0), null, null, null);
            formatter1.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
            formatter1.getLinePaint().setStrokeWidth(10);
            dynamicPlot.addSeries(sine1Series,
                    formatter1);

            LineAndPointFormatter formatter2 =
                    new LineAndPointFormatter(Color.rgb(0, 0, 200), null, null, null);
            formatter2.getLinePaint().setStrokeWidth(10);
            formatter2.getLinePaint().setStrokeJoin(Paint.Join.ROUND);

            //formatter2.getFillPaint().setAlpha(220);
            dynamicPlot.addSeries(sine2Series, formatter2);

            // hook up the plotUpdater to the data model:
            data.addObserver(plotUpdater);

            // thin out domain tick labels so they dont overlap each other:
            dynamicPlot.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
            dynamicPlot.setDomainStepValue(5);

            dynamicPlot.setRangeStepMode(XYStepMode.INCREMENT_BY_VAL);
            dynamicPlot.setRangeStepValue(10);

            dynamicPlot.setRangeValueFormat(new DecimalFormat("###.#"));

            // uncomment this line to freeze the range boundaries:
            dynamicPlot.setRangeBoundaries(-100, 100, BoundaryMode.FIXED);

            // create a dash effect for domain and range grid lines:
            DashPathEffect dashFx = new DashPathEffect(
                    new float[] {PixelUtils.dpToPix(3), PixelUtils.dpToPix(3)}, 0);
            dynamicPlot.getGraphWidget().getDomainGridLinePaint().setPathEffect(dashFx);
            dynamicPlot.getGraphWidget().getRangeGridLinePaint().setPathEffect(dashFx);


        }

        @Override
        public void onResume() {
            // kick off the data generating thread:
            myThread = new Thread(data);
            myThread.start();
            super.onResume();
        }

        @Override
        public void onPause() {
            data.stopThread();
            super.onPause();
        }

        class SampleDynamicXYDatasource implements Runnable {

            // encapsulates management of the observers watching this datasource for update events:
            class MyObservable extends Observable {
                @Override
                public void notifyObservers() {
                    setChanged();
                    super.notifyObservers();
                }
            }

            private static final double FREQUENCY = 5; // larger is lower frequency
            private static final int MAX_AMP_SEED = 100;
            private static final int MIN_AMP_SEED = 10;
            private static final int AMP_STEP = 1;
            public static final int SINE1 = 0;
            public static final int SINE2 = 1;
            private static final int SAMPLE_SIZE = 30;
            private int phase = 0;
            private int sinAmp = 1;
            private MyObservable notifier;
            private boolean keepRunning = false;
            private int i = 0;

            {
                notifier = new MyObservable();
            }

            public void stopThread() {
                keepRunning = false;
            }

            //@Override
            public void run() {
                try {
                    keepRunning = true;
                    boolean isRising = true;
                    while (keepRunning) {

                        Thread.sleep(500); // decrease or remove to speed up the refresh rate.
                        phase++;

                        notifier.notifyObservers();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            public int getItemCount(int series) {
                return SAMPLE_SIZE;
            }

            public Number getX(int series, int index) {
                if (index >= SAMPLE_SIZE) {
                    throw new IllegalArgumentException();
                }
                return index;
            }

            public Number getY(int series, int index) {
                if (i == TestActivity.TEST_LENGTH)
                    i = 0;
                return TestActivity.test[i++];
            }

            public void addObserver(Observer observer) {
                notifier.addObserver(observer);
            }

            public void removeObserver(Observer observer) {
                notifier.deleteObserver(observer);
            }

        }

        class SampleDynamicSeries implements XYSeries {
            private SampleDynamicXYDatasource datasource;
            private int seriesIndex;
            private String title;

            public SampleDynamicSeries(SampleDynamicXYDatasource datasource, int seriesIndex, String title) {
                this.datasource = datasource;
                this.seriesIndex = seriesIndex;
                this.title = title;
            }

            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public int size() {
                return datasource.getItemCount(seriesIndex);
            }

            @Override
            public Number getX(int index) {
                return datasource.getX(seriesIndex, index);
            }

            @Override
            public Number getY(int index) {
                return datasource.getY(seriesIndex, index);
            }
        }
    }
/*

    private XYPlot plot;

    // Create a couple arrays of y-values to plot:
    Number[] series1Numbers = {1, 8, 5, 2, 7, 4};
    Number[] series2Numbers = {4, 6, 3, 8, 2, 10};

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // fun little snippet that prevents users from taking screenshots
        // on ICS+ devices :-)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.activity_display_message);

        // initialize our XYPlot reference:
        plot = (XYPlot) findViewById(R.id.mySimpleXYPlot);

        XYSeries series1 = new SimpleXYSeries(
                Arrays.asList(series1Numbers),          // SimpleXYSeries takes a List so turn our array into a List
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // Y_VALS_ONLY means use the element index as the x value
                "Series1");                             // Set the display title of the series

        // same as above
        XYSeries series2 = new SimpleXYSeries(Arrays.asList(series2Numbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Series2");

        // Create a formatter to use for drawing a series using LineAndPointRenderer
        // and configure it from xml:
        LineAndPointFormatter series1Format = new LineAndPointFormatter();
        series1Format.setPointLabelFormatter(new PointLabelFormatter());
        series1Format.configure(getApplicationContext(),
                R.xml.line_point_formatter_with_plf1);

        // add a new series' to the xyplot:
        plot.addSeries(series1, series1Format);

        // same as above:
        LineAndPointFormatter series2Format = new LineAndPointFormatter();
        series2Format.setPointLabelFormatter(new PointLabelFormatter());
        series2Format.configure(getApplicationContext(),
                R.xml.line_point_formatter_with_plf2);
        plot.addSeries(series2, series2Format);

        // reduce the number of range labels
        plot.setTicksPerRangeLabel(3);
        plot.getGraphWidget().setDomainLabelOrientation(-45);
    }

    public void updateGraph(View view) {
        XYSeries series1 = new SimpleXYSeries(
                Arrays.asList(series1Numbers),          // SimpleXYSeries takes a List so turn our array into a List
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // Y_VALS_ONLY means use the element index as the x value
                "Series1");                             // Set the display title of the series

        // same as above
        XYSeries series2 = new SimpleXYSeries(Arrays.asList(series2Numbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Series2");

        // Create a formatter to use for drawing a series using LineAndPointRenderer
        // and configure it from xml:
        LineAndPointFormatter series1Format = new LineAndPointFormatter();
        series1Format.setPointLabelFormatter(new PointLabelFormatter());
        series1Format.configure(getApplicationContext(),
                R.xml.line_point_formatter_with_plf1);

        // add a new series' to the xyplot:
        plot.addSeries(series1, series1Format);

        // same as above:
        LineAndPointFormatter series2Format = new LineAndPointFormatter();
        series2Format.setPointLabelFormatter(new PointLabelFormatter());
        series2Format.configure(getApplicationContext(),
                R.xml.line_point_formatter_with_plf2);
        plot.addSeries(series2, series2Format);

        // reduce the number of range labels
        plot.setTicksPerRangeLabel(3);
        plot.getGraphWidget().setDomainLabelOrientation(-45);

        series1Numbers = new Number[]{8, 8, 8, 8, 7, 4};
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}*/
