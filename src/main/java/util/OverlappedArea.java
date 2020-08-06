package util;


import org.opencv.core.Rect;

/**
 * @author ：tyy
 * @date ：Created in 2020/8/5 16:46
 * @description：
 * @modified By：
 * @version: $
 */
public class OverlappedArea {
    public static  double getOverlappedArea(Rect rect1, Rect rect2){

        if (rect1 == null || rect2 == null) {
            return -1;
        }

        if (rect1 == null || rect2 == null) {
            return -1;
        }
        double p1_x = rect1.x, p1_y = rect1.y;
        double p2_x = p1_x + rect1.width, p2_y = p1_y + rect1.height;
        double p3_x = rect2.x, p3_y = rect2.y;
        double p4_x = p3_x + rect2.width, p4_y = p3_y + rect2.height;
        if (p1_x > p4_x || p2_x < p3_x || p1_y > p4_y || p2_y < p3_y) {
            return 0;
        }
        double Len = Math.min(p2_x, p4_x) - Math.max(p1_x, p3_x);
        double Wid = Math.min(p2_y, p4_y) - Math.max(p1_y, p3_y);
        return Len * Wid;

    }
}
