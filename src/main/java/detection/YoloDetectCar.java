package detection;

import config.AppConfig;
import org.apache.log4j.Logger;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Rect2d;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ：tyy
 * @date ：Created in 2020/6/10 1:33
 * @description：
 * @modified By：
 * @version: $
 */
public class YoloDetectCar implements  Serializable {
    private static final Logger logger = Logger.getLogger(YoloDetectCar.class);
    Detector obj;
    List<Rect2d> detectedObjects = new ArrayList<>();
    public YoloDetectCar(){//每批会进行创建
        obj = Detector.getYoloDetector(AppConfig.YOLO_RESOURCE_PATH);
    }
    public List<Rect2d> detectObject(Mat m) {
        detectedObjects.clear();
        long size = m.total() * m.elemSize();
        byte bytes[] = new byte[(int)size];  // you will have to delete[] that later
        m.get(0,0,bytes);
        System.out.println( m.width() +"  " +m.height()  +" "+ m.channels());
        BoxesAndAcc[] res = obj.startYolo(bytes, m.width(), m.height(), m.channels());
        String name = Thread.currentThread().getName();
        System.out.println(name + "end yolo" );
        if(res.length  <= 0){
            logger.warn("detector number < 0" + res.length);
            return null;
        }
        ArrayList<Rect2d> last= new ArrayList<>();
        for (BoxesAndAcc re : res) {
            if( re.isVaild() == true && re.getNames().equals("car") ) {
                logger.info("recognize object" + re);
                if(re.getBoxes().getX() <0||re.getBoxes().getY() < 0||
                        re.getBoxes().getH() < 0 || re.getBoxes().getW() < 0){
                    continue;
                }
                Rect tmp  = re.transfor(m.width(),m.height());
                Rect2d rect = new Rect2d(tmp.x,tmp.y,tmp.width,tmp.height);
                last.add(rect);
                detectedObjects.add(rect);
                logger.info(tmp);
            }
        }
        logger.info("car length is:" + last.size());
        return  last;
   }
}
