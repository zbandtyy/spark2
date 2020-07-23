package pr;
import config.AppConfig;
import org.apache.log4j.Logger;
import org.apache.spark.sql.catalyst.expressions.aggregate.Collect;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.opencv.imgcodecs.Imgcodecs.imread;

/**
 * @author ：tyy
 * @date ：Created in 2020/5/14 15:20
 * @description：
 * @modified By：
 * @version: $
 */
public class PlateRecognition {




    private static final Logger logger = Logger.getLogger(PlateRecognition.class);

        /**
         * 实现图片的传图，返回相关信息,私有函数
         *
         * @param ptrNative
         * @param img
         * @return
         */
        protected native PlateInfo[] plateRecognize(long ptrNative, byte[] img);

        /**
         * 初始化加载类的模型文件,只需要model上层的
         *
         * @param path  模型文件顶级目录
         * @return
         */
        protected native long initPath(String path);

       /*
     *
     * @param detector_filename  各个模型路径 + 文件名
     *
     */
        protected native long initPath(String detector_filename,
                                       String finemapping_prototxt, String finemapping_caffemodel,
                                       String segmentation_prototxt, String segmentation_caffemodel,
                                       String charRecognization_proto, String charRecognization_caffemodel,
                                       String segmentationfree_proto,String segmentationfree_caffemodel);

        /**
         * 删除c++类，删除前确信不在调用，不然需重新执行init函数给ptrNative赋值 只能执行一次，两次出错，内存释放只有一次
         *
         * @param ptrNative
         * @return
         */
        protected native void delete(long ptrNative);

        // 静态调用只执行一次，加载本地库 ,编译好的动态放到src/main/resources下面

        //作为单例模式
        protected static long ptrNative = 0;
        private volatile static PlateRecognition uniqueHyperLPR= null;
        private PlateRecognition(String path) {

            logger.warn("ptrNative assign");
            ptrNative = initPath(path);
            logger.warn("ptrNative success");

        }

        private PlateRecognition() {

        }

        public static PlateRecognition getHyperLPR(String path) {

            if (uniqueHyperLPR == null) {
                synchronized (PlateRecognition.class) {//多个task并行，可能导致创建多个实例
                    if (uniqueHyperLPR == null) {
                        logger.warn("load init xml");
                        uniqueHyperLPR = new PlateRecognition(path);//只会初始化一次，创建的实例模型加载必须只能有一次
                        logger.warn("success one load" + ptrNative);
                    }
                }
            }
            return uniqueHyperLPR;
        }

        public ArrayList<PlateInfo> getPlateInfo(byte[] img,double confidenceThreshold) throws UnsupportedEncodingException {
            logger.warn("plate recognize  initial");
            PlateInfo[] pi  = plateRecognize(ptrNative, img);
            logger.warn("plate   sucess");
            ArrayList<PlateInfo> pA = new ArrayList<>();
            for (int i = 0; i < pi.length; i++) {
                if(pi[i].confidence > confidenceThreshold) {
                    String str = new String(pi[i].getName().getBytes(),"utf-8");
                    pA.add(new PlateInfo(pi[i].getRoi(),str,pi[i].confidence));
                }
            }
            return pA;
        }
        static {
            System.load(AppConfig.HYPERLPR_LIB_PATH);

        }

        //资源清理
        @Override
        protected void finalize() {
            if (ptrNative != 0) {
                delete(ptrNative);
            }
        }

}
