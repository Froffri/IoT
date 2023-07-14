package unipi.iot.actuators;

import java.util.HashMap;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import unipi.iot.skeleton.ActuatorManager;

public class PowerModule implements ActuatorManager{

    public static class Actuator{
        private String ip;
        CoapClient coapClient;

        public Actuator(String ip){
            this.ip = ip;
            coapClient = new CoapClient("coap://[" + ip + "]/power_module");
        }
        public Actuator(){
            this.ip = "fd00::f6ce:3654:b8b3:cdf8";
            // this.ip = "fd00::204:4:4:4";
            coapClient = new CoapClient("coap://[" + ip + "]/power_module");
        }
    }

    private final HashMap<Integer, Actuator> powerMap = new HashMap<>();

    public PowerModule(int nSubzones) {
        for (int i = 1; i <= nSubzones; i++) {
            powerMap.put(i, new Actuator());
        }
    }

    @Override
    public void sendMessage(int subzone, String msg) {
        powerMap.get(subzone).coapClient.put(msg, MediaTypeRegistry.TEXT_PLAIN);
    }

    @Override
    public String getIp(int subzone) {
        return powerMap.get(subzone).ip;
    }
    
}
