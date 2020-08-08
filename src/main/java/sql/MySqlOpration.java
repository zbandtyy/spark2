package sql;

import datatype.PlateData;

import java.sql.SQLException;

/**
 * @author ：tyy
 * @date ：Created in 2020/8/7 15:57
 * @description：
 * @modified By：
 * @version: $
 */
public class MySqlOpration {
    public  static TrackSql mysql = null;
    public static void init() throws SQLException, ClassNotFoundException {
        mysql = new TrackSql();
    }
    public static  void process( PlateData value) {
        System.out.println("start process,the value mysql is" + mysql);
        if(value.getPlateStr() == null){
            return;
        }
        String[] plateStrs = value.getPlateStr().split(",");//","车牌以，分割

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
    public  static  void close() throws SQLException {
        mysql.close();
    }
}
