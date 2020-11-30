import config.AppConfig;
import datatype.PlateData;
import datatype.VideoEventData;
import datatype.YOLOIdentifyData;
import detection.Box;
import detection.BoxesAndAcc;
import imageutil.ImageEdit;
import lombok.extern.log4j.Log4j;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import pr.PlateInfo;
import pr.PlateRecognition;
import scala.Tuple2;
import util.OverlappedArea;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.opencv.imgcodecs.Imgcodecs.*;
import static org.opencv.imgproc.Imgproc.resize;

/**
 * @author ：tyy
 * @date ：Created in 2020/7/31 16:11
 * @description：
 * @modified By：
 * @version: $
 */
@Log4j(topic = "app2.PlateProcessing")
public class PlateProcessing {
    //load native lib
    static {
        System.out.println("========start load================"+ AppConfig.OPENCV_LIB_FILE);

        log.warn("start load library");
        System.load(AppConfig.OPENCV_LIB_FILE);
        System.out.println("========end load opencv================");
        //logger.warn("load easypr");
        //System.load(AppConfig.EASYPR_LABLE_PATH);
        //logger.warn("Completed load library");

    }
    /*编辑距离*/
    public static   long levenshteinDistance(char[] s1, char[] s2) {
        int len1 = s1.length, len2 = s2.length;
        int[] col = new int[len2 + 1];
        int[] prevCol = new int[len2 + 1];
        for ( int i = 0; i < prevCol.length; i++) prevCol[i]  = i;//第一行
        for (int i = 0; i < len1; i++) {
            col[0] = i + 1;//当前行
            for ( int j = 0; j < len2; j++) {
                col[j + 1] = Math.min(
                        Math.min(prevCol[1 + j] + 1, col[j] + 1),
                        prevCol[j] + (s1[i] == s2[j] ? 0 : 1));
            }
            int[] temp = prevCol;
            prevCol = col;
            col = temp;

        }
        return prevCol[len2];
    }
    /***
     *实现对异常常车牌数据的修改，修改为最短编辑距离的合理的车牌
     * @param collectorResult 所有的车牌数据结构集合
     */
    public static  void updatNormalUsedMajorityRule( ArrayList<PlateInfo> collectorResult){
        Map<String, Long> collect = collectorResult.stream()
                .collect(Collectors.groupingBy(o -> o.getName(), Collectors.counting()));//1重新进行分组统计
        ArrayList<Tuple2<String,Long>> list = new ArrayList<>(collect.size());
        Iterator<String> iterator = collect.keySet().iterator();
        while(iterator.hasNext()){
            String key = iterator.next();
            list.add(new Tuple2<>(key,collect.get(key)));
        }
        list.sort(new Comparator<Tuple2<String, Long>>() {
            @Override
            public int compare(Tuple2<String, Long> o1, Tuple2<String, Long> o2) {
                return (int) (o1._2 - o2._2);
            }
        });//2按照分组数量大小进行排序
        log.warn("updatNormalUsedMajorityRule list: " + list);
        log.warn("collect" + collect);
        for (int i = 0; i < collectorResult.size(); i++) {

            PlateInfo plateInfo = collectorResult.get(i);
            String plateStr = plateInfo.getName();
            char[] needModify = plateStr.toCharArray();
            if(needModify.length != 7){
                continue;
            }
            //正常情况下，偏向检测结果多的数据
            //1.查找结果多的数据
            long size = collect.get(plateStr);
            String newpStr = null;
            for (int j = list.size() - 1; j >= 0; j--) {
                String s = list.get(j)._1;
                if(size < collect.get(s)){
                    char[] newpb = list.get(j)._1().toCharArray();
                    if (newpb.length == 7 && levenshteinDistance(needModify, newpb) <= 2) {
                        newpStr = list.get(j)._1();
                    }
                }
            }
            if (newpStr != null) {
                plateInfo.setName(newpStr);
                log.warn("updatNormalUsedMajorityRule replace" + plateStr + "for" + newpStr);
            }
        }//3.少数服从多数的更新原则
    }
    /***
     *实现对异常常车牌数据的修改，修改为最短编辑距离的合理的车牌
     * @param collectorResult 所有的车牌数据结构集合
     *
     *
     */
    public  static void updateNotNormalPlate( ArrayList<PlateInfo> collectorResult){

        // 先修正所有不正常的数据
        Map<String, Long> collect = collectorResult.stream()
                .collect(Collectors.groupingBy(o -> o.getName(), Collectors.counting()));//1重新进行分组统计
        ArrayList<Tuple2<String,Long> > list = new ArrayList<>(collect.size());
        Iterator<String> iterator = collect.keySet().iterator();
        while(iterator.hasNext()){
            String key = iterator.next();
            list.add(new Tuple2<>(key,collect.get(key)));
        }
        list.sort(new Comparator<Tuple2<String, Long>>() {
            @Override
            public int compare(Tuple2<String, Long> o1, Tuple2<String, Long> o2) {
                return (int) (o1._2 - o2._2);
            }
        });//2按照分组数量大小进行排序
        log.warn("updateNotNormalPlate list:" + list);
        log.warn("collect" + collect);
        for (int i = 0; i < collectorResult.size(); i++) {
            PlateInfo plateInfo = collectorResult.get(i);
            String plateStr = plateInfo.getName();
            char[]  needModify = plateStr.toCharArray();//需要更正的车牌
            if(needModify.length != 7){ //异常车牌即选择一个最相似的进行修正 （一般车牌号为7位）
                String newStr = null;
                int mindistance = -1;
                for (int j = list.size() - 1; j >= 0; j--) {
                    char[]  newpb = list.get(j)._1.toCharArray(); //需要更正的车牌
                    if(newpb.length == 7 ){
                        if(mindistance == -1){
                            mindistance = (int) levenshteinDistance(needModify,newpb);
                            newStr = list.get(j)._1; //一定会更新为其中一个正常的
                        }else {
                            int d = (int) levenshteinDistance(needModify, newpb);//进行对比
                            if(mindistance > d){
                                newStr = list.get(j)._1;
                                mindistance  = d;
                            }

                        }
                    }
                }
                if(newStr != null) {
                    plateInfo.setName(newStr);
                    log.warn(" updateNotNormalPlate replace" + plateStr + "for" + newStr);
                }

            }
        }
    }

