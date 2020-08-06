package datatype;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Base64;

/**
 * Java Bean to hold JSON message
 * 输入数据
 * @author ;;
 *
 */
@NoArgsConstructor
public class VideoEventData  implements Serializable {
	@Setter @Getter
	private String cameraId;

	public VideoEventData(String cameraId, Timestamp timestamp, int rows, int cols, int type, String data) {
		this.cameraId = cameraId;
		this.timestamp = timestamp;
		this.rows = rows;
		this.cols = cols;
		this.type = type;
		this.data = data;
	}

	public VideoEventData(String cameraId, Timestamp timestamp, int rows, int cols, int type, byte[] jpgImageBytes) {
		this.cameraId = cameraId;
		this.timestamp = timestamp;
		this.rows = rows;
		this.cols = cols;
		this.type = type;
		this.jpgImageBytes = jpgImageBytes;
		this.data = null;
	}

	public VideoEventData(String cameraId, Timestamp timestamp, int rows, int cols, int type, byte[] jpgImageBytes, String data) {
		this.cameraId = cameraId;
		this.timestamp = timestamp;
		this.rows = rows;
		this.cols = cols;
		this.type = type;
		this.jpgImageBytes = jpgImageBytes;
		this.data = data;
	}
	@Setter @Getter
	private Timestamp timestamp;
	@Setter @Getter
	private int rows;
	@Setter @Getter
	private int cols;
	@Setter @Getter
	private int type;
	 @Setter @Getter
	private byte[] jpgImageBytes = null;//外部不应该随意操作这两种数据，需要对其进行限制，但是spark中会使用到。
	@Setter  @Getter
	private String data;//在spark进行初始化的时候需要使用Getter，但是再自己的代码中平时不应使用
	/*返回的一定是可以用的jpg的格式数据*/
	public byte[] getImagebytes() {
		if(this.getData() == null){

			return  jpgImageBytes;
		}
		byte[] pic = Base64.getDecoder().decode(this.getData());
		System.out.println(pic.length);
		if(pic.length >= this.getRows()*this.getCols() * 3) {

			Mat frame = new Mat(this.getRows(), this.getCols(), this.getType());
			frame.put(0, 0, pic);
			MatOfByte mob = new MatOfByte();
			Imgcodecs.imencode(".jpg", frame, mob);
			return mob.toArray();
		}else {
			return  pic;
		}



	}

	public String toJson(){

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
	public VideoEventData fromJson(String data){
		Gson gson = new Gson();
		/**
		 *  <T> T fromJson(String json, Class<T> classOfT)
		 *  json：被解析的 json 字符串
		 *  classOfT：解析结果的类型，可以是基本类型，也可以是 POJO 对象类型，gson 会自动转换
		 */
		VideoEventData p = gson.fromJson(data, VideoEventData.class);
		return p;
	}

	public void setJpgImageBytes(byte[] jpgImageBytes) {
		this.data = null;
		this.jpgImageBytes = jpgImageBytes;
	}

	@Override
	public String toString() {
		return "VideoEventData{" +
				"cameraId='" + cameraId + '\'' +
				", timestamp=" + timestamp +
				", rows=" + rows +
				", cols=" + cols +
				", type=" + type +
				", jpgImageBytes=" + (jpgImageBytes==null) +
				", data='" + (data == null) + '\'' +
				'}';
	}

	public static void main(String[] args) {
		VideoEventData data = new VideoEventData("vid",new Timestamp(12345),3,4,3,"data");
		System.out.println(data.getData());
		String s = data.toJson();
		System.out.println(s);
		VideoEventData d = data.fromJson(s);
		System.out.println(d);

	}
}


