package DataType;

import org.opencv.core.Mat;

import java.io.Serializable;
import java.sql.Timestamp;

public class PlateData extends VideoEventData implements Serializable {

    public PlateData(){

    }
    Mat frame ;//存储帧
    public PlateData(String plateStr, Timestamp time, String camerID,Mat frame) {
        this.plateStr = plateStr;
        this.setTimestamp(time);
        this.setCameraId(camerID);
        this.frame = frame;
    }
    private String plateStr;

    public String getPlateStr() {
        return plateStr;
    }

    public void setPlateStr(String plateStr) {
        this.plateStr = plateStr;
    }


}
