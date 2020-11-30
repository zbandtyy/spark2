
package detection;


import config.AppConfig;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class Detector implements Serializable {

    private long peer;

    public long getPeer() {
        return peer;
    }

    // detect by input bytes...
    public BoxesAndAcc[] execComputeBoxesAndAccByInputBytes(long strutWrapperPeer, byte[] bytes, String outfile, float thresh, float hier_thresh, int fullscreen, int w, int h, int c) {
        return computeBoxesAndAccByInputBytes(strutWrapperPeer, bytes, outfile, thresh, hier_thresh, fullscreen, w,h,c);
    }

    public byte[] jpgfile2bytes(String path, int w, int h, int c) {
        System.out.println("path: " + path + " " + c);
        return jpg2Bytes(path, w, h , c);
    }

    private Detector(String cfgfile, String weightfile,String datacfg,  String labelpath, int batchsize, int gpu_index, String gpuid){
        peer = initialize(cfgfile, weightfile,  datacfg,   labelpath, batchsize, gpu_index, gpuid);
    }

    /**
     * i 为 valArr下标
     * b 为次数
     * c 为a 下标
     */
    public byte[] getCopyArray(byte[] valArr, int t){
        byte[] a=null;//定义一个空数组
        if(null!=valArr){
            a=new byte[valArr.length*t];//生成多少次
            for(int i=0,b=0,c=0;i<=valArr.length && b<t && c<valArr.length*t;i++,c++){
                if(i>valArr.length-1){
                    i=0;
                    b++;
                }
                a[c]=valArr[i];
            }
        }
        return a ;
    }

    private native byte[] jpg2Bytes(String path, int w, int h, int c);

    private native BoxesAndAcc[] computeBoxesAndAccByInputBytes(long strutWrapperPeer, byte[] bytes, String outfile, float thresh, float hier_thresh, int fullscreen, int w, int h, int c);

    private native long initialize(String cfgfile, String weightfile, String datacfg, String labelpath, int batchsize, int gpu_index, String gpuid );

    static {
        //System.load("/home/kfch/Downloads/0924/0925/Detector-v6/libjdetection.so");

        System.load(AppConfig.YOLO_LIB_FILE);
        //System.loadlibrary("libjdetection");
    }

    private volatile static Detector uniqueDetector  = null;
    public  static Detector getYoloDetector(String Path) {

        if (uniqueDetector == null) {
            System.out.println("uniqueDetector ====null" );
            synchronized (Detector.class) {//多个task并行，可能导致创建多个实例
                if (uniqueDetector == null) {
                    if(Path.charAt(Path.length() - 1) != '/'){
                        Path+="/";

                    }
                    int bs =1;// for default...
                    int gpu_index = 0; // Gpu for default...
                    String gpuid = "0,1";
                    String cfgPath = Path+"cfg/yolov3.cfg";
                    String weightsPath =Path+ "yolov3.weights";
                    String datacfg = Path+"cfg/coco.data";
                    String labelpath = Path+"data/labels";
                    System.out.println("start load" );

                    uniqueDetector = new Detector(cfgPath, weightsPath,datacfg, labelpath,bs, gpu_index, gpuid);//只会初始化一次，创建的实例模型加载必须只能有一次
                    System.out.println("成功加载"+labelpath);
                }

            }
        }
        return uniqueDetector;


    }
    public  BoxesAndAcc[] startYolo(byte[] jpgbytes, int w, int h, int c) {
        if(jpgbytes.length < w * h * c && w*h*c <= 0){
            System.out.println(jpgbytes.length + "< (w=" + w + ")*(h="+ h+")*(c=" + c+")" + w*h*c);
            return  null;

        }
        //System.out.println(jpgbytes.length + "< (w=" + w + ")*(h="+ h+")*(c=" + c+")" + w*h*c);
        synchronized (Detector.class) {

            BoxesAndAcc[] boxesAndAccs = this.execComputeBoxesAndAccByInputBytes(this.getPeer(), jpgbytes, "prediction", (float) 0.5, (float) 0.5, 1, w, h, c);// from jpgbytes
            System.out.println("recognize sucess,the length is" + boxesAndAccs.length);
            if (boxesAndAccs == null) {
                System.out.println("No Boxes");
            }
            ArrayList<BoxesAndAcc> blist = new ArrayList<>();
            for (BoxesAndAcc boxesAndAcc : boxesAndAccs) {

                if (boxesAndAcc.isVaild == true && ((boxesAndAcc.names.equals("car") || boxesAndAcc.names.equals("person")))) {
                    blist.add(boxesAndAcc);//这个内存交接后是不是会被释放？
                }
            }// or
            String name = Thread.currentThread().getName();
            System.out.println(name + "==== this yolo recognize" );
            BoxesAndAcc[] bArray = new BoxesAndAcc[blist.size()];

            blist.toArray(bArray);

            return bArray;
        }
    }

    public static void main(String[] args) throws Exception {
        int w=640;
        int h=480;
        int c=3;
        int bs =1;// for default...
        int gpu_index = 0; // Gpu for default...
        String gpuid = "0,1";

        int argsLength = args.length;

        String cfgPath = "./cfg/yolov3.cfg";
        if(argsLength>=1){
            cfgPath = args[0];
        }

        String weightsPath = "./yolov3.weights";
        if(argsLength>=2){
            weightsPath = args[1];
        }


        String datacfg = "./cfg/coco.data";
        if(argsLength>=3){
            datacfg = args[2];
        }


        String labelpath = "./data/labels";
        if(argsLength>=4){
            labelpath = args[3];
        }

        String path = "./dog.jpg";
        if(argsLength>=5){
            path = args[4];
        }

        if(argsLength>=6){
            bs= Integer.parseInt(args[5]);
        }

        if(argsLength>=7){
            gpu_index= Integer.parseInt(args[6]);
        }

        if(argsLength>=8){
            gpuid= args[7];
        }

        Detector obj = new Detector(cfgPath, weightsPath,datacfg, labelpath,bs, gpu_index, gpuid);

        BufferedImage sourceImg =ImageIO.read(new FileInputStream(path));

        //System.out.println(String.format("%.1f",picture.length()/1024.0));// 源图大小
        //System.out.println(sourceImg.getWidth()); // 源图宽度
        //System.out.println(sourceImg.getHeight()); // 源图高度

        w = sourceImg.getWidth() ;
        h = sourceImg.getHeight();
        c = 3;

        byte[] jpgbytes = obj.jpgfile2bytes(path, w, h,  c);
        //System.out.println("jpgbytes: " + (jpgbytes[1]<0?(256 + jpgbytes[1]):(jpgbytes[1])));
        //System.out.println("jpgbytes len: " + jpgbytes.length);

        //简单的拼接一个buffer...
        byte[] jpgbytes_bs = new byte[w*h*c*(bs+2)];
        jpgbytes_bs = obj.getCopyArray(jpgbytes, bs + 1);


        int count=0;

        long sumtime = 0;

        while(count< bs){
            System.out.println("This is the " + (count + 1) + " pic of batchsize = " + bs + ".");

            count+=1;

            /** 获取当前系统时间*/
            long startTime =  System.currentTimeMillis();

            // 通过输入的Bytes来预测。。。
            byte[] jpgbytes_bs_part = Arrays.copyOfRange(jpgbytes_bs, w*h*c*count, w*h*c*(count + 1));
            BoxesAndAcc[] boxesAndAccs = obj.execComputeBoxesAndAccByInputBytes(obj.getPeer(), jpgbytes_bs_part  ,"predictions",(float)0.5, (float)0.5, 1, w,h,c);// from jpgbytes

            /** 获取当前的系统时间，与初始时间相减就是程序运行的毫秒数，除以1000就是秒数*/
            long endTime =  System.currentTimeMillis();
            long usedTime = (endTime-startTime);
            sumtime += usedTime;


            if(boxesAndAccs==null){
                System.out.println("No Boxes");
            }
            for(BoxesAndAcc boxesAndAcc : boxesAndAccs){

                if(boxesAndAcc.isVaild==true){
                    System.out.println("names:"+boxesAndAcc.names);
                    System.out.println("acc:"+boxesAndAcc.acc);
                    System.out.println("x:"+boxesAndAcc.boxes.x);
                    System.out.println("y:"+boxesAndAcc.boxes.y);
                    System.out.println("w:"+boxesAndAcc.boxes.w);
                    System.out.println("h:"+boxesAndAcc.boxes.h);
                    Box b = boxesAndAcc.boxes;
                    System.out.println("-------------------");

                }
            }// for
        }// while
        System.out.println("-------------------" + " batchsize = " + bs + ", total time: " + sumtime + " micro-secs.");
    }//main
} //class

