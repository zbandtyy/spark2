import datatype.PlateData;
import datatype.VideoEventData;
import datatype.YOLOIdentifyData;
import lombok.extern.log4j.Log4j;
import org.apache.log4j.Logger;
import org.apache.spark.api.java.function.FlatMapGroupsFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.opencv.core.Size;
import scala.Tuple2;
import util.PropertyFileReader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
@Log4j(topic = "app2")
public class ReadPhoto {
    private static final Logger logger = Logger.getLogger(ReadPhoto.class);

    public static void main(String[] args) throws Exception {

        //Read properties
        Properties prop = PropertyFileReader.readPropertyFile();

        //SparkSesion 配置spark环境  Spark SQL 的入口。
        //使用 Dataset 或者 Datafram 编写 Spark SQL 应用的时候，第一个要创建的对象就是 SparkSession。
        SparkSession spark = SparkSession
                .builder()//使用builer创建sparkSession的实例
                .appName("VideoStreamProcessor123457")
                .master(prop.getProperty("spark.master.url"))//设置主要的spark 环境  spark://mynode1:7077
                .getOrCreate();	//获取或者创建一个新的sparksession

        //directory to save image files with motion detected 有什么用？
        final String processedImageDir = prop.getProperty("processed.output.dir");//  /home/user/Apache/project
        logger.warn("Output directory for saving processed images is set to "+processedImageDir+". This is configured in processed.output.dir key of property file.");

        //create schema for json message 配置能够取得的数据格式
        StructType schema =  DataTypes.createStructType(new StructField[] {
                DataTypes.createStructField("cameraId", DataTypes.StringType, false),
                DataTypes.createStructField("timestamp", DataTypes.TimestampType, false),
                DataTypes.createStructField("rows", DataTypes.IntegerType, false),
                DataTypes.createStructField("cols", DataTypes.IntegerType, false),
                DataTypes.createStructField("type", DataTypes.IntegerType, false),
                DataTypes.createStructField("data", DataTypes.StringType, false),
                DataTypes.createStructField("jpgImageBytes", DataTypes.BinaryType, true)
        });

        logger.warn(prop.getProperty("kafka.bootstrap.servers"));

        //Create DataSet from stream messages from kafka  配置kafka的数据格式
        //// Subscribe to 1 topic defaults to the earliest and latest offsets
        Dataset<Row> ds1 = spark
                .readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", prop.getProperty("kafka.bootstrap.servers"))//创建并且订阅了几个kafka主题
                .option("subscribe"," collector-1920-1080")//prop.getProperty("kafka.topic")
                .option("failOnDataLoss",false)
                .option("startingOffsets", "{\"collector-1920-1080\":{\"0\":10}}")//必须指定全部
                .option("kafka.max.partition.fetch.bytes", prop.getProperty("kafka.max.partition.fetch.bytes"))
                .option("kafka.max.poll.records", prop.getProperty("kafka.max.poll.records"))
                .option("maxOffsetsPerTrigger","20")//开了最多的200个Ta sk处理全部的历史数据，groupby的时候shuffle存储空间不够，应该限制接受的一批 数据大小
               // .option("startingOffsets", "earliest")
                //.option("endingOffsets", "{\"video-kafka-large\":{\"0\":50,\"1\":-1}")
                .load();
        logger.warn("subscribe" + prop.getProperty("kafka.topic"));

        Dataset<VideoEventData> ds= ds1.selectExpr("CAST(value AS STRING) as message")//读取数据
                .select(functions.from_json(functions.col("message"),schema).as("json"))//选择数据的格式，在后面的query中使用
                .select("json.*")
                .as(Encoders.bean(VideoEventData.class));//将所有数据转换为 Dataset<VideoEventData> 不分开产生问题》怀疑历史数据被他一批读走了，限制
        logger.warn("subscribe" + prop.getProperty("kafka.topic") );
        logger.warn(ds.schema());
       // key-value pair of cameraId-VideoEventData 窗口进行分组的时候是针对聚合实践进行处理
        KeyValueGroupedDataset<String, VideoEventData> kvDataset = ds.groupByKey(new MapFunction<VideoEventData, String>() {
            @Override
            public String call(VideoEventData value) throws Exception {
                return value.getCameraId();
            }
        }, Encoders.STRING());
       /////////////////// //1. YOLO识别测试//////// json数据 + data为jpg格式///////////////////////////////////

        Dataset<PlateData> dfTrack = kvDataset.flatMapGroups(new FlatMapGroupsFunction<String, VideoEventData, PlateData>() {
            @Override
            public Iterator<PlateData> call(String key, Iterator<VideoEventData> values) throws Exception {
                // classify image  key 是cameraid    values是数据集
                System.out.println("======start process===================");
                ArrayList<VideoEventData> inputdata = ImageProcess.loadAndSortData(values);
                List<YOLOIdentifyData> processed = YOLOv3Recoginize.processTrack(key,inputdata ,false);
                List<PlateData> pr = PlateProcessing.process(key,inputdata ,false);//前面2步只做数据的检测和保存，并没有对input数据产生改动
                ImageProcess.changeAndAnnotateImage(processed,pr,inputdata,new Size(640,480));
                //有保存到数据库的操作
                ImageProcess.saveAsMysql(pr);
                System.out.println("======end process===================");
                return pr.iterator();
            }
        },Encoders.bean(PlateData.class));
        Dataset<Row> djson = dfTrack.map(new MapFunction<PlateData, Tuple2<String, String>>() {
            @Override
            public Tuple2<String, String> call(PlateData pd) throws Exception {

                return new Tuple2<>(pd.getCameraId(), pd.toJson());
            }

        }, Encoders.tuple(Encoders.STRING(), Encoders.STRING())).toDF("key", "value");

        StreamingQuery query = djson
                .selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)") //如果Kafka数据记录中的字符存的是UTF8字符串，我们可以用cast将二进制转换成正确类型
                .writeStream()
                .format("kafka")
                .option("kafka.bootstrap.servers",prop.getProperty("kafka.bootstrap.servers"))
                .option("topic", prop.getProperty("kafka.result.topic"))
                .option("kafka.max.request.size", prop.getProperty("kafka.result.max.request.size"))
                .option("kafka.batch.size",prop.getProperty("kafka.result.batch.size"))
                .option("kafka.compression.type","gzip")
                .option("checkpointLocation", prop.getProperty("checkpoint.dir"))//对输入的数据也有影响！！注意 （这个位置为恢复的位置，不同的应用重新设置）
                .start();//启动实时流计
        logger.warn("output" + prop.getProperty("kafka.result.topic"));
        query.awaitTermination();
        ///////////////////////////////////////2.车牌识别////////////////////////////////
////        一个专门做车牌检测     （查看在Spark中是否会并行执行）
////        两个信息进行联合？？ 由两个时间戳和视频进行连接即可进入Kafka中，可以分开进行！！！！ 缺点：数据冗余？no 一批数据内存能hold住吗？yes lantency：需要测量
////         group数据的迭代器，new(K,V ,U(返回值类型))
//        Dataset<PlateData> df = kvDataset.flatMapGroups(new FlatMapGroupsFunction<String, VideoEventData, PlateData>() {
//            @Override
//        public Iterator<PlateData> call(String key, Iterator<VideoEventData> values) throws Exception {
//            // classify image  key 是cameraid    values是数据集
//                System.out.println("======start process===================");
//                List<PlateData> processed = PlateProcessing.process(key, values);
//                System.out.println("======end process===================");
//                return processed.iterator();
//        }
//    },Encoders.bean(PlateData.class));

//    MysqlSink mysql  = new MysqlSink("jdbc:mysql://10.68.243.135:3306/track?user=root","root","123456");
//    StreamingQuery query1 = df
//                            .writeStream()
//                            .foreach(mysql)
//                            .start();
//        StreamingQuery query1 = djson.writeStream()
//                .outputMode("update")
//                .format("console")
//                .start();
//        query1.awaitTermination();
 //   query.awaitTermination();
//        StreamingQuery query1 = ds1
//                .writeStream()
//                .format("json")
//                //.trigger(Trigger. ("1 seconds"))//对历史记录他想要一次处理，这只针对新批入的数据，
//                .option("checkpointLocation", "./project")
//                .option("path","./project")
//                .start();
//        query1.awaitTermination();
    }
}
