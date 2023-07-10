package unipi.iot.sensors;

import java.util.HashMap;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

import unipi.iot.actuators.Watercooling;
import unipi.iot.skeleton.SensManager;

public class TempManager implements SensManager {

    Watercooling coapManager;

    public static class TempData {
        public int temperature; 
        public long timestamp;
        public TempData(int temp){
            temperature = temp;
            timestamp = System.currentTimeMillis();
        }
    }

    HashMap<Integer, TempData> lastValues = new HashMap<>();

    // Class that holds the bounds in which the temperature should stay
    public static class Bounds{
        int lowBound;
        int highBound;
        public Bounds(){
            lowBound = 1800;
            highBound = 2600;
        }
        public Bounds(int low, int high){
            lowBound = low;
            highBound = high;
        }
    }
    HashMap<Integer, Bounds> boundsList = new HashMap<>();

    public TempManager(int nSubzones){
        //I initialize the tables containing the bounds
        for (int i = 0; i < nSubzones; i++) {
            boundsList.put(i, new Bounds());
        }

        coapManager = new Watercooling(nSubzones);
    }

    @Override
    public void get(int[] subzones) {
        if(subzones == null){
            System.out.println("No subzone selected!");
            return;
        }
            
        for (int i : subzones) {
            String temperature = Integer.toString(lastValues.get(i).temperature);

            // Area 1 -> Temperature: 18.00°C
            System.out.println("Area:" +  i + "-> Temperature: " + temperature.substring(0, (int)temperature.length()/2) + "." + temperature.substring((int)temperature.length()/2) + "°C");
        }
    }

    @Override
    public void set(int[] subzones, int down, int up) {

        // If the subzone list is null I have to change every value
        if(subzones == null){
            boundsList.forEach((key, value) -> {
                value.lowBound = down;
                value.highBound = up;
            });
        }
        
        for (int i : subzones) {
            boundsList.put(i, new Bounds(down, up));
        }
    }

    // To see if the temperature is below or above the bounds. Returns the message to send to the actuator 
    @Override
    public String check(int subzone, int temp) { 
        Bounds borders = boundsList.get(subzone);

        if(temp <= borders.lowBound){
            return "WOFF";
        } else if(temp >= borders.highBound){
            return "WON";
        }

        return null;
    }

    public void handle(int subzone, int temp, MqttClient mqttClient){
        // TODO Create a function that handles the temperature data (db insert and sensor message shit)
        
        // Override the value of the temperature
        lastValues.put(subzone, new TempData(temp));

        String mes = check(subzone, temp);

        if(mes != null) {
            try {
                mqttClient.publish("SEN" + subzone, new MqttMessage(mes.getBytes()));
            } catch (MqttPersistenceException e) {
                System.out.println("Could not publish on area " + subzone + "!");
                e.printStackTrace();
            } catch (MqttException e) {
                System.out.println("Could not publish on area " + subzone + "!");
                e.printStackTrace();
            }

            coapManager.sendMessage(subzone, mes);
        }
            


        // TODO insert in the database
    }
    
}
