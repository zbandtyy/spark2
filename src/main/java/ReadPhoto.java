import DataType.PlateData;
import DataType.VideoEventData;
import org.apache.log4j.Logger;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.FlatMapGroupsFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import scala.Tuple2;
import sql.MysqlSink;
import util.PropertyFileReader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

public class ReadPhoto {
    private static final Logger logger = Logger.getLogger(ReadPhoto.class);

    public static void main(String[] args) throws Exception {

        if(args.length < 1){
            System.out.println("Please input gap of image");
            return;
        }
        String gap = args[args.length - 1];
        logger.warn("your gap is"+ gap);
        //Read properties
        Properties prop = PropertyFileReader.readPropertyFile();

        //SparkSesion 配置spark环境  Spark SQL 的入口。
        //使用 Dataset 或者 Datafram 编写 Spark SQL 应用的时候，第一个要创建的对象就是 SparkSession。
        SparkSession spark = SparkSession
                .builder()//使用builer创建sparkSession的实例
                .appName("VideoStreamProcessor123456")
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
                DataTypes.createStructField("data", DataTypes.StringType, false)
        });

        logger.warn(prop.getProperty("kafka.bootstrap.servers"));

        //Create DataSet from stream messages from kafka  配置kafka的数据格式
        //// Subscribe to 1 topic defaults to the earliest and latest offsets
        Dataset<Row> ds1 = spark
                .readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", prop.getProperty("kafka.bootstrap.servers"))//创建并且订阅了几个kafka主题
                .option("subscribe", prop.getProperty("kafka.topic"))
                .option("failOnDataLoss",false)
               // .option("startingOffsets", "{\"video-kafka-large\":{\"1\":100,\"0\":100}}")//必须指定全部
                .option("kafka.max.partition.fetch.bytes", prop.getProperty("kafka.max.partition.fetch.bytes"))
                .option("kafka.max.poll.records", prop.getProperty("kafka.max.poll.records"))
                .option("maxOffsetsPerTrigger","20")//开了最多的200个Ta sk处理全部的历史数据，groupby的时候shuffle存储空间不够，应该限制接受的一批 数据大小
                .option("startingOffsets", "earliest")
                //.option("endingOffsets", "{\"video-kafka-large\":{\"0\":50,\"1\":-1}")
                .load();
        Dataset<VideoEventData> ds= ds1.selectExpr("CAST(value AS STRING) as message")//读取数据
                .select(functions.from_json(functions.col("message"),schema).as("json"))//选择数据的格式，在后面的query中使用
                .select("json.*")
                .as(Encoders.bean(VideoEventData.class));//将所有数据转换为 Dataset<VideoEventData> 不分开产生问题》怀疑历史数据被他一批读走了，限制

       // key-value pair of cameraId-VideoEventData 窗口进行分组的时候是针对聚合实践进行处理
        KeyValueGroupedDataset<String, VideoEventData> kvDataset = ds.groupByKey(new MapFunction<VideoEventData, String>() {
            @Override
            public String call(VideoEventData value) throws Exception {
                return value.getCameraId();
            }
        }, Encoders.STRING());
       /////////////////// //1. YOLO识别测试///////////////////////////////////////////

        Dataset<VideoEventData> dfTrack = kvDataset.flatMapGroups(new FlatMapGroupsFunction<String, VideoEventData, VideoEventData>() {
            @Override
            public Iterator<VideoEventData> call(String key, Iterator<VideoEventData> values) throws Exception {
                // classify image  key 是cameraid    values是数据集
                ArrayList<VideoEventData> processed = ImageProcessor2.processTrack(key, values,gap);
                return processed.iterator();
            }
        },Encoders.bean(VideoEventData.class));


        Dataset<Row> djson = dfTrack.flatMap(new FlatMapFunction<VideoEventData, Tuple2<String, String>>() {
            @Override
            public Iterator<Tuple2<String, String>> call(VideoEventData videoEventData) throws Exception {
                ArrayList<Tuple2<String, String>> pro = new ArrayList<>();
                pro.add(new Tuple2<>(videoEventData.getCameraId(), videoEventData.toJson()));
                return pro.iterator();
            }

        }, Encoders.tuple(Encoders.STRING(), Encoders.STRING())).toDF("key", "value");

//        StreamingQuery query = djson.writeStream()
//                .outputMode("update")
//                .format("console")
//                .start();
//     	query.awaitTermination();
        StreamingQuery query = djson
                .selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)") //如果Kafka数据记录中的字符存的是UTF8字符串，我们可以用cast将二进制转换成正确类型
                .writeStream()
                .format("kafka")
                .option("kafka.bootstrap.servers",prop.getProperty("kafka.bootstrap.servers"))
                .option("topic", prop.getProperty("kafka.result.topic"))
                .option("kafka.max.request.size", prop.getProperty("kafka.result.max.request.size"))
                .option("kafka.batch.size",prop.getProperty("kafka.result.batch.size"))
                .option("kafka.compression.type","gzip")
                .option("checkpointLocation", prop.getProperty("checkpoint.dir"))
                .start();//启动实时流计

        ///////////////////////////////////////2.车牌识别////////////////////////////////
////        一个专门做车牌检测     （查看在Spark中是否会并行执行）
////        两个信息进行联合？？ 由两个时间戳和视频进行连接即可进入Kafka中，可以分开进行！！！！ 缺点：数据冗余？no 一批数据内存能hold住吗？yes lantency：需要测量
////         group数据的迭代器，new(K,V ,U(返回值类型))
        Dataset<PlateData> df = kvDataset.flatMapGroups(new FlatMapGroupsFunction<String, VideoEventData, PlateData>() {
            @Override
        public Iterator<PlateData> call(String key, Iterator<VideoEventData> values) throws Exception {
            // classify image  key 是cameraid    values是数据集
            ArrayList<PlateData> processed = ImageProcessor2.process(key, values,gap);
            return processed.iterator();
        }
    },Encoders.bean(PlateData.class));
    MysqlSink mysql  = new MysqlSink("jdbc:mysql://192.168.0.100:3306/track?user=root","root","123456");
    StreamingQuery query1 = df
                            .writeStream()
                            .foreach(mysql)
                            .start();

    query1.awaitTermination();
    query.awaitTermination();

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
