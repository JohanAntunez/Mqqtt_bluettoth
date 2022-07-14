package com.example.arduino_led_bluetooth;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;



import android.os.Build;//para obtener el nombre del dispositivo
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    MqttAndroidClient client;
    TextView subText;

    String nombre_Dispositivo;                                //string para obtener el nombre del
    //parametros del broker la siguiente variable con el broker de shiftr.io
    static String MQTTHOST = "postman.cloudmqtt.com"; //el broker
    static String USERNAME = "lhcrtsku";                   //el token de acceso
    static String PASSWORD = "pdB8HRCpfCxN";              //la contraceña del token
    //static String topic = "cerradura/mensaje"; //el tópico


    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    public static String address = null;
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public boolean activar;
    Handler bluetoothIn;
    final int handlerState = 0;
    Button prender, btn, apagar, desconectar;
    private ConnectedThread MyConexionBT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //obtener_nombre_Dispositivo();

        //Se asocian los botones
        subText = (TextView) findViewById(R.id.subText);
        prender = findViewById(R.id.prender);
        btn = findViewById(R.id.btn);
        apagar = findViewById(R.id.Apagar);
        desconectar = findViewById(R.id.Desconectar);

        //Ajustes para la conexión bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        verificarBluetooth();
        Set<BluetoothDevice> pairedDeveicesList = btAdapter.getBondedDevices();
        for (BluetoothDevice pairedDevice : pairedDeveicesList) {
            if (pairedDevice.getName().equals("ESP32_BT")) {

                address = pairedDevice.getAddress();
            }
        }

        //Conecta al bluetooth
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activar = true;
                onResume();
            }
        });
        //Prende el led bluetooth
        prender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyConexionBT.write("a");
            }
        });
        //Apaga el led bluetooth
        apagar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyConexionBT.write("A");
            }
        });
        //Se desconecta del bluetooth
        desconectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    btSocket.close();
                    Toast.makeText(getBaseContext(), "Desconectado exitosamente", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


//-----------------------MQTT------------------------------------------------------------------
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(),"tcp://postman.cloudmqtt.com:10667", clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName("lhcrtsku");
        options.setPassword("pdB8HRCpfCxN".toCharArray());

        try {
            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(MainActivity.this,"connected!!",Toast.LENGTH_LONG).show();
                    setSubscription();

                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(MainActivity.this, "connection failed!!", Toast.LENGTH_LONG).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                subText.setText(new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

    }

    //Topico para abrir la cerradura
    public void abrir(View v){
        String topic = "cerradura/mensaje";
        String message = "ON";
        try {
            client.publish(topic, message.getBytes(),0,false);
            Toast.makeText(this,"Abierta",Toast.LENGTH_SHORT).show();
        } catch ( MqttException e) {
            e.printStackTrace();
        }
    }
    //Topico para cerrar la cerradura
    public void cerrar(View v){
        String topic = "cerradura/mensaje";
        String message = "OFF";
        try {
            client.publish(topic, message.getBytes(),0,false);
            Toast.makeText(this,"Cerrada",Toast.LENGTH_SHORT).show();
        } catch ( MqttException e) {
            e.printStackTrace();
        }
    }

    private void setSubscription(){
        try{
            client.subscribe("event",0);
        }catch (MqttException e){
            e.printStackTrace();
        }
    }

    //Hace la conexión por MQTT
    public void conn(View v){
        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(MainActivity.this,"connected!!",Toast.LENGTH_LONG).show();
                    setSubscription();

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(MainActivity.this,"connection failed!!",Toast.LENGTH_LONG).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    //Se desconecta del MQTT
    public void disconn(View v){

        try {
            IMqttToken token = client.disconnect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(MainActivity.this,"Disconnected!!",Toast.LENGTH_LONG).show();


                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(MainActivity.this,"Could not diconnect!!",Toast.LENGTH_LONG).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }




//--------------BLUETOOTH------------------------------------------------------------------

    private BluetoothSocket createBluetoothSocket (BluetoothDevice device) throws IOException{

        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }




    private void verificarBluetooth(){
        if(btAdapter.isEnabled()){
        }else{
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,1);
        }

    }
    public void onResume() {
        super.onResume();

        if (activar) {
            BluetoothDevice device = btAdapter.getRemoteDevice(address);

            try {
                btSocket = createBluetoothSocket(device);

            } catch (IOException e) {
                Toast.makeText(getBaseContext(), "La creacción del Socket falló", Toast.LENGTH_LONG).show();
            }
            // Establece la conexión con el socket Bluetooth.
            try {
                btSocket.connect();
                Toast.makeText(getBaseContext(), "Conexión exitosa", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                try {
                    btSocket.close();
                } catch (IOException e2) {

                }
            }
            MyConexionBT = new ConnectedThread(btSocket);
            MyConexionBT.start();
        }

    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {

            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {

            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Se mantiene en modo escucha para determinar el ingreso de datos
            while (true) {
                try {

                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    // Envia los datos obtenidos hacia el evento via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        //Envio de trama
        public void write(String input) {
            try {
                mmOutStream.write(input.getBytes());
            } catch (IOException e) {
                //si no es posible enviar datos se cierra la conexión
                Toast.makeText(getBaseContext(), "La Conexión falló", Toast.LENGTH_LONG).show();
                finish();
            }
        }



    }



}