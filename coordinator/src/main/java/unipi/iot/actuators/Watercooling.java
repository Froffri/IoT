package unipi.iot.actuators;

import java.util.HashMap;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import unipi.iot.skeleton.ActuatorManager;

public class Watercooling implements ActuatorManager{

    public static class Actuator{
        private String ip;
        CoapClient coapClient;

        public Actuator(String ip){
            this.ip = ip;
            this.coapClient = new CoapClient("coap://[" + ip + "]/watercooling");
        }
        public Actuator(){
            this.ip = "fd00::203:3:3:3";
            // this.ip = "fd00::1";
            this.coapClient = new CoapClient("coap://[" + ip + "]/watercooling");
        }
    }

    private final HashMap<Integer, Actuator> waterMap = new HashMap<>();

    public Watercooling(int nSubzones) {
        for (int i = 1; i <= nSubzones; i++) {
            waterMap.put(i, new Actuator());
        }
    }

    @Override
    public void sendMessage(int subzone, String msg) {
        waterMap.get(subzone).coapClient.put(msg, MediaTypeRegistry.TEXT_PLAIN);
    }

    @Override
    public String getIp(int subzone) {
        return waterMap.get(subzone).ip;
    }
    
}
