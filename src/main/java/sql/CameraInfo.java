package sql;

public class    CameraInfo{
    @Override
    public String toString() {
        return "CameraInfo{" +
                "camerName='" + camerName + '\'' +
                ", road='" + road + '\'' +
                ", direction='" + direction + '\'' +
                ", lane='" + lane + '\'' +
                '}';
    }

    String camerName;
    String road;
    String direction;
    String lane;
    public CameraInfo(String camerName, String road, String direction, String lane) {
        this.camerName = camerName;
        this.road = road;
        this.direction = direction;
        this.lane = lane;
    }

}
