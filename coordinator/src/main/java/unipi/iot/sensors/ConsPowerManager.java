package unipi.iot.sensors;

import java.util.HashMap;

import unipi.iot.DBManager;
import unipi.iot.actuators.PowerModule;
import unipi.iot.skeleton.SensManager;

public class ConsPowerManager implements SensManager {

    public PowerModule coapManager;
    static Boolean overload = false;

    public static class CPowerData {
        public int consumed_power; 
        public long timestamp;
        public CPowerData(int cd){
            consumed_power = cd;
            timestamp = System.currentTimeMillis();
        }
        public CPowerData(){
            consumed_power = 10000;
            timestamp = 0;
        }
    }

    public HashMap<Integer, CPowerData> lastValues = new HashMap<>();

    // Class that holds the bounds in which the consumed current should stay
    public static class Bounds{
        public int lowBound;
        public int highBound;
        public Bounds(){
            lowBound = 40000;
            highBound = 100000;
        }
        public Bounds(int low, int high){
            lowBound = low;
            highBound = high;
        }
    }
    public HashMap<Integer, Bounds> boundsList = new HashMap<>();

    public ConsPowerManager(int nSubzones){
        //I initialize the tables containing the bounds
        for (int i = 1; i <= nSubzones; i++) {
            boundsList.put(i, new Bounds());
            lastValues.put(i, new CPowerData());
        }

        coapManager = new PowerModule(nSubzones);
    }

    @Override
    public void get(int[] subzones) {
        if(subzones == null){
            lastValues.forEach((key, value) -> {
                System.out.println("Area: " + key + "-> Consumed Power: " + value.consumed_power / 1000 + "kW | Bounds: [" + boundsList.get(key).lowBound / 1000 + "kW] [" + boundsList.get(key).highBound / 1000 + "kW]");
            });
            return;
        }
            
        for (int i : subzones) {
            // Area 1 -> Consumed Power: 49.365kW
            System.out.println("Area: " + i + "-> Consumed Power: " + lastValues.get(i).consumed_power / 1000 + "kW");
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

    // To see if the consumed power is below or above the bounds. Returns the message to send to the actuator 
    @Override
    public String check(int subzone, int cp) { 
        Bounds borders = boundsList.get(subzone);

        // If i'm in overload and the consumed power is still greater than the 3/4 than the high bound then i'm still in overload
        if(overload && cp >= borders.highBound * 0.75) 
            return null;
        // If i'm in overload and the consumed power is still greater than the 3/4 than the high bound then i'm not in overload anymore
        else if(overload){
            overload = false;
            return "PON";
        }
        // If the power is less than the lower bound then i'm in underuse phase
        else if(cp <= borders.lowBound)
            return "POFF";
        // If the power is more than the upper bound then i'm in overload phase
        else if(cp >= borders.highBound){
            overload = true;
            return "OL";
        }
        return null;
    }

    public String handle(int subzone, int cp){
        
        // Override the value of the cperature
        lastValues.put(subzone, new CPowerData(cp));

        DBManager.getInstance().insertCPSample(subzone, cp);
        
        String mes = check(subzone, cp);

        return mes;

    }

}