    /***
     * 修正数据
     * @param resAll 所以检测到的车牌数据集合，不会修改PlateData[]的顺序，
     */
    public  static  void modifyData(PlateData[] resAll){
        log.warn("start modifyData for consistency");
        /////检测结果数据进行修正////////////////
        ArrayList<PlateInfo> collectorResult = new ArrayList<>();
        for (int i = 0; i < resAll.length; i++) {
            List<PlateInfo> plates = resAll[i].getPlates();
            if(plates == null) continue;
            for (int j = 0; j < plates.size(); j++) {
                if(plates.get(j) != null)
                    //再内部会修改的内容
                    collectorResult.add(plates.get(j));
            }
        }
        updatNormalUsedMajorityRule(collectorResult);
        //按照少数服从多数的原则进行修正，保证批次内的数据内容是一致的,
        updateNotNormalPlate(collectorResult);

    }


    /***
     *
     * @param sortedList  所有需要进行车牌检测的视频帧
     * @return
     */
    public static  PlateData[] detectAndRecognize(List<? extends  VideoEventData> sortedList)  {
        log.warn("start detect plates AndRecognize");
        PlateData[] resAll = new PlateData[sortedList.size()];
        PlateRecognition pl =  PlateRecognition.getHyperLPR(AppConfig.HYPERLPR_RESOURCE_PATH);
        int frmaeCount = 0;
        for (VideoEventData eventData : sortedList) {
            //1.转码数据
            Mat frame = imdecode(new MatOfByte(eventData.getImagebytes()), CV_LOAD_IMAGE_COLOR);
            byte[] recognizebyts = new byte[frame.rows()*frame.cols()* frame.channels()];
            frame.get(0,0,recognizebyts);
           // Imgcodecs.imwrite("/home/user/Apache/App2/tracker/output/yolo/"+ eventData.getTimestamp()+ "-pr-before.jpg",frame);
            //2.开始识别
            ArrayList<PlateInfo> plates = null;
            if (recognizebyts.length >=  frame.rows()*frame.cols()*frame.channels()) {
                plates = pl.getPlateInfo(recognizebyts,frame.rows(),frame.cols(),0.7);
            }
            //System.out.println(resAll[frmaeCount]);
            //进行数据保存，以防止初始的数据不正确
            resAll[frmaeCount] = new PlateData(plates,eventData);
            log.info("识别结果"+resAll[frmaeCount].getPlateStr());
            resAll[frmaeCount].setCols(frame.cols());
            resAll[frmaeCount].setRows(frame.rows());
            resAll[frmaeCount].setType(frame.type());
            frmaeCount++;
        }

        return  resAll;
    }

