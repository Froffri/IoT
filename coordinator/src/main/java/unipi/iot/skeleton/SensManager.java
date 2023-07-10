package unipi.iot.skeleton;

public interface SensManager{

    void get(int[] subzones);
    void set(int[] subzones, int down, int up);
    
    String check(int subzone, int temp);
}


