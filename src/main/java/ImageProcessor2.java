import DataType.PlateData;
import DataType.VideoEventData;
import config.AppConfig;
import imageutil.ImageEdit;
import org.apache.log4j.Logger;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import pr.PlateInfo;
import pr.PlateRecognition;
import yolo.*;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import static org.opencv.imgproc.Imgproc.resize;


/**
 * Class to extract frames from video using OpenCV library and process using TensorFlow.
 *
 * @author abaghel
 */
//在spark中处理所有的对象都应该是序列化的 也就是写在 spark内部函数的所有对象 最好经过序列化
public class  ImageProcessor2 implements Serializable {
    private static final Logger logger = Logger.getLogger(ImageProcessor2.class);

    //load native lib
    static {
        System.out.println("========start load================"+ AppConfig.OPENCV_LIB_FILE);

        logger.warn("start load library");
        System.load(AppConfig.OPENCV_LIB_FILE);
        System.out.println("========end load opencv================");
        //logger.warn("load easypr");
        //System.load(AppConfig.EASYPR_LABLE_PATH);
        //logger.warn("Completed load library");

    }

    /***
     *
     * @param camId  Key
     * @param frames Value
     * @param gap  对于一批图像跳过的帧数
     * @return 返回封装的数据
     * @throws Exception
     */
    //组内的所有视频使用相同的进行车牌识别                  Key         Value                     跳过处理的帧数
    public static ArrayList<PlateData> process(String camId, Iterator<VideoEventData> frames, String gap) throws Exception {
        //Add frames to list
        //1.创建保存结果的容器
        ArrayList<PlateData> resAll = new ArrayList<>();
        int count = 0;
        int step = new Integer(gap);
        //2.一批数据处理的图片
        ArrayList<VideoEventData> sortedList = new ArrayList<VideoEventData>();
           while (frames.hasNext()) {
                VideoEventData data = frames.next();
                if(count == step) {
                    sortedList.add(data);
                    count=0;
                }
                count++;
           }
        logger.warn("cameraId=" + camId + "processed total frames=" + sortedList.size() +"actual size" + step * sortedList.size());
        //如果美帧视频都 进来  那么使用ArrayList不是每次都在创建新的
        int frameCount = 0;
        //SimpleDateFormat fmat = new SimpleDateFormat("mm:ss");
        //String time = fmat.format(eventData.getTimestamp());
        //EasyPR pl = EasyPR.getEasyPR("/home/user/Apache/EasyPR-install/model");

        PlateRecognition pl =  PlateRecognition.getHyperLPR(AppConfig.HYPERLPR_RESOURCE_PATH);//加载模型

        for (VideoEventData eventData : sortedList) {
            //1.转码数据
            Mat frame = getMat(eventData);//解码64位编码
            MatOfByte bytemat = new MatOfByte();
            Imgcodecs.imencode(".jpg", frame, bytemat);
            byte[] bytes = bytemat.toArray();
            //2.开始识别
            ArrayList<PlateInfo> plates = null;
            if (bytes.length > 0) {
                plates = pl.getPlateInfo(bytes,0.5);
                String plateStr = new String();
                if (plates.size() > 0) {

                    for (int i = 0; i < plates.size(); i++) {
                        plateStr += plates.get(i).getName() +",";
                    }
                    byte[] labled = ImageEdit.editPlateInfos(bytes,plates);
                    frame.put(0,0,labled);
                    resAll.add(new PlateData(plateStr, eventData.getTimestamp(), eventData.getCameraId(),frame));
                }

            }
        }
        logger.warn("a batch recognize success");
        return resAll;
    }
    //Get Mat from byte[]
    private static Mat getMat(VideoEventData ed) throws Exception {
        Mat mat = new Mat(ed.getRows(), ed.getCols(), ed.getType());
        mat.put(0, 0, Base64.getDecoder().decode(ed.getData()));
        return mat;
    }

