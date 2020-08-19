package pr;

import detection.Box;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Rect;

import java.io.Serializable;
@Slf4j(topic = "app2.PlateInfo")
public class PlateInfo implements Serializable {
    @Setter @Getter
    Box box ;
    @Setter @Getter
    String name ;//车牌名称
    @Setter @Getter
    double confidence;
    @Setter
    transient Rect roi;//必须保留。需要使用！！！！！！！！！！！！
    PlateInfo(){//在jni中需要使用

    }
    public Box getPlateRect(){

        if(roi == null){
            log.warn("caanot get the roi ,roi == null");
        }
        Box box = new Box();
        box.x = roi.x;
        box.y = roi.y;
        box.w = roi.width;
        box.h = roi.height;
        return  box;
    }
    public PlateInfo(Box roi, String name, double confidence) {
        this.box = roi;
        this.name = name;
        this.confidence = confidence;
    }


    @Override
    public String toString() {
        return "PlateInfo{" +
                "box=" + box +
                ", name='" + name + '\'' +
                ", confidence=" + confidence +
                '}';
    }

    public String getName() {
        return name;
    }

}
