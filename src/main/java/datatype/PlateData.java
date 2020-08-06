package datatype;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.spark.sql.Encoders;
import pr.PlateInfo;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
@NoArgsConstructor
public class PlateData extends VideoEventData implements Serializable {
    @Getter @Setter
    private List<PlateInfo> plates ;//存储多个车牌

    public  PlateData(List<PlateInfo> plateInfos,VideoEventData ed){
        this(plateInfos,ed.getTimestamp(),ed.getCameraId(),ed.getRows(),ed.getCols(),ed.getType(),ed.getImagebytes());
    }
    public  PlateData(PlateInfo[] plateInfos,VideoEventData ed){
        this(plateInfos,ed.getTimestamp(),ed.getCameraId(),ed.getRows(),ed.getCols(),ed.getType(),ed.getImagebytes());

    }
    public PlateData(PlateInfo[] plateInfos, Timestamp time, String camerID,
                     int rows,int cols,int type,byte[] jpgimageBytes) {
        super(camerID,time,rows,cols,type,jpgimageBytes);
        if(plateInfos != null ){
            this.plates = new ArrayList<>(plateInfos.length);
            for (int i = 0; i < plateInfos.length; i++) {
                this.plates.add(plateInfos[i]);
            }
        }
    }
    public PlateData(List<PlateInfo> plateInfos, Timestamp time, String camerID,
                     int rows, int cols, int type, byte[] jpgimageBytes) {
        super(camerID,time,rows,cols,type,jpgimageBytes);
        if(plateInfos != null){
            this.plates = plateInfos;
        }
    }
    public  String getPlateStr(){
        String str = null;
        if(plates != null){
            for (int i = 0; i < plates.size(); i++) {
                if(str == null){
                    str = plates.get(i).getName();
                }else {
                    str = str + "," + plates.get(i).getName();
                }
            }

        }
        return  str;
    }
    @Override
    public  String toString(){
        Gson gson = new Gson();
        /**
         * String toJson(Object src)
         * 将对象转为 json，如 基本数据、POJO 对象、以及 Map、List 等
         * 注意：如果 POJO 对象某个属性的值为 null，则 toJson(Object src) 默认不会对它进行转化
         * 结果字符串中不会出现此属性
         */
        String json = gson.toJson(this);
        return  json;

    }
    public static void main(String[] args) {
        System.out.println(Encoders.bean(PlateData.class));

    }
}