    /***
     * 根据yolo检测的结果进行调整车牌绘制位置，位置为车的上方
     * @param yoloRes
     * @param prRes
     */
    public static  void updatePlatePos(List<YOLOIdentifyData> yoloRes, List<PlateData> prRes){
        log.info("updatePlatePos by yolo");
        for (int i = 0; i < prRes.size(); i++) {
            PlateData plates = prRes.get(i);
            YOLOIdentifyData yolo = yoloRes.get(i);
            //保证修改的是同一张图片,查找与车牌相同的框，进行位置绘制的更改
            if(plates.getTimestamp().equals(yolo.getTimestamp())  && plates.getCameraId().equals(yolo.getCameraId())){
                log.info("==== isdatasame====" + (yolo.getImagebytes() == plates.getImagebytes()) + "i = " + i);
                List<PlateInfo> platesid = plates.getPlates();
                List<BoxesAndAcc> carsPoses = yolo.getCarsPoses();
                log.info("plateidsize=" + platesid.size() + "  yolo =" + carsPoses.size());
                for (PlateInfo plateInfo : platesid) {
                    log.info("enter update by pos\n" + plateInfo);
                    Box pBox = plateInfo.getBox();
                    Point rt = new Point(pBox.getX(),pBox.getY());
                    Point rb = new Point(pBox.getX() + pBox.getH(),pBox.getY() + pBox.getW());
                    Rect pRect = new Rect(rt, rb);
                    //查看车牌是否坐落再车牌的位置之内，如果再则对绘制位置进行更新
                    double maxArea = 0;
                    for (BoxesAndAcc carsPose : carsPoses) {
                        Box boxes = carsPose.getBoxes();
                        Rect rrect = new Rect((int) boxes.getX(), (int)boxes.getY(), (int)boxes.getW(), (int)boxes.getH());
                        System.out.println(boxes);
                        double overlappedArea = OverlappedArea.getOverlappedArea(pRect,rrect);
                        if((rt.inside(rrect) && rb.inside(rrect)) ){//车牌的左上角和右下角都在内部，那么更新位置
                            log.info("updatePlatePos update rt.inside(rrect)" + pBox);
                            pBox.setX( rrect.x);
                            pBox.setY(rrect.y);
                            pBox.setH(rrect.height);
                            pBox.setW(rrect.width);
                            log.info("updatePlatePos update after" + pBox);
                            break;
                        }else if(maxArea < overlappedArea){
                            log.info("updatePlatePos update maxArea < overlappedArea" + pBox);
                            pBox.setX( rrect.x);
                            pBox.setY(rrect.y);
                            pBox.setH(rrect.height);
                            pBox.setW(rrect.width);
                            log.info("updatePlatePos update after" + pBox);
                        }
                    }

                }

            }



        }
        

    }
    /***
     *
     * @param camId  Key
     * @param sortedList Value
     * @return 返回封装的数据
     * @throws Exception
     */
    //组内的所有视频使用相同的进行车牌识别                  Key         Value                     跳过处理的帧数
    public static <T> List<PlateData> process(String camId,  List<? extends VideoEventData > sortedList , boolean isAnnotation) throws Exception {
        ///////////////处理数据/////////////////
        //1.加载原始
        log.warn("cameraId=" + camId + "processed total frames=" + sortedList.size() +"actual size" +  sortedList.size());
        PlateData[] resAll = detectAndRecognize(sortedList);
        log.info("recogize plates" + resAll.length);
        //保证resAll的数据顺序不会变动
        modifyData(resAll);
        if(isAnnotation == true)
            return annoteImage(resAll);
        else
            return Arrays.asList(resAll);


    }

