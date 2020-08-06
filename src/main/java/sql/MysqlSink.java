package sql;

import datatype.PlateData;
import config.AppConfig;
import org.apache.log4j.Logger;
import org.apache.spark.sql.ForeachWriter;


import java.io.Serializable;
import java.sql.*;

public class MysqlSink extends ForeachWriter<PlateData> implements Serializable {
    String url;
    String id;
    String passwd;
    TrackSql mysql ;
    static int count = 0;
    private static final Logger logger = Logger.getLogger(TrackSql.class);
    public MysqlSink(String sqlMaster, String id, String passwd)  {
        url = sqlMaster;
        this.id = id;
        this.passwd = passwd;
    }

    /**
     * //初始化 ，但不确定调用的次数？？？在哪里调用
     * @param partitionId
     * @param version
     * @return
     */
    @Override
    public boolean open(long partitionId,  long version) {
        //open 和close 的次数 过多，需要资源吗？
        try {

            // 必须在这里初始化 这里是会变得地方，想办法抽象出来
            mysql = new TrackSql(AppConfig.MYSQL_CONNECT_URL,AppConfig.MYSQL_USER_NAME,AppConfig.MYSQL_USER_PASSWD);//初始化 连接不能序列化

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("partitionid " + partitionId);
        System.out.println("url " + this.url);
        System.out.println("version " + version);
        if(mysql != null)
            return true;
        else
            return  false;
    }

    /**
     *
     * @param value 根据识别的结果value，找出车牌号，存储到TrackTable中，value中的plateStr格式要求为：video：time，video2，time
     *
     */
    @Override
    public void process( PlateData value) {
        System.out.println("start process,the value mysql is" + mysql);
        String plateStrs[] = value.getPlateStr().split(",");//","车牌以，分割
        String cameraSeq = value.getCameraId()+":"+value.getTimestamp();//摄像头序列，video：time
        System.out.println("start process,the value mysql is" + value.getPlateStr()+ plateStrs[0]);
        for(String s: plateStrs){
            try {
                System.out.println("start insert"+ s+ cameraSeq);
                mysql.insertTrackTable(new TrackTable(s,cameraSeq));//s是分割车牌，对应的video:time
                System.out.println(s+"***********" +cameraSeq);

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    /**
     * 多线程关闭的时候冲突了，喵喵喵？？？？
     */
    public void close( Throwable errorOrNull) {
//        try {
//            System.out.println("mysql is " + mysql);
//            if(mysql != null)
//                mysql.close();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
    }
}
