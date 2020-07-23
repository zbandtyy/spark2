import config.AppConfig;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import scala.App;
import yolo.Recognition;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;
/*不能定义到内部包去*/
class BoxesAndAcc{
	public float acc;
	public Box boxes;
	public String names;
	public boolean isVaild;
	public int size;
}
class Box {
	public float x;
	public float y;
	public float w;
	public float h;
}

/***
 * YOLO 算子，课题五
 */
public class  Detector {

	private long peer;

	public long getPeer() {
		return peer;
	}

	// detect by input bytes...
	public BoxesAndAcc[] execComputeBoxesAndAccByInputBytes(long strutWrapperPeer, byte[] bytes, String outfile, float thresh, float hier_thresh, int fullscreen, int w, int h, int c) {
		BoxesAndAcc[] b = null;
		return b = computeBoxesAndAccByInputBytes(strutWrapperPeer, bytes, outfile, thresh, hier_thresh, fullscreen, w,h,c);
	}

	public byte[] jpgfile2bytes(String path, int w, int h, int c) {
		System.out.println("path: " + path + " " + c);
		return jpg2Bytes(path, w, h , c);
	}

	private Detector(String cfgfile, String weightfile,String datacfg,  String labelpath, int batchsize, int gpu_index, String gpuid){

		peer = initialize(cfgfile, weightfile, datacfg, labelpath, batchsize, gpu_index, gpuid);

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
		System.load(AppConfig.YOLO_LIB_PATH);
		//System.loadlibrary("libjdetection");
	}
	private  static Detector uniqueDetector  = null;
	public static Detector getDetector(String Path) {

		if (uniqueDetector == null) {
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
					uniqueDetector = new Detector(cfgPath, weightsPath,datacfg, labelpath,bs, gpu_index, gpuid);//只会初始化一次，创建的实例模型加载必须只能有一次
					System.out.println("成功加载"+labelpath);
				}
			}
		}
		return uniqueDetector;
	}
	public  BoxesAndAcc[] startYolo(byte[] jpgbytes, int w, int h, int c) {
		BoxesAndAcc[] boxesAndAccs = this.execComputeBoxesAndAccByInputBytes(this.getPeer(), jpgbytes ,"predictions",(float)0.5, (float)0.5, 1, w,h,c);// from jpgbytes
		System.out.println("recognize sucess");
		if(boxesAndAccs==null){
			System.out.println("No Boxes");
		}
		ArrayList<BoxesAndAcc> blist = new ArrayList<>();
		for(BoxesAndAcc boxesAndAcc : boxesAndAccs){

			if(boxesAndAcc.isVaild==true && (boxesAndAcc.names.equals( "car") ||boxesAndAcc.names.equals( "person")) ){
				blist.add(boxesAndAcc);//这个内存交接后是不是会被释放？
			}
		}// or

		BoxesAndAcc[] bArray = new BoxesAndAcc[blist.size()];

		blist.toArray(bArray);
		return bArray;
	}
		public static void main(String[] args) throws Exception {
			System.load(AppConfig.OPENCV_LIB_FILE);

			////////////////////Mat 获取像素数组 bytes////////////
			//1.JNI所定义的所有类都需要定义在和Detector相同的包中
			//2.他需要所有的像素字节480
			Mat m = imread("./10.jpg", 1);
			long size = m.total() * m.elemSize();
			byte bytes[] = new byte[(int)size];  // you will have to delete[] that later
			m.get(0,0,bytes);
			System.out.println("===bytes==="+bytes.length);
			////////////////////Mat 转换成 jpg bytes////////////
			MatOfByte mob = new MatOfByte();
			Imgcodecs.imencode(".jpg", m, mob);
			// convert the "matrix of bytes" into a byte array
			byte[] byteArray = mob.toArray();


			///////////////YOLO识别//////////////////
			Detector d = Detector.getDetector(AppConfig.YOLO_RESOURCE_PATH);
			BoxesAndAcc[] Box_acc = d.startYolo(bytes,640,480,3);
			System.out.println("Box_acc length"+Box_acc.length);


			ImageUtil util = ImageUtil.getInstance(AppConfig.YOLO_LABEL_FILE);
			List<Recognition> recognitions = util.transfor(Box_acc,(int)640 ,(int)480) ;
			System.out.println("recognitions length"+recognitions.size());
			byte[] res = util.labelImage(byteArray, recognitions);
			//bytes 转换成 Mat///////////////////////////////////
			Mat image = new Mat(480,640,CV_8UC3); // make a copy
			image.put(0,0,res);

//			byte bytes1[] = new byte[image.rows()*image.cols()*image.channels()];  // you will have to delete[] that later
//			m.get(0,0,bytes1);
//			image.get(0,0,bytes1);
//			System.out.println("bytes.length"+bytes1.length);
//			d.startYolo(bytes,640,480,0);

			imwrite("after.jpg",image);

		}//main

	} //class