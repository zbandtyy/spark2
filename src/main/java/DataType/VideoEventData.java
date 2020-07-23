package DataType;

import com.google.gson.Gson;
import org.bouncycastle.math.ec.ScaleYPointMap;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Java Bean to hold JSON message
 * 输入数据 和 车辆跟踪的结果数据
 * @author abaghel
 *
 */
public class VideoEventData implements Serializable {

	private String cameraId;

	public VideoEventData(String cameraId, Timestamp timestamp, int rows, int cols, int type, String data) {
		this.cameraId = cameraId;
		this.timestamp = timestamp;
		this.rows = rows;
		this.cols = cols;
		this.type = type;
		this.data = data;
	}
	public VideoEventData(){}
	private Timestamp timestamp;
	private int rows;
	private int cols;
	private int type;
	private String data;

	public String getCameraId() {
		return cameraId;
	}
	public void setCameraId(String cameraId) {
		this.cameraId = cameraId;
	}	
	public Timestamp getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Timestamp timestamp) {
		this.timestamp = timestamp;
	}
	public int getRows() {
		return rows;
	}
	public void setRows(int rows) {
		this.rows = rows;
	}
	public int getCols() {
		return cols;
	}
	public void setCols(int cols) {
		this.cols = cols;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
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

	public static void main(String[] args) {
		VideoEventData data = new VideoEventData("vid",new Timestamp(12345),3,4,3,"data");
		String s = data.toJson();
		System.out.println(s);
		VideoEventData d = data.fromJson(s);
		System.out.println(d);
	}
}


