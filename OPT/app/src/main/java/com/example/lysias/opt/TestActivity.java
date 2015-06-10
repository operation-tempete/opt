package com.example.lysias.opt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.androidplot.Plot;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.Set;
import java.util.UUID;


public class TestActivity extends ActionBarActivity {
    public final static String EXTRA_MESSAGE = "com.mycompany.testfirstapp.MESSAGE";
    public static final int TEST_LENGTH = 20;
    public static Number[] test = new Number[TEST_LENGTH];
    private Set<BluetoothDevice> pairedDevices;
    private BluetoothSocket socket;
    private static final int NB_VALUES = 2;
    private boolean activateBuzzer = true;
    private boolean hasBeenConstructed = false;
    private ValuesGetter getter;
    public int alcohol_index = 0;
    public int sound_index = 0;

    class ValuesGetter implements Runnable {
        private static final int NB_VALUES = 20;
        private BluetoothSocket socket;
        private static final int SAMPLE_SIZE = 20;
        public int[] alcoholValues = new int[SAMPLE_SIZE];
        public int[] soundValues = new int[SAMPLE_SIZE];
        int index = 0;

        public ValuesGetter(BluetoothSocket socket) {
            this.socket = socket;
        }

        public String readString(BluetoothSocket socket) {
            InputStream inputStream = null;
            try {
                inputStream = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Reader reader = new InputStreamReader(inputStream, Charset.forName("US-ASCII"));
            StringBuilder builder = new StringBuilder();

            try {
                char r;
                while((r = (char) reader.read()) != '\n')
                    builder.append(r);
            } catch (IOException e) {
                return null;
            }

            String received = builder.toString();
            return received;
        }

        public void getValues(String received) {
            // extract sound and alcohol values
            if (!received.contains("-"))
                return;

            String[] parts = received.split("-");
            int sound = 0;
            int alcohol = 0;

            try {
                alcoholValues[index] = Integer.parseInt(parts[0].replaceAll("\\s", ""));
                soundValues[index] = Integer.parseInt(parts[1].replaceAll("\\s", ""));
            }
            catch(Exception e) {
                return;
            }

            System.out.print("alcohol = " + alcoholValues[index]);
            System.out.println("; sound = " + soundValues[index]);

            runOnUiThread(new Runnable() {
                public void run() {
                    TextView twAlcohol = (TextView) findViewById(R.id.textViewAlcohol);
                    twAlcohol.setText(Integer.toString(alcoholValues[index]));
                    TextView twSound = (TextView) findViewById(R.id.textViewSound);
                    twSound.setText(Integer.toString(soundValues[index]));

                    index++;
                    if (index == SAMPLE_SIZE)
                        index = 0;
                }
            });
        }

        @Override
        public void run() {
            while(true) {
                String received = readString(socket);
                if (received != null)
                    getValues(received);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        if (!hasBeenConstructed) {
            BluetoothAdapter BA = BluetoothAdapter.getDefaultAdapter();

            if (!BA.isEnabled()) {
                Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnOn, 0);
            }

            pairedDevices = BA.getBondedDevices();
            ArrayList list = new ArrayList();

            for (BluetoothDevice bt : pairedDevices) {
                list.add(bt.getName());

                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
                try {
                    this.socket = bt.createRfcommSocketToServiceRecord(uuid);
                    this.socket.connect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //new Thread(new ValuesGetter(this.socket)).start();
                getter = new ValuesGetter(this.socket);
                new Thread(getter).start();
            }

            createGraph(savedInstanceState);

            hasBeenConstructed = true;
        }
    }

    public void toggleBuzzer(View view) {
        try {
            OutputStream outputStream = this.socket.getOutputStream();
            char c = activateBuzzer ? '1' : '0';
            activateBuzzer = !activateBuzzer;
            outputStream.write(new byte[] {(byte) c});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private int getSoundAndAlcoholValues(String received) {
        return 0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_test, menu);
        return true;
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

    public void Quit(View view) {
        System.exit(0);
    }

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

    public void createGraph(Bundle savedInstanceState) {

        // android boilerplate stuff
        //super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_display_message);

        // get handles to our View defined in layout.xml:
        dynamicPlot = (XYPlot) findViewById(R.id.dynamicXYPlot);

        plotUpdater = new MyPlotUpdater(dynamicPlot);

        // only display whole numbers in domain labels
        dynamicPlot.getGraphWidget().setDomainValueFormat(new DecimalFormat("0"));

        // getInstance and position datasets:
        data = new SampleDynamicXYDatasource();
        SampleDynamicSeries sine1Series = new SampleDynamicSeries(data, 0, "Alcohol");
        SampleDynamicSeries sine2Series = new SampleDynamicSeries(data, 1, "Sound");

        LineAndPointFormatter formatter1 = new LineAndPointFormatter(
                Color.rgb(3, 191, 61), null, null, null);
        formatter1.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
        formatter1.getLinePaint().setStrokeWidth(10);
        dynamicPlot.addSeries(sine1Series, formatter1);

        LineAndPointFormatter formatter2 =
                new LineAndPointFormatter(Color.rgb(48, 140, 232), null, null, null);
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
        dynamicPlot.setRangeStepValue(50);

        dynamicPlot.setRangeValueFormat(new DecimalFormat("###.#"));

        // uncomment this line to freeze the range boundaries:
        dynamicPlot.setRangeBoundaries(0, 700, BoundaryMode.FIXED);

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

        public static final int SINE1 = 0;
        public static final int SINE2 = 1;
        private static final int SAMPLE_SIZE = 20;
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
            if (index >= SAMPLE_SIZE)
                throw new IllegalArgumentException();

            if (series == 0) {
                if (alcohol_index == SAMPLE_SIZE)
                    alcohol_index = 0;
                return getter.alcoholValues[alcohol_index++];
            }
            if (series == 1) {
                if (sound_index == SAMPLE_SIZE)
                    sound_index = 0;
                return getter.soundValues[sound_index++];
            }
            return 0;
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
