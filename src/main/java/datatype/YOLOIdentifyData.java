package datatype;

import detection.BoxesAndAcc;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * @author ：tyy
 * @date ：Created in 2020/8/4 14:37
 * @description：
 * @modified By：
 * @version: $
 */
@NoArgsConstructor
public class YOLOIdentifyData extends VideoEventData {
    @Setter @Getter
    private List<BoxesAndAcc> carsPoses;
    public YOLOIdentifyData(VideoEventData data){
        super(data.getCameraId(),data.getTimestamp(),data.getRows(),data.getCols(),data.getType(),data.getImagebytes(),data.getData());
    }


    public void setJpgImageBytes(byte[]jpgImageBytes){
        super.setJpgImageBytes(jpgImageBytes);
        super.setData(null);
    }
}
