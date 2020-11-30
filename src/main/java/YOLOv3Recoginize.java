import config.AppConfig;
import datatype.VideoEventData;
import datatype.YOLOIdentifyData;
import detection.Box;
import detection.BoxesAndAcc;
import detection.Detector;
import lombok.extern.log4j.Log4j;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import yolo.Recognition;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgcodecs.Imgcodecs.CV_LOAD_IMAGE_COLOR;
import static org.opencv.imgcodecs.Imgcodecs.imdecode;
import static org.opencv.imgproc.Imgproc.resize;


/**
 * Class to extract frames from video using OpenCV library and process using TensorFlow.
 *
 * @author abaghel
 */
//在spark中处理所有的对象都应该是序列化的 也就是写在 spark内部函数的所有对象 最好经过序列化
 @Log4j(topic = "app2.YOLOv3Recoginize")
public class  YOLOv3Recoginize implements Serializable {

    static {
        System.out.println("========start load================" + AppConfig.OPENCV_LIB_FILE);
        log.warn("start load library");
        System.load(AppConfig.OPENCV_LIB_FILE);
        System.out.println("========end load opencv================");
    }


    // 找出位置对应的中心点，为Box_acc找出对应的边界
    private static  void transPos(Box box, int w, int h){
        //System.out.println( String.format("(%.2f,%.2f,%.2f,%.2f) (%d x %d)",box.x,box.y,box.w,box.h,w,h));
        float left = (box.x - box.w/2) * w;//w*（box.x - box.w/2）
        float top =  (box.y  - box.h/2)*h;
        float bot   = (box.y + box.h/2)*h;
        float right = (box.x + box.w/2)*w;
        box.x = left;
        box.y = top;
        box.setW(right - left);
        box.setH(bot - top);
     //   log.info("tranpos size" + box);
        return ;
    }
    public static  ArrayList<YOLOIdentifyData> recognizeWithYolo(List<VideoEventData> sortedList) {
        log.warn("load yolo success" + AppConfig.YOLO_LIB_FILE);
        Detector obj = Detector.getYoloDetector(AppConfig.YOLO_RESOURCE_PATH);
        ArrayList<YOLOIdentifyData> yolo = new ArrayList<>(sortedList.size());
        for (VideoEventData ev : sortedList) {
            YOLOIdentifyData eventData = new YOLOIdentifyData(ev);
            byte[] jpgimage = eventData.getImagebytes();
            Mat imdecode = imdecode(new MatOfByte(jpgimage), Imgcodecs.CV_LOAD_IMAGE_COLOR);
            //调整数据的大小，以防止在产生数据时没有调整好使用
           // Imgcodecs.imwrite("/home/user/Apache/App2/tracker/output/yolo/"+eventData.getTimestamp() + "-raw.jpg",imdecode);
            eventData.setCols(imdecode.cols());
            eventData.setRows(imdecode.rows());
            eventData.setType(imdecode.type());
            //识别数组获取
            byte[] data = new byte[imdecode.width()*imdecode.height()* imdecode.channels()];
            imdecode.get(0,0,data);
            //3.对每帧数据进行调整大小以及解码
            BoxesAndAcc[] res = obj.startYolo(data, imdecode.width(), imdecode.height(), imdecode.channels());
            ArrayList<BoxesAndAcc> box = new ArrayList<>();
            for (BoxesAndAcc re : res) {
                if( re.isVaild() == true && re.getNames().equals("car") ) {
                    if(re.getBoxes().getX() <0||re.getBoxes().getY() < 0||
                            re.getBoxes().getH() < 0 || re.getBoxes().getW() < 0){
                        continue;
                    }
                    //转换成与原图大小对应的原始框
                    transPos(re.getBoxes(),imdecode.width(),imdecode.height());
                    box.add(re);
                }
            }
            eventData.setCarsPoses(box);
            //识别的数据进行封装保存
            yolo.add(eventData);

        }
        return yolo;
    }

