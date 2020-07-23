package sql;
import config.AppConfig;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.sql.*;
import java.text.MessageFormat;
public class TrackSql implements Serializable {
    private static final Logger logger = Logger.getLogger(TrackSql.class);
    public static Connection conn = null;
    public static Statement stat =null;
    public static PreparedStatement ps = null;

    /**
     * 试图让他只能调用一次，How to do？多线程？？？进来两次也没关系吧，喵？
     * 如果有两个线程同时进来，怎么办？对于cnn和stat 肯定有泄漏，但是getConnection是单例模式
     * @param url
     * @param user
     * @param passwd
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public  TrackSql(String url,String user,String passwd) throws ClassNotFoundException, SQLException {
        //1.注册数据库的驱动
        if(conn == null) {
            Class.forName(AppConfig.MYSQL_JDBC_CLASSNAME);
            //2.获取数据库连接（里面内容依次是："jdbc:mysql://主机名:端口号/数据库名","用户名","登录密码"）
            conn = DriverManager.getConnection(url, user, passwd);
            stat = conn.createStatement();
        }
    }
    //创建表
    public void createTable() throws SQLException {
        //2.获取数据库连接（里面内容依次是："jdbc:mysql://主机名:端口号/数据库名","用户名","登录密码"）
        // 3.需要执行的sql语句（?是占位符，代表一个参数）
        stat.executeUpdate("create table IF NOT EXISTS TrackTable(plateStr varchar(20)not null primary key, cameraSeq Text)");//{1:n}，n为camerID + timestamp；
        stat.executeUpdate("create table IF NOT EXISTS CameraTable(cameraName varchar(40) primary key,road varchar(80),direction varchar(40),lane varchar(80))");
        stat.executeUpdate("create table IF NOT EXISTS  VehicleInfoTable(plateStr varchar(20) not null primary key,VIN varchar(40), carBranch varchar(40),owner varchar(20),ownerID varchar(20))");

    }

    //2.往表中插入内容
     public void insertCameraTable(String cameraName,String roadName ,String direction,String plane){
        String str = MessageFormat.format("insert into CameraTable(cameraName,road,direction,lane) values(\"{0}\",\"{1}\",\"{2}\",\"{3}\") ",cameraName,roadName,direction,plane );
        System.out.println(str);
        try {
            stat.executeUpdate(str);
        }catch (SQLIntegrityConstraintViolationException e){
            System.out.println(e.getMessage());
        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    //应该要有可以绑定
     public void insertTrackTable(TrackTable tt) throws SQLException {
        //1.如果有相同的主键，则增加相应的数据
        logger.warn("insertTrackTable");
        TrackTable torg = getTrackByPlateStr(tt.getPlateStr());
        if(torg != null){
            tt.append(torg);// video：新的时间，video2:旧的时间
        }
         //2.将增加的数据插入到数据库中
        String str = MessageFormat.format("insert into TrackTable(plateStr,cameraSeq) values(\"{0}\",\"{1}\")" +
                "on duplicate key update cameraSeq=\"{2}\"",tt.getPlateStr(),tt.getCameraSeq(),tt.getCameraSeq());
        try {
            logger.warn(str);
            stat.executeUpdate(str);
            logger.warn("start");
        }catch (SQLIntegrityConstraintViolationException e){
            System.out.println("has error");

            e.printStackTrace();
            throw e;
        }
        System.out.println("insert Track sucess");
    }
    //这些表还不能更新
     public void insertVehicleInfoTable(String plateStr,String VIN,String carBranch,String owner,String ownerID ) throws SQLException {
        String str = MessageFormat.format("insert into VehicleInfoTable(plateStr,VIN,carBranch,owner,ownerID) " +
                     "values(\"{0}\",\"{1}\",\"{2}\",\"{3}\",\"{4}\")",plateStr,VIN,carBranch,owner,ownerID);

        stat.executeUpdate(str);

    }


    //3.获取信息根据车牌号查询车辆信息  只能有一个
    public VehicleInfo getVehicleInfoByPlateStr(String plateStr) throws SQLException {

        String sql = "select * from  VehicleInfoTable where plateStr =?";
        ps= conn.prepareStatement(sql);
        ps.setString(1,plateStr);
        ResultSet rs = ps.executeQuery();
        while(rs.next()){
              return new VehicleInfo(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5));
        }

        return  null;

    }
    //根据摄像头编号查询摄像头的位置
    public CameraInfo getCameraInfoByCID(String cameraID) throws SQLException {

        String sql = "select * from  CameraTable where cameraName=?";
        ps = conn.prepareStatement(sql);
        ps.setString(1,cameraID);
        ResultSet rs = ps.executeQuery();
        while(rs.next()){
            return new CameraInfo(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4));
        }
        return  null;
    }
    public TrackTable getTrackByPlateStr(String plateStr) throws SQLException {
        logger.warn("getTrackByPlateStr");
        String sql = "select * from  TrackTable where PlateStr =?";
        logger.warn(sql + conn);
        ps = conn.prepareStatement(sql);
        ps.setString(1,plateStr);
        logger.warn("has prepared "+ plateStr);
        logger.warn(ps.toString()+ ps.executeQuery());
        ResultSet rs = ps.executeQuery();
        logger.warn("executeQuery");
        while(rs.next()){
            return new TrackTable(rs.getString(1),rs.getString(2));
        }
        return  null;
    }
    public void close() throws SQLException {
        if(ps != null && conn != null && stat !=null ) {
            ps.close();
            conn.close();
            stat.close();
        }
    }
    //4.test
    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        TrackSql sql = new TrackSql("jdbc:mysql://115.157.201.214/tracker?user=root&serverTimezone=UTC","root","123456");
        sql.createTable();
        System.out.println(sql.getCameraInfoByCID("video-01"));
        System.out.println(sql.getVehicleInfoByPlateStr("苏E05EV8"));
        TrackTable track = sql.getTrackByPlateStr("苏E05EV8");
        System.out.println(track);
        sql.close();
    }
}
