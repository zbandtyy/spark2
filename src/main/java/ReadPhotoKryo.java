import datatype.PlateData;
import datatype.VideoEventData;
import datatype.YOLOIdentifyData;
import lombok.extern.log4j.Log4j;
import org.apache.spark.api.java.function.FlatMapGroupsFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.opencv.core.Size;
import serialize.VideoEventDataKryoDeSerializer;
import util.PropertyFileReader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * @author ：tyy
 * @date ：Created in 2020/7/29 19:56
 * @description：
 * @modified By：
 * @version: $
 */
@Log4j(topic = "app2.ReadPhotoKryo")
public class ReadPhotoKryo {
    public static void main(String[] args) throws Exception {

        if(args.length < 1){
            System.out.println("Please input gap of image");
            return;
        }
        String gap = args[args.length - 1];
        log.warn("your gap is"+ gap);
        //Read properties
        Properties prop = PropertyFileReader.readPropertyFile();

        //SparkSesion 配置spark环境  Spark SQL 的入口。
        //使用 Dataset 或者 Datafram 编写 Spark SQL 应用的时候，第一个要创建的对象就是 SparkSession。
        SparkSession spark = SparkSession
                .builder()//使用builer创建sparkSession的实例
                .appName("VideoStreamProcessor1")
                .master(prop.getProperty("spark.master.url"))//设置主要的spark 环境  spark://mynode1:7077

                .getOrCreate();	//获取或者创建一个新的sparksession

        //directory to save image files with motion detected 有什么用？
        final String processedImageDir = prop.getProperty("processed.output.dir");//  /home/user/Apache/project
        log.warn("Output directory for saving processed images is set to "+processedImageDir+". This is configured in processed.output.dir key of property file.");
        log.warn(prop.getProperty("kafka.bootstrap.servers"));

        //Create DataSet from stream messages from kafka  配置kafka的数据格式
        //// Subscribe to 1 topic defaults to the earliest and latest offsets
        Dataset<Row> ds1 = spark
                .readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", prop.getProperty("kafka.bootstrap.servers"))//创建并且订阅了几个kafka主题
                .option("subscribe", prop.getProperty("kafka.topic"))
                // .option("failOnDataLoss",false)
                 .option("startingOffsets", "{\"kryo-collector\":{\"0\":10}}")//必须指定全部
                .option("kafka.max.partition.fetch.bytes", prop.getProperty("kafka.max.partition.fetch.bytes"))
                .option("kafka.max.poll.records", prop.getProperty("kafka.max.poll.records"))
                .option("maxOffsetsPerTrigger","5")//开了最多的200个Ta sk处理全部的历史数据，groupby的时候shuffle存储空间不够，应该限制接受的一批 数据大小
                //.option("startingOffsets", "earliest")
                //.option("endingOffsets", "{\"video-kafka-large\":{\"0\":50,\"1\":-1}")
                .load();
        Dataset<byte[]> bytesRow = ds1.select("value")
                .as(Encoders.BINARY());
        Dataset<VideoEventData> map = bytesRow.map(new MapFunction<byte[], VideoEventData>() {

            @Override
            public VideoEventData call(byte[] value) throws Exception {
                VideoEventData deserialize = new VideoEventDataKryoDeSerializer().deserialize("", value);
                return  deserialize;
            }
        }, Encoders.bean(VideoEventData.class));

        // key-value pair of cameraId-VideoEventData 窗口进行分组的时候是针对聚合实践进行处理
        KeyValueGroupedDataset<String, VideoEventData> kvDataset = map.groupByKey(new MapFunction<VideoEventData, String>() {
            @Override
            public String call(VideoEventData value) throws Exception {
                return value.getCameraId();
            }
        }, Encoders.STRING());
        Dataset<PlateData> df = kvDataset.flatMapGroups(new FlatMapGroupsFunction<String, VideoEventData, PlateData>() {
            @Override
            public Iterator<PlateData> call(String key, Iterator<VideoEventData> values) throws Exception {
                // classify image  key 是cameraid    values是数据集
                System.out.println("======start process===================");
                ArrayList<VideoEventData> inputdata = ImageProcess.loadAndSortData(values);
                List<YOLOIdentifyData> processed = YOLOv3Recoginize.processTrack(key,inputdata ,false);
                List<PlateData> pr = PlateProcessing.process(key,inputdata ,false);
                ImageProcess.changeAndAnnotateImage(processed,pr,inputdata,new Size(640,480));
                System.out.println("======end process===================");
                return pr.iterator();
            }
        },Encoders.bean(PlateData.class));



/////////////////////////////////////////车牌识别/////////////////////////////////////////////////////////////////////////
//        Dataset<PlateData> df = df.flatMapGroups(new FlatMapGroupsFunction<String, VideoEventData, PlateData>() {
//            @Override
//            public Iterator<PlateData> call(String key, Iterator<VideoEventData> values) throws Exception {
//                // classify image  key 是cameraid    values是数据集
//                System.out.println("======start process===================");
//                List<PlateData> processed = PlateProcessing .process(key, values);
//                System.out.println("======end process===================");
//                return processed.iterator();
//            }
//        },Encoders.bean(PlateData.class));


//        Dataset<Tuple3<String, String, String>> map1 = df.map(new MapFunction<PlateData, Tuple3<String, String, String>>() {
//            @Override
//            public Tuple3<String, String, String> call(PlateData value) throws Exception {
//                DateFormat format = new SimpleDateFormat("ss:SSS");
//                return new Tuple3<String, String, String>(value.getCameraId(), format.format(value.getTimestamp())
//                        , value.getPlateStr());
//            }
//        }, Encoders.tuple(Encoders.STRING(), Encoders.STRING(), Encoders.STRING()));
        StreamingQuery query1 = df.writeStream()
                .outputMode("update")
                .format("console")
                .start();
        query1.awaitTermination();

    }
}