    /***
     *
     * @param resAll 未进行任何处理的原始图片
     * @param size
     * @param inList，inList的图片数据已经经过yolo标注的图片，这里使用该图片重新进行车牌的标注
     *              就重新制定 size是要求输出的分辨率，
     *              imdecode是yolo标准完进行修改的输出分辨率
     *               plateData.getCols() 进行检测时数据的分辩率
     * @throws IOException
     */
    public static void annoteScaleImage(List<PlateData> resAll, Size size, List<? extends VideoEventData> inList) throws IOException {
        log.info("annote annoteScaleImage Image");
        //1.获取数据，对数据车牌绘制位置进行更改
        int i = 0;
        for (PlateData plateData : resAll) {
            //inList的大小为 640 * 480 * 3为更改后的图片
            if(inList != null && inList.size() == resAll.size()) {
                plateData.setJpgImageBytes(inList.get(i).getImagebytes());
            }
            byte[] imagebytes = plateData.getImagebytes();
            Mat imdecode = imdecode(new MatOfByte(imagebytes), CV_LOAD_IMAGE_COLOR);
            log.info(String.format("pr get jpg： height * width(%d * %d)",imdecode.height(),imdecode.width() ) );
            log.info("getRows" + plateData.getRows()+ "getCols" + plateData.getCols());//获取的是Jpg格式的数据 应该格式为1920 * 1080的数据
        //    Imgcodecs.imwrite("/home/user/Apache/App2/tracker/output/yolo/"+plateData.getTimestamp()+"-pr.jpg",imdecode);
            //裁剪大小,重新进行编码
            Mat frame = imdecode;
            //如果图片本身的分辨率不同，就重新制定 size是要求输出的分辨率，imdecode是yolo标准完进行修改的输出分辨率
            if(size != null &&( size.height != imdecode.rows() || size.width != imdecode.cols())) {
                frame = new Mat();
                resize(imdecode, frame, size);

            }
            //重新调整边框的位置
            float scaleX = (float) (size.width * 1.0 / plateData.getCols());
            float scaleY = (float) (size.height * 1.0 / plateData.getRows());
            log.info("scaleX = " + scaleX + "scaleY = " + scaleY + "size = " + size +"row:"+ plateData.getRows() + "rawcols"  + plateData.getCols());
            List<PlateInfo> plates = plateData.getPlates();
            log.info("before changed scale" + plates );
            for (PlateInfo plate : plates) {
                Box plateRect = plate.getBox();
                plateRect.x = (int) (plateRect.x * scaleX);
                plateRect.y = (int) (plateRect.y * scaleY);
                plateRect.h = (int) (plateRect.h * scaleY);
                plateRect.w = (int) (plateRect.w * scaleX);

            }
            log.info("after changed scale" + plates );
            //最终形成的数据
            MatOfByte buf = new MatOfByte();
            imencode(".jpg", frame, buf);
            imwrite("/home/user/Apache/App2/output/outputFile/"+plateData.getTimestamp()+".jpg",frame);
            plateData.setJpgImageBytes(buf.toArray());
            plateData.setCols(frame.cols());
            plateData.setRows(frame.rows());
            plateData.setData(null);
            plateData.setType(frame.type());
            plateData.setPlates(plates);
            i++;
        }
        //2.进行绘制
        annoteImage((PlateData[]) resAll.toArray());
    }
    /***
     *
     * @param resAll 所有需要进行标注的图片
     * @return
     * @throws IOException
     */
    public static List<PlateData> annoteImage( PlateData[] resAll) throws IOException {
        log.warn("annote pr Image");
        ///////////给标注数据//////////////////
        for (int i = 0; i < resAll.length; i++) {
            if(resAll[i].getPlates() != null ) {
                log.info("i" + resAll[i].getPlates());
                byte[] jpgbytes = resAll[i].getImagebytes();//获取数据,数据应该已经是yolo进行标注完的， 并且已经调整到了指定的大小
                byte[] labled = null;
                if(resAll[i].getPlates().size()  > 0) {
                    labled = ImageEdit.editPlateInfos(jpgbytes, resAll[i].getPlates());
                }
                //主要是为了保存数据在本地，实现写入，存储数据的话只需要保存labled就行,用于调试
                Mat frame = imdecode(new MatOfByte(jpgbytes), CV_LOAD_IMAGE_COLOR);
                if(labled != null) {
                    log.info("labelength" +  labled.length);
                    frame.put(0, 0, labled);

                    MatOfByte mob = new MatOfByte();
                    Imgcodecs.imencode(".jpg", frame, mob);
                    resAll[i].setJpgImageBytes( mob.toArray()); //更新新的编辑图片数据,只需要更新由车牌的数据
                    Imgcodecs.imwrite("/home/user/Apache/App2/tracker/output/yolo/"+resAll[i].getTimestamp()+"has-plate.jpg",frame);
                }
              //  Imgcodecs.imwrite("/home/user/Apache/App2/tracker/output/yolo/"+resAll[i].getTimestamp()+"res.jpg",frame);

            }
        }
        return  Arrays.asList(resAll);
    }


}
