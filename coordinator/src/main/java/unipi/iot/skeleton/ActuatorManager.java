package unipi.iot.skeleton;

public interface ActuatorManager {
    void sendMessage(int subzone, String msg);
    String getIp(int subzone);
}
