package pr;

import config.AppConfig;
import imageutil.ImageEdit;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import scala.App;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;

import static org.opencv.highgui.HighGui.imshow;

/**
 * @author ：tyy
 * @date ：Created in 2020/5/15 16:08
 * @description：
 * @modified By：
 * @version: $
 */
public class TestHyperLPR {
    static {
        //System.loadLibrary("easyprjni");
        System.load(AppConfig.HYPERLPR_LIB_PATH);
       // System.load(AppConfig.OPENCV_LIB_FILE);
    }
    public static void main(String[] args) throws IOException {
        String basePath = AppConfig.HYPERLPR_RESOURCE_PATH;
        PlateRecognition pr = PlateRecognition.getHyperLPR(basePath);
        Mat image = Imgcodecs.imread("./7.jpg");
        MatOfByte bytemat = new MatOfByte();


        //该函数对图像进行压缩，并将其存储在调整大小以适应结果的内存缓冲区中。
        Imgcodecs.imencode(".jpg", image, bytemat);
        byte[] bytes = bytemat.toArray();
        System.out.println("byte length :" + bytes.length);
        long start = new Date().getTime();
        ArrayList<PlateInfo> res = pr.getPlateInfo(bytes, 0.6);//需要的byte是一张完整的 jpg图片
        long end = new Date().getTime();
        System.out.println("time is" + (end - start));

        byte[] resbytes = ImageEdit.editPlateInfos(bytes, res);

//        for (int i = 0; i < res.size(); i++) {
//
//                Rect region = res.get(i).roi;
//                String plate = new String(res.get(i).name.getBytes(), "utf-8");
//                System.out.print(res.get(i).roi.x + " ");
//                System.out.print(res.get(i).roi.y + " ");
//                System.out.print(res.get(i).roi.width + " ");
//                System.out.println(res.get(i).roi.height);
//                System.out.println(res.get(i).confidence);
//                Imgproc.rectangle(image, new Point(region.x +10, region.y +10), new Point(region.x + region.width -10, region.y + region.height-10), new Scalar(255, 255, 0), 2);//图片上进行标注
//            }
//        }

        image.put(0,0,resbytes);
        Imgcodecs.imwrite("result.jpg",image);
       // imshow("image",image);

    }
    public static byte[] setImageToByteArray(String fileName) {
        File file = new File(fileName);
        return setImageToByteArray(file);
    }

    /**
     * 图片文件转化为二进制
     *
     * @param file
     * @return
     */
    public static byte[] setImageToByteArray(File file) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(file);
            byte[] filebuff = new byte[fis.available()];
            fis.read(filebuff);
            fis.close();
            return filebuff;
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
        }
        return null;
    }
}