    /***
     * 会改变原有数据大小
     * @param ed
     * @return
     * @throws IOException
     */
    public static ArrayList<YOLOIdentifyData> annoteScaleImage(List<YOLOIdentifyData> ed, Size sz,List<? extends VideoEventData> outlist) throws IOException {

        return annoteImage(ed,sz,outlist);
    }

    /***
     *
     * @param ed 检测的数据
     * @param sz 最终标注的数据集大小
     * @param outList 需要标注的数据 ，需要与ed的大小一致，最终产生的标注数据会保存到该outlist中，供车牌检测的标准函数操作通过一张图片
     * @return
     * @throws IOException
     */

    private static ArrayList<YOLOIdentifyData> annoteImage(List<YOLOIdentifyData> ed,Size sz,List<? extends VideoEventData> outList) throws IOException {
        log.info("annoteScaleImage yolo");
        //5.1获取整张图片的bytes
        ArrayList<YOLOIdentifyData> result = new ArrayList<>();
        ImageUtil util = ImageUtil.getInstance(AppConfig.YOLO_LABEL_FILE);
        for (int i = 0; i < ed.size(); i++) {
            YOLOIdentifyData eventData = ed.get(i);
            byte[] imagebytes = ed.get(i).getImagebytes();
            List<Recognition> recognitions = util.transfor(eventData.getCarsPoses(), eventData.getCols(), eventData.getRows());
            log.info("recognitions before "+recognitions );
            Mat imdecode = imdecode(new MatOfByte(imagebytes), CV_LOAD_IMAGE_COLOR);
            //Imgcodecs.imwrite("/home/user/Apache/App2/tracker/output/yolo/"+eventData.getTimestamp() + "-yolo-pr.jpg",imdecode);
            //裁剪大小,重新进行编码    //所有的数据都会进入，重新调整大小为SZ，同时输出为jpg
            Mat frameResize = imdecode;
            float scaleX = 1.0f;
            float scaleY = 1.0f;
            byte[] editImage = imagebytes;
            if(sz != null && (sz.width != eventData.getCols() || sz.height != eventData.getRows())) {
                frameResize = new Mat((int)sz.height,(int)sz.width,imdecode.channels());
                resize(imdecode, frameResize, sz);
                scaleX = (float) (sz.width * 1.0 / eventData.getCols());
                scaleY= (float) (sz.height * 1.0 / eventData.getRows());
                log.info("scaleX = " + scaleX + "scaleY = " + scaleY + "size = " + sz +"row:"+ eventData.getRows() + "rawcols"  + eventData.getCols());
                MatOfByte mob = new MatOfByte();
                Imgcodecs.imencode(".jpg", frameResize, mob);
                editImage = mob.toArray();
            }
            //进行YOLO的方框标注
            byte[] res = util.labelImage(editImage, recognitions,scaleX,scaleY);
            frameResize.put(0,0,res);
            MatOfByte mob = new MatOfByte();
            Imgcodecs.imencode(".jpg", frameResize, mob);
            VideoEventData out = eventData;
            if(outList != null) {

                out = outList.get(i);
                out.setType(frameResize.type());
                out.setRows(frameResize.rows());
                out.setCols(frameResize.cols());
                out.setJpgImageBytes(mob.toArray());
                log.warn("yolo  " + out.getJpgImageBytes().length);
            }
            Imgcodecs.imwrite("/home/user/Apache/App2/tracker/output/yolo/"+eventData.getTimestamp() + "-yolo.jpg",frameResize);
            ////////////6.bytes 转换成 Mat开始进行数据封装///////////////////////////////////
            result.add(eventData);
        }
        return  result;
    }

    public static List<YOLOIdentifyData> processTrack(String camId, List<VideoEventData> sortedList, boolean isAnnotation) throws Exception {
        ArrayList<YOLOIdentifyData> yolo = recognizeWithYolo(sortedList);
        if(isAnnotation == true){
            return annoteImage(yolo,null,null);
        }else {
            return yolo;
        }
    }
}

