package unipi.iot;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.gson.Gson;

import unipi.iot.sensors.ConsPowerManager;
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

    private ConsPowerManager cpManager = null;

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

            cpManager.handle(msg.subzone, msg.consumed_power, mqttClient);
        } else {
            return;
        }

    }

    public Coordinator(){

        tManager = new TempManager(nSubzones);

        cpManager = new ConsPowerManager(nSubzones);

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
        if(topic.equals(topics[1])) // Consumed Power
            return cpManager;
        return null;
    }

    public void overview(){
        
        System.out.println("+---------------+-----------------------+-----------------------+");
        System.out.println("|Subzone\t|Temperature\t\t|Consumed Power\t\t|");
        System.out.println("+---------------+-----------------------+-----------------------+");
        
        for (int i = 1; i <= nSubzones; i++) {
            
            System.out.println("|" + i + "\t\t|" + seeTemp(tManager.lastValues.get(i).temperature) + "\t\t|"+ cpManager.lastValues.get(i).consumed_power / 1000 +"kW\t\t\t|");
            System.out.println("|\t\t| [" + seeTemp(tManager.boundsList.get(i).lowBound) + "] [" + seeTemp(tManager.boundsList.get(i).highBound) + "]\t| [" + cpManager.boundsList.get(i).lowBound / 1000 + "kW] [" + cpManager.boundsList.get(i).highBound / 1000 + "kW]\t|");
            System.out.println("+---------------+-----------------------+-----------------------+");
        }
    }

    public String seeTemp(int temp){
        String stemp = Integer.toString(temp);
        // System.out.print(stemp.substring(0, (int)stemp.length()/2+1) + "." + stemp.substring((int)stemp.length()/2+1) + "°C");
        return stemp.substring(0, (int)stemp.length()/2) + "." + stemp.substring((int)stemp.length()/2) + "°C";
    }
}

