import datatype.PlateData;
import datatype.VideoEventData;
import datatype.YOLOIdentifyData;
import lombok.extern.log4j.Log4j;
import org.opencv.core.Size;
import sql.MySqlOpration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author ：tyy
 * @date ：Created in 2020/8/4 20:06
 * @description：
 * @modified By：
 * @version: $
 */
@Log4j(topic = "app2.ImageProcess")
public class ImageProcess {
    /***
     * 在这里的数据 YOLOIdentifyData和PlateData 是一样的
     * @param process  进行yolo识别的数据结果，必须包含识别框
     * @param pr  进行车牌识别的结果，必须包含车牌的位置
     * @param size  最后需要保存的图片的大小
     */
    public static List<PlateData> changeAndAnnotateImage(List<YOLOIdentifyData> process, List<PlateData> pr,List<VideoEventData>input, Size size) throws IOException {
        //NOTE：更好的是传递videoEventdata进行修改
        //1.根据yolo识别的结果对车牌识别pr进行更新，主要是更新车牌绘制的位置为车辆的上方，如果不需要更改位置，则不用动
        PlateProcessing.updatePlatePos(process,pr);
        //2.绘制yolo识别结果 ，对数据的大小进行更改，共享data数据
        YOLOv3Recoginize.annoteScaleImage(process,size,input);
        //3.绘制车牌识别pr结果，对数据的大小进行更改，一样的绘制  ，3-4的位置不能进行交换
        PlateProcessing.annoteScaleImage(pr,size,input);
        //4.保存图片数据，退出 （主要保存车牌识别的结果）
        return  pr;
    }
    public static ArrayList<VideoEventData> loadAndSortData(Iterator<VideoEventData> frames) {
        //Add frames to list
        log.warn("sorted by timestamp");
        //2.一批数据处理的图片
        ArrayList<VideoEventData> sortedList = new ArrayList<>();
        while (frames.hasNext()) {

            VideoEventData raw = frames.next();
            log.info(String.format("frames rows*cols(%dX%d) before",raw.getRows(),raw.getCols() ));

//            raw.setCols(1920);
//            raw.setRows(1080);
//            raw.setType(16);
            log.info("get data" + raw);
            log.info(String.format("frames rows*cols(%dX%d) changed",raw.getRows(),raw.getCols() ));
            sortedList.add(raw);
        }
        sortedList.sort((o1, o2) -> {
            return (int) (o1.getTimestamp().getTime() - o2.getTimestamp().getTime());
        });
        log.warn("preparing process " + sortedList.size() + "frames");

        return sortedList;
    }
    public  static void saveAsMysql(List<PlateData> list) throws SQLException, ClassNotFoundException {

        MySqlOpration.init();
        for (PlateData plateData : list) {

            MySqlOpration.process(plateData);
        }
        MySqlOpration.close();
    }


}
