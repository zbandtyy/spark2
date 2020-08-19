package detection;

import lombok.Getter;
import lombok.Setter;
import org.opencv.core.Point;
import org.opencv.core.Rect;

import java.io.Serializable;

@Getter
@Setter
public class BoxesAndAcc implements Serializable {
    public  float acc;
    public Box boxes;
    public String names;
    public boolean isVaild;
    public  int size;

    public Rect transfor( int w, int h ){

        BoxesAndAcc tmp = this;
        Box box = tmp.getBoxes();
        float left = (box.getX() - box.getW()/2) * w;//w*（box.x - box.w/2）
        float top =  (box.getY()  - box.getH()/2)*h;
        float bot   = (box.getY() + box.getH()/2)*h;
        float right = (box.getX() + box.getH()/2)*w;
        return  new Rect(new Point(left,top),new Point(right,bot));

    }

    @Override
    public String toString() {
        return "BoxesAndAcc{" +
                "acc=" + acc +
                ", boxes=" + boxes +
                ", names='" + names + '\'' +
                ", isVaild=" + isVaild +
                ", size=" + size +
                '}';
    }
}
