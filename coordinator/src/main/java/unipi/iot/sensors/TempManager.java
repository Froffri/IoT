package unipi.iot.sensors;

import java.util.HashMap;

import unipi.iot.DBManager;

import unipi.iot.actuators.Watercooling;
import unipi.iot.skeleton.SensManager;

public class TempManager implements SensManager {

    public Watercooling coapManager;

    public static class TempData {
        public int temperature; 
        public long timestamp;
        public TempData(int temp){
            this.temperature = temp;
            this.timestamp = System.currentTimeMillis();
        }
        public TempData(){
            this.temperature = 1000;
            this.timestamp = 0;
        }
    }

    public HashMap<Integer, TempData> lastValues = new HashMap<>();

    // Class that holds the bounds in which the temperature should stay
    public static class Bounds{
        public int lowBound;
        public int highBound;
        public Bounds(){
            lowBound = 1800;
            highBound = 2600;
        }
        public Bounds(int low, int high){
            lowBound = low;
            highBound = high;
        }
    }
    public HashMap<Integer, Bounds> boundsList = new HashMap<>();

    public TempManager(int nSubzones){
        //I initialize the tables containing the bounds
        for (int i = 1; i <= nSubzones; i++) {
            boundsList.put(i, new Bounds());
            lastValues.put(i, new TempData());
        }

        coapManager = new Watercooling(nSubzones);
    }

    @Override
    public void get(int[] subzones) {
        if(subzones == null){
            
            lastValues.forEach((key, value) -> {

                // Area 1 -> Temperature: 18.00°C
                System.out.println("Area: " + key + "-> Temperature: " + seeTemp(value.temperature) + " | Bounds: [" + seeTemp(boundsList.get(key).lowBound) + "] [" + seeTemp(boundsList.get(key).highBound) + "]");
            });

            return;
        }
            
        for (int i : subzones) {

            // Area 1 -> Temperature: 18.00°C
            System.out.println("Area: " +  i + "-> Temperature: " + seeTemp(lastValues.get(i).temperature) + " | Bounds: [" + seeTemp(boundsList.get(i).lowBound) + "] [" + seeTemp(boundsList.get(i).highBound) + "]");
        }
    }

    @Override
    public void set(int[] subzones, int down, int up) {

        if(down >= up * 0.75){
            System.out.println("The lower bound must be inferior to the 3/4 of the upper bound!");
            return;
        }

        // If the subzone list is null I have to change every value
        if(subzones == null){
            boundsList.forEach((key, value) -> {
                value.lowBound = down;
                value.highBound = up;
            });
            return;
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

    public String handle(int subzone, int temp){
        
        // Override the value of the temperature
        lastValues.put(subzone, new TempData(temp));

        DBManager.getInstance().insertTempSample(subzone, temp);

        String mes = check(subzone, temp);

        return mes;

    }

    public String seeTemp(int temp){
        String stemp = Integer.toString(temp);
        // System.out.print(stemp.substring(0, (int)stemp.length()/2+1) + "." + stemp.substring((int)stemp.length()/2+1) + "°C");
        return stemp.substring(0, (int)stemp.length()/2) + "." + stemp.substring((int)stemp.length()/2) + "°C";
    }
    
}
