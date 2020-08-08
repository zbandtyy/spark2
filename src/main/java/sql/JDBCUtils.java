package sql;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import config.AppConfig;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @author ：tyy
 * @date ：Created in 2020/7/19 1:49
 * @description：
 * @modified By：
 * @version: $
 */
public class JDBCUtils {
   private static DataSource dataSource = null;
    static {
        //1.加载文件
        Properties properties = new Properties();
        InputStream resourceAsStream = JDBCUtils.class.getClassLoader().getResourceAsStream("druid-config.properties");
        try {
            properties.load(resourceAsStream);
            properties.setProperty("url" , AppConfig.MYSQL_CONNECT_URL);
            properties.setProperty("username" ,AppConfig.MYSQL_USER_NAME);
            properties.setProperty("password" ,AppConfig.MYSQL_USER_PASSWD);
            properties.setProperty("driverClassName" ,AppConfig.MYSQL_JDBC_CLASSNAME);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
             dataSource = DruidDataSourceFactory.createDataSource(properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public  static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

    public static<T> void  close(T t){

        if(t != null){
            try {
                 Class<?> c = t.getClass();
                Method close = c.getMethod("close");
                //执行方法
                close.invoke(t);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args) {
        Connection connection = null;
        try {
            connection  = JDBCUtils.getConnection();
       //     INSERT INTO `tracker`.`VehicleInfoTable` (`plateStr`, `VIN`, `carBranch`, `owner`, `ownerID`) VALUES ('湘H123456', '234', 'yake', 'tyy', '233');
          //  String sql = "insert into employ(idemploy,name,salary) value (?,?,?);";
            String sql = " INSERT INTO VehicleInfoTable (plateStr, VIN, carBranch, owner, ownerID) VALUES (?,?,?,?,?)";
            PreparedStatement pstm1 = connection.prepareStatement(sql);
            pstm1.setString(1,"湘H126");
            pstm1.setString(2,"唐艳阳");
            pstm1.setString(3,"yake");
            pstm1.setString(4,"tyy");
            pstm1.setString(5,"777");
            int count = pstm1.executeUpdate();
            System.out.println(count);
            JDBCUtils.close(pstm1);
            JDBCUtils.close(connection);

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

}
