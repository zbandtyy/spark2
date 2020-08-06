package pr;

import config.AppConfig;
import imageutil.ImageEdit;
import org.junit.Test;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import static org.opencv.imgcodecs.Imgcodecs.CV_LOAD_IMAGE_COLOR;
import static org.opencv.imgcodecs.Imgcodecs.imdecode;
import static org.opencv.imgproc.Imgproc.resize;

/**
 * @author ：tyy
 * @date ：Created in 2020/5/15 16:08
 * @description：
 * @modified By：
 * @version: $
 */
public class TestHyperLPR {

    @Test
    public  void testVideo() throws IOException {
        System.load(AppConfig.OPENCV_LIB_FILE);

        VideoCapture videoCapture = new VideoCapture();
        boolean isopen = videoCapture.open("F:\\LPR\\data\\4k2\\4k2.mp4");
        if (isopen == false) return;
        String basePath = AppConfig.HYPERLPR_RESOURCE_PATH;
        PlateRecognition pr = PlateRecognition.getHyperLPR(basePath);
        int frameCount = 0;
        //2.对第一帧检测到的对象进行方框标记,并且对Tracker进行初始化
        Mat frame1 = new Mat();
        while (videoCapture.read(frame1)) {
            Mat frame = new Mat();
            resize(frame1,frame,new Size(1920,1080));

            MatOfByte bytemat = new MatOfByte();
            //该函数对图像进行压缩，并将其存储在调整大小以适应结果的内存缓冲区中。
            Imgcodecs.imencode(".jpg", frame, bytemat);
            byte[] rawBytes = bytemat.toArray();
            System.out.println(rawBytes.length);
            //////////////一般的数据封装以及传输过程//////////////////////////////
            if(frameCount % 5 == 0){
                //正常的解码过程
                Mat imdecode = imdecode(new MatOfByte(rawBytes), CV_LOAD_IMAGE_COLOR);
                byte[] data = new byte[imdecode.rows()* imdecode.cols()*imdecode.channels()];
                imdecode.get(0,0,data);
                try {
                    ArrayList<PlateInfo> res = pr.getPlateInfo(data,imdecode.rows() ,imdecode.cols(),0.8);//需要的byte是一张完整的 jpg图片
                    if(res.size() > 0) {
                        byte[] resbytes = ImageEdit.editPlateInfos(rawBytes, res);
                        imdecode.put(0,0,resbytes);
                        Imgcodecs.imwrite("F:\\LPR\\data\\4k2\\"+frameCount+".jpg",imdecode);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            frameCount ++;

        }
    }
    @Test
    public  void testCompression() throws IOException {
        int bytesLength = 1024;
        byte[] bytes = new byte[bytesLength];
        for(int i = 0; i < bytesLength; i++) {
            bytes[i] = (byte) (i % 10);
        }
//Compress the data, and write it to somewhere (a byte array for this example)
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        DeflaterOutputStream outputStream = new DeflaterOutputStream(arrayOutputStream);
        outputStream.write(bytes);
        System.out.println(arrayOutputStream.toByteArray().length);
        outputStream.close();

//Read and decompress the data
        int read;
        byte[] finalBuf = new byte[0], swapBuf;
        byte[] readBuffer = new byte[5012];

        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(
                arrayOutputStream.toByteArray());
        InflaterInputStream inputStream = new InflaterInputStream(
                arrayInputStream);
        while ((read = inputStream.read(readBuffer)) != -1) {
            System.out.println("Intermediate read: " + read);
            swapBuf = finalBuf;
            finalBuf = new byte[swapBuf.length + read];//动态扩展存储空间
            System.arraycopy(swapBuf, 0, finalBuf, 0, swapBuf.length);
            System.arraycopy(readBuffer, 0, finalBuf, swapBuf.length, read);;
        }

        System.out.println(Arrays.equals(bytes, finalBuf));//压缩和解压缩之后进行比较

    }
    @Test
    public  void testJPEGCompression() throws IOException {
        System.load(AppConfig.OPENCV_LIB_FILE);
        VideoCapture videoCapture = new VideoCapture();
        videoCapture.open("F:\\LPR\\data\\4k2\\4k2.mp4");
        Mat frame = new Mat();
        int framecount = 1;
        int minus = 0;
        while (videoCapture.read(frame)) {
            MatOfByte bytemat = new MatOfByte();
            Imgcodecs.imencode(".jpg", frame, bytemat);
            ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
            DeflaterOutputStream outputStream = new DeflaterOutputStream(arrayOutputStream);
            outputStream.write(bytemat.toArray());
            int after = arrayOutputStream.toByteArray().length;
            int before = bytemat.toArray().length;
            System.out.println(((before - after)+ minus)/framecount);
            minus = before - after;
            outputStream.close();
            framecount++;
        }
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
     //   ArrayList<PlateInfo> res = pr.getPlateInfo(bytes, 0.6);//需要的byte是一张完整的 jpg图片
        long end = new Date().getTime();
        System.out.println("time is" + (end - start));

    //    byte[] resbytes = ImageEdit.editPlateInfos(bytes, res);

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

    //    image.put(0,0,resbytes);
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
