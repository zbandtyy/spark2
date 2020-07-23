package pr;//package easypr; 如果要使用 必须放在包外

import org.apache.log4j.Logger;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

//作为单例
@Deprecated
class EasyPR implements Serializable {
	private static final Logger logger = Logger.getLogger(EasyPR.class);

	/**
	 * 实现图片的传图，返回相关信息,私有函数
	 *
	 * @param ptrNative
	 * @param img
	 * @return
	 */
	protected native byte[] plateRecognize(long ptrNative, byte[] img);

	;;;

	/**
	 * 初始化类的模型文件
	 *
	 * @param path  模型文件顶级目录
	 * @revimturn
	 */
	protected native long initPath(String path);

	/**
	 * 预留函数，初始化类
	 *
	 * @return
	 */
	protected native long init();

	/**
	 * 删除c++类，删除前确信不在调用，不然需重新执行init函数给ptrNative赋值 只能执行一次，两次出错，内存释放只有一次
	 *
	 * @param ptrNative
	 * @return
	 */
	protected native void delete(long ptrNative);


	// 静态调用只执行一次，加载本地库 ,编译好的动态放到src/main/resources下面
	static {
		//System.loadLibrary("easyprjni");
		logger.warn("load eaypr2 ....");
		System.load("/home/user/share/shared/PR/easypr-EasyPR-master/EasyPR/final/libeasyprjni.so");
		logger.warn("load eaypr2 Completed");
	}

	//作为单例模式
	protected static long ptrNative = 0;
	private volatile static EasyPR uniqueEasypr = null;

	private EasyPR(String path) {

			logger.warn("ptrNative assign");
			ptrNative = initPath(path);
			logger.warn("ptrNative success");

	}

	private EasyPR() {
	}

	public static EasyPR getEasyPR(String path) {

		if (uniqueEasypr == null) {
			synchronized (EasyPR.class) {//多个task并行，可能导致创建多个实例
				if (uniqueEasypr == null) {
					logger.warn("load init xml");
					uniqueEasypr = new EasyPR(path);//只会初始化一次，创建的实例模型加载必须只能有一次
					logger.warn("success one load" + ptrNative);
				}
			}
		}
		return uniqueEasypr;
	}

	public String getPlateStr(byte[] img) throws UnsupportedEncodingException {
		logger.warn("plate recognize  initial");
		byte[] res = plateRecognize(ptrNative, img);
		String rawString  = new String(res, "utf-8");
		if(rawString.equals("null"))//与识别的底层实现有关
			return "";
		logger.warn("raw  plate：" + res);
		String plateStr[] = rawString.split(",");
		String plates ="";
		for(String s :plateStr){
			String info[] = s.split(":");
			if(info.length >= 2 ){
				if(!plates.isEmpty()) {
					plates += ",";
				}
				plates += info[1];
			}
		}
		logger.warn("plate end......" + plates);
		return plates;
	}

	//资源清理
	@Override
	protected void finalize() {
		if (ptrNative != 0) {
			delete(ptrNative);
		}
	}
}