    private static Mat getMat2(int rows, int cols, int type, byte[] ed) throws Exception {
        Mat mat = new Mat(rows, cols, type);
        mat.put(0, 0, ed);
        return mat;
    }
    //Save image file
    private static String saveImageAndData(Mat mat, VideoEventData ed, String match, String outputDir) throws IOException {
        String imagePath = outputDir + ed.getCameraId() + "-T-" + ed.getTimestamp().getTime() + ".jpg";
        logger.warn("Saving images to " + imagePath);
        boolean result = Imgcodecs.imwrite(imagePath, mat);
        if (!result) {
            logger.error("Couldn't save images to path " + outputDir + ".Please check if this path exists. This is configured in processed.output.dir key of property file.");
        }
        String matchPath = outputDir + ed.getCameraId() + "-T-" + ed.getTimestamp().getTime() + ".txt";
        logger.warn("Saving classification result to " + imagePath);
        Files.write(Paths.get(matchPath), match.getBytes());
        return  imagePath;
    }


    public static ArrayList<VideoEventData>  processTrack(String camId, Iterator<VideoEventData> frames, String gap) throws Exception {
        //Add frames to list
        ArrayList<VideoEventData>  result = new ArrayList<>();
        //1.创建保存结果的容器   对一批数据进行保存
        int count = 0;
        int step = new Integer(gap);
        //2.一批数据处理的图片
        ArrayList<VideoEventData> sortedList = new ArrayList<VideoEventData>();
        while (frames.hasNext()) {
            VideoEventData data = frames.next();
            if(count == step) {
                sortedList.add(data);
                count=0;
            }
            count++;
        }
        logger.warn("preparing process " +  sortedList.size()+"frames");
        //2.处理保存的所有数据
        int imageWidth = 640;
        int imageHeight = 480;
        Size sz = new Size(imageWidth, imageHeight);//opencv中数据的大小
        logger.warn("cameraId=" + camId + "processed total frames=" + sortedList.size() +"actual size" + step * sortedList.size());
        //如果美帧视频都 进来  那么使用ArrayList不是每次都在创建新的
        for (VideoEventData eventData : sortedList) {
            //3.对每帧数据进行调整大小以及解码
            Mat newFrame = getMat(eventData);//解码64位编码
            Mat frame = new Mat();
            resize(newFrame, frame, new Size(imageWidth,imageHeight)); //把dst调整到src一半的大小
            long size = frame.total() * frame.elemSize();
            byte bytes[] = new byte[(int)size];
            frame.get(0,0,bytes);
            ///////////////////////////4.进行识别//////////////////////////////////
            Detector d = Detector.getDetector(AppConfig.YOLO_RESOURCE_PATH);
            BoxesAndAcc[] Box_acc = d.startYolo(bytes,imageWidth,imageHeight,3);
            ///////////////////////////5.进行图片上的标记//////////////////////////////////
            //5.1获取整张图片的bytes
            MatOfByte mob = new MatOfByte();
            Imgcodecs.imencode(".jpg", frame, mob);
            // convert the "matrix of bytes" into a byte array
            byte[] byteArray = mob.toArray();
            //5.2进行标注
            ImageUtil util = ImageUtil.getInstance(AppConfig.YOLO_LABEL_FILE);
            List<Recognition> recognitions = util.transfor(Box_acc,640 ,480) ;
            System.out.println("recognitions length"+recognitions.size());
            byte[] res = util.labelImage(byteArray, recognitions);
            ////////////6.bytes 转换成 Mat开始进行数据封装///////////////////////////////////
            VideoEventData obj = new VideoEventData();
            obj.setCameraId( eventData.getCameraId());
            obj.setTimestamp(eventData.getTimestamp());
            obj.setRows(frame.rows() );
            obj.setCols(frame.cols());
            obj.setType(frame.type());
            obj.setData( Base64.getEncoder().encodeToString(res));
            result.add(obj);
        }
        return result;

    }
    public static ArrayList<PlateData> processAll(String camId, Iterator<VideoEventData> frames, String gap) throws Exception {
        //Add frames to list
        ArrayList<PlateData>  result = new ArrayList<>();
        //1.创建保存结果的容器   对一批数据进行保存
        int count = 0;
        int step = new Integer(gap);
        //2.一批数据处理的图片
        ArrayList<VideoEventData> sortedList = new ArrayList<VideoEventData>();
        while (frames.hasNext()) {
            VideoEventData data = frames.next();

            sortedList.add(data);
            count++;
        }
        logger.warn("cameraId=" + camId + "processed total frames=" + sortedList.size() +"actual size" + step * sortedList.size());
        //如果美帧视频都 进来  那么使用ArrayList不是每次都在创建新的
        int frameCount = 0;
        //SimpleDateFormat fmat = new SimpleDateFormat("mm:ss");
        //String time = fmat.format(eventData.getTimestamp());
        //EasyPR pl = EasyPR.getEasyPR("/home/user/Apache/EasyPR-install/model");

        PlateRecognition pl =  PlateRecognition.getHyperLPR(AppConfig.HYPERLPR_RESOURCE_PATH);//加载模型
        Detector d = Detector.getDetector(AppConfig.YOLO_RESOURCE_PATH);


        //2.处理保存的所有数据
        int imageWidth = 640;
        int imageHeight = 480;
        Size sz = new Size(imageWidth, imageHeight);//opencv中数据的大小
        logger.warn("cameraId=" + camId + "processed total frames=" + sortedList.size() +"actual size" + step * sortedList.size());
        //如果美帧视频都 进来  那么使用ArrayList不是每次都在创建新的
        for (VideoEventData eventData : sortedList) {
            //3.对每帧数据进行调整大小以及解码
            Mat newFrame = getMat(eventData);//解码64位编码
            Mat frame = new Mat();
            resize(newFrame, frame, new Size(imageWidth,imageHeight)); //把dst调整到src一半的大小
            long size = frame.total() * frame.elemSize();
            byte bytes[] = new byte[(int)size];
            frame.get(0,0,bytes);
            ///////////////////////////4.进行识别//////////////////////////////////
            BoxesAndAcc[] Box_acc = d.startYolo(bytes,imageWidth,imageHeight,3);
            ///////////////////////////5.进行图片上的标记//////////////////////////////////
            //5.1获取整张图片的bytes
            MatOfByte mob = new MatOfByte();
            Imgcodecs.imencode(".jpg", frame, mob);
            // convert the "matrix of bytes" into a byte array
            byte[] byteArray = mob.toArray();
            //5.2进行标注
            ImageUtil util = ImageUtil.getInstance(AppConfig.YOLO_LABEL_FILE);
            List<Recognition> recognitions = util.transfor(Box_acc,640 ,480) ;
            System.out.println("recognitions length"+recognitions.size());
            byte[] res = util.labelImage(byteArray, recognitions);

            //2.开始识别
            String plateStr = new String();
            ArrayList<PlateInfo> plates = null;
            if (bytes.length > 0) {
                plates = pl.getPlateInfo(bytes,0.5);
                if (plates.size() > 0) {
                    for (int i = 0; i < plates.size(); i++) {
                        plateStr += plates.get(i).getName() +",";
                    }
                    byte[] labled = ImageEdit.editPlateInfos(byteArray,plates);
                    frame.put(0,0,labled);
                }
            }
            ////////////6.bytes 转换成 Mat开始进行数据封装///////////////////////////////////
            PlateData obj = new PlateData();
            obj.setPlateStr(plateStr);
            obj.setCameraId( eventData.getCameraId());
            obj.setTimestamp(eventData.getTimestamp());
            obj.setRows(frame.rows() );
            obj.setCols(frame.cols());
            obj.setType(frame.type());
            obj.setData( Base64.getEncoder().encodeToString(res));
            result.add(obj);
        }
        return result;

    }
}

