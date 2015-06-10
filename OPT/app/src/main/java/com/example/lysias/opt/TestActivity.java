package com.example.lysias.opt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
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
    ListView lv;

    class ValuesGetter implements Runnable {
        private static final int NB_VALUES = 20;
        private BluetoothSocket socket;
        public int alcoholValue;
        public int soundValue;

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
                alcoholValue = Integer.parseInt(parts[0].replaceAll("\\s", ""));
                soundValue = Integer.parseInt(parts[1].replaceAll("\\s", ""));
            }
            catch(Exception e) {
                return;
            }

            System.out.print("alcohol = " + alcoholValue);
            System.out.println("; sound = " + soundValue);

            runOnUiThread(new Runnable() {
                public void run() {
                    TextView twAlcohol = (TextView) findViewById(R.id.textViewAlcohol);
                    twAlcohol.setText(Integer.toString(alcoholValue));
                    TextView twSound = (TextView) findViewById(R.id.textViewSound);
                    twSound.setText(Integer.toString(soundValue));
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
            lv = (ListView) findViewById(R.id.listView);

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
                new Thread(new ValuesGetter(this.socket)).start();
            }

            final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
            lv.setAdapter(adapter);

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

    public void sendMessage(View view) {
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        String message = "Ici en direct d'un gros penis";
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    public void updateValues() {
        Random generator = new Random();
        test[TEST_LENGTH - 1] = generator.nextInt(100);
    }

    public void Quit(View view) {
        System.exit(0);
    }
}
