package unipi.iot.sensors;

import java.util.HashMap;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

import unipi.iot.actuators.PowerModule;
import unipi.iot.skeleton.SensManager;

public class ConsPowerManager implements SensManager {

    PowerModule coapManager;

    public static class CPowerData {
        public int consumed_power; 
        public long timestamp;
        public CPowerData(int cd){
            consumed_power = cd;
            timestamp = System.currentTimeMillis();
        }
    }

    HashMap<Integer, CPowerData> lastValues = new HashMap<>();

    // Class that holds the bounds in which the consumed current should stay
    public static class Bounds{
        int lowBound;
        int highBound;
        public Bounds(){
            lowBound = 40000;
            highBound = 100000;
        }
        public Bounds(int low, int high){
            lowBound = low;
            highBound = high;
        }
    }
    HashMap<Integer, Bounds> boundsList = new HashMap<>();

    public ConsPowerManager(int nSubzones){
        //I initialize the tables containing the bounds
        for (int i = 0; i < nSubzones; i++) {
            boundsList.put(i, new Bounds());
        }

        coapManager = new PowerModule(nSubzones);
    }

    @Override
    public void get(int[] subzones) {
        if(subzones == null){
            System.out.println("No subzone selected!");
            return;
        }
            
        for (int i : subzones) {
            // Area 1 -> Consumed Power: 49.365kW
            System.out.println("Area:" +  i + "-> Consumed Power: " + lastValues.get(i).consumed_power / 1000 + "kW");
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

    // To see if the consumed power is below or above the bounds. Returns the message to send to the actuator 
    @Override
    public String check(int subzone, int cp) { 
        Bounds borders = boundsList.get(subzone);

        if(cp <= borders.lowBound)
            return "POFF";
        else if(cp >= borders.highBound)
            return "OL";
        else if(lastValues.get(subzone).consumed_power >= borders.highBound)
            return "PON";
        return null;
    }

    public void handle(int subzone, int cp, MqttClient mqttClient){
        // TODO Create a function that handles the consumed power data (db insert and sensor message shit)
        
        // Override the value of the cperature
        lastValues.put(subzone, new CPowerData(cp));

        String mes = check(subzone, cp);

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
