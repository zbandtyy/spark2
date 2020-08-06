package detection;

import config.AppConfig;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Rect2d;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import java.util.List;

import static org.opencv.videoio.Videoio.CV_CAP_PROP_POS_FRAMES;

/**
 * @author ：tyy
 * @date ：Created in 2020/7/22 20:27
 * @description：
 * @modified By：进行车牌的识别测试
 * @version: $
 */
public class YOLOTest {
    public static  Mat cropFromImage(Mat image,Rect2d rect){
        if(rect.x + rect.width >= image.width() ||
                rect.y + rect.height >= image.height()||
        rect.x < 0 || rect.y < 0){
            return  null;

        }
//        int zeroadd_w  = (int) (rect.width*0.30);
//        int zeroadd_h = (int)(rect.height*2);
//        int zeroadd_x = (int)(rect.width*0.15);
//        int zeroadd_y = (int)(rect.height*1);
//        rect.x-=zeroadd_x;
//        rect.y-=zeroadd_y;
//        rect.height += zeroadd_h;
//        rect.width += zeroadd_w;
        Rect n  =  new Rect((int)rect.x,(int)rect.y,(int)rect.width,(int)rect.height);
        Mat  temp ;
        temp = image.submat(n);
        Mat cropped = new Mat();
        temp.copyTo(cropped);
        return cropped;

    }
    public static void main(String[] args) {
        System.load(AppConfig.OPENCV_LIB_FILE);
        System.load(AppConfig.YOLO_LIB_FILE);
        YoloDetectCar yolo = new YoloDetectCar();
        VideoCapture vc = new VideoCapture("/home/user/Apache/App1/test/4k6.mp4");
        vc.set(CV_CAP_PROP_POS_FRAMES,27);
        Mat frame = new Mat();
        int frameCount = 27;
        while (vc.read(frame)) {
            if(frameCount % 10 == 0){
                List<Rect2d> rect2ds = yolo.detectObject(frame);
                for (int i = 0; i < rect2ds.size(); i++) {
                    if(rect2ds.get(i).area() > 300*300){
                        Rect2d plate = rect2ds.get(i);
                        Mat cars = cropFromImage(frame,plate);
                        if(cars != null) {
                            Imgcodecs.imwrite("/home/user/Apache/App1/test/cars/" + frameCount +
                                    "_" + i + ".jpg", cars);
                        }

                    }
                }
            }
            frameCount++;
            

        }

    }
}
