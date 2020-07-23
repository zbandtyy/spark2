package config;


import scala.App;
import util.PropertyFileReader;

import java.io.Serializable;
import java.util.Properties;

/**
 * @author ：tyy
 * @date ：Created in 2020/3/14 21:45
 * @description：路径等相关配置
 *      FILE 表示要文件名
 *      PATH:表示要路径 路径下应该包含的文件 具体详情见注释
 * @modified By：
 * @version: $
 */
public class AppConfig implements Serializable {
    public static  final  String  KAFKA_CONFIG_FILE= "./stream-processor.properties";//包含所有可配置的路径！！！

    public  static    String OPENCV_LIB_FILE="/home/user/Apache/opencv3.4.7-install/lib/libopencv_java347.so";

    //YOLO的模型文件等的路径 ，目录形式必须如下
    //String cfgPath = Path+"cfg/yolov3.cfg";
    //		String weightsPath =Path+ "yolov3.weights";
    //			String datacfg = Path+"cfg/coco.data";
    //		String labelpath = Path+"data/labels";
    public  static    String YOLO_RESOURCE_PATH="/home/user/Apache/App2/tracker/config";
    public  static    String YOLO_LIB_PATH="/home/user/Apache/App2/tracker/config/libjdetection.so";
    public  static  String HYPERLPR_LIB_PATH="/home/user/Apache/App2/lib/libhyperlprjava.so";
    public  static  String HYPERLPR_RESOURCE_PATH="/home/user/Apache/App2/lib/";//车牌识别参数文件位置


    //YOLO的各种物体的名称文件(按行分割)  eg Car,Track
    public  static    String YOLO_LABEL_FILE="/home/user/Apache/App2/tracker/config/coco.names";
    public  static    String EASYPR_LABLE_PATH="/home/user/Apache/EasyPR-install/libeasyprjni.so";

    public  static    String MYSQL_CONNECT_URL="jdbc:mysql://115.157.201.214/tracker?user=root&serverTimezone=UTC";
    public  static    String MYSQL_USER_NAME="root";
    public  static    String MYSQL_USER_PASSWD="123456";
    public  static    String MYSQL_JDBC_CLASSNAME="com.mysql.cj.jdbc.Driver";

    static{
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    static void init() throws Exception {
        if(KAFKA_CONFIG_FILE != null){
            System.out.println("kafka_config_file" + AppConfig.KAFKA_CONFIG_FILE);
            Properties prop = PropertyFileReader.readPropertyFile();
            if(prop.getProperty("yolo.label.file") != null){

                YOLO_LABEL_FILE = prop.getProperty("yolo.label.file");
            }
            if(prop.getProperty("yolo.resource.file") != null ){
                YOLO_RESOURCE_PATH = prop.getProperty("yolo.resource.file");
                System.out.println(YOLO_RESOURCE_PATH);
            }
            if(prop.getProperty("easypr.label.path") != null ){
                YOLO_RESOURCE_PATH = prop.getProperty("easypr.label.path");
            }
            if(prop.getProperty("easypr.label.path") != null ){
                EASYPR_LABLE_PATH = prop.getProperty("easypr.label.path");
            }
            if(prop.getProperty("hyperlpr.lib.path") != null ){
                HYPERLPR_LIB_PATH = prop.getProperty("hyperlpr.lib.path");
            }
            if(prop.getProperty("hyperlpr.resource.path") != null ){
                HYPERLPR_RESOURCE_PATH = prop.getProperty("hyperlpr.resource.path");
            }



        }


    }


}
