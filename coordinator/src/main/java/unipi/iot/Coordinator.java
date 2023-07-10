package unipi.iot;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.gson.Gson;

import unipi.iot.sensors.TempManager;
import unipi.iot.skeleton.SensManager;

public class Coordinator extends CoapServer implements MqttCallback {

    private static final int nSubzones = 1;

    private static final String BROKER = "tcp://[::1]:1883";
    private static final String CLIENT_ID = "SmartDatacenter";

    String[] topics = {"temperature", "consumed_power"};

    private static final Gson parser = new Gson();

    private MqttClient mqttClient = null;

    private TempManager tManager = null;

    // TODO private ConsPowerManager cpManager = null;

    private class TempMessage {
        int subzone;
        int temperature;
    }
    private class CpMessage {
        int subzone;
        int consumed_power;
    }

    @Override
    public void connectionLost(Throwable cause) {
        cause.printStackTrace();
        System.out.println(cause.getMessage());
        System.out.println("CONNECTION LOST");
        System.exit(-1);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // TODO Auto-generated method stub
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {

        if(topic.equals("temperature")){

            TempMessage msg = parser.fromJson(new String(message.getPayload()), TempMessage.class);
    
            String temp = Integer.toString(msg.temperature);
            
            System.out.println("Received message from area " + msg.subzone + 
                                "-> Temperature: " + temp.substring(0, (int)temp.length()/2) + "." + temp.substring((int)temp.length()/2) + "°C"
                                );

            tManager.handle(msg.subzone, msg.temperature, mqttClient);

        } else if(topic.equals("consumed_power")) {

            CpMessage msg = parser.fromJson(new String(message.getPayload()), CpMessage.class);

            System.out.println("Received message from area " + msg.subzone + 
                                "->Consumed Power: " + msg.consumed_power / 1000 + "kW"
                                );

            // TODO cpManager.handle(msg.subzone, msg.consumed_power, mqttClient);
        } else {
            return;
        }

    }

    public Coordinator(){

        tManager = new TempManager(nSubzones);

        // TODO cpManager = new ConsPowerManager(nSubzones);

        do {
            try {
                mqttClient = new MqttClient(BROKER, CLIENT_ID);
                System.out.println("Connecting to the broker: " + BROKER);

                mqttClient.setCallback( this );
                mqttClient.connect();

                for(String topic : topics) {
                    mqttClient.subscribe(topic);
                    System.out.println("Subscribed to: " + topic);
                }
                
            }
            catch(MqttException me)
            {
                System.out.println("I could not connect, Retrying ...");
            }
        } while(!mqttClient.isConnected());
        
    }
    
    public SensManager getSensorManager(String topic) {
        if(topic.equals(topics[0]))// Temperature
            return tManager;
        //TODO if(topic.equals(topics[1])) // Consumed Power
        //  return cpManager;
        return null;
    }
}

