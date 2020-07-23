package pr;

import org.opencv.core.Rect;

/**
 * @author ：tyy
 * @date ：Created in 2020/5/14 17:10
 * @description：
 * @modified By：
 * @version: $
 */
public class PlateInfo {
    Rect roi;//车牌区域
    String name ;
    double confidence;
    PlateInfo(){//在jni中需要使用

    }
    public PlateInfo(Rect roi, String name, double confidence) {
        this.roi = roi;
        this.name = name;
        this.confidence = confidence;
    }

    public Rect getRoi() {
        return roi;
    }

    public String getName() {
        return name;
    }
}
