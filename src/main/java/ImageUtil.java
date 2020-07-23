import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yolo.BoxPosition;
import yolo.Config;
import yolo.IOUtil;
import yolo.Recognition;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Util class for image processing. 收到Detector影响只能换到外部包中
 */
public class ImageUtil<imageUtil> implements Serializable {
    //加载标签 进行绘制

    private final static Logger LOGGER = LoggerFactory.getLogger(ImageUtil.class);
    private static ImageUtil imageUtil;

    private ImageUtil() {
        IOUtil.createDirIfNotExists(new File(Config.OUTPUT_DIR));
    }
    public static Map<String, Integer> lableAndClass = null;

    /*
    * 初始化工作：
    * 1.加载需要绘制的标签文件
    *2.对画图位置进行必要的转换
    * */
    private static HashMap<String, Integer> getLables(String labelPath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(labelPath));
        String line = null;
        int i =1;
        HashMap<String, Integer> map = new HashMap<>();
        while ((line = reader.readLine()) != null) {
            map.put(line,i++);
        }
        reader.close();
        return map;
    }
    /**
     * It returns the singleton instance of this class加载需要绘制的label获取图形工具.
     * @return ImageUtil instance
     */
    public static ImageUtil getInstance(String labelPath) throws IOException {
        if (imageUtil == null ||lableAndClass == null) {
            imageUtil = new ImageUtil();
            lableAndClass =  getLables(labelPath);//保存标签分类
            }
        return imageUtil;
    }


    private BufferedImage createImageFromBytes(final byte[] imageData) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
        try {
            return ImageIO.read(bais);
        } catch (IOException ex) {
            throw ex;
        }
    }
    /**
     *
     * Label image with classes and predictions given by the ThensorFLow标记图片
     * @param image buffered image to label 需要BufferImage需要完整的图片
     * @param recognitions list of recognized objects 识别的对象进行标记
     */
    public byte[] labelImage(final byte[] image, final List<Recognition> recognitions) throws IOException {
        //6.对识别结果进行统计 需要绘制中文 在java中必须使用Graphics2D

        Map<String, Integer> car_person_map = new HashMap();
        car_person_map.put("car",0);
        car_person_map.put("person",0);
        BufferedImage bufferedImage = imageUtil.createImageFromBytes(image);
        Graphics2D graphics = (Graphics2D) bufferedImage.getGraphics();
        for (Recognition recognition: recognitions) {
            BoxPosition box1= recognition.getLocation();
           // BoxPosition box = recognition.getScaledLocation(scaleX, scaleY);//获取在图片的具体坐标，使用比例
            //draw text
            graphics.setPaint(Color.BLUE);//设置画笔,设置Paint属性
            graphics.drawString(recognition.getTitle(), box1.getLeft(), box1.getTop() - 7);
            // draw bounding box
            graphics.setPaint(Color.YELLOW);//设置画笔,设置Paint属性
            graphics.drawRect(box1.getLeftInt(),box1.getTopInt(), box1.getWidthInt(), box1.getHeightInt());

            if(recognition.getTitle().equals("car")){
                car_person_map.put("car",car_person_map.get("car")+1);
            } else if(recognition.getTitle().equals("person")){
                car_person_map.put("person",car_person_map.get("person")+1);
            }
        }
    //绘制总的信息
        int y = 20;
        graphics.setPaint(Color.GREEN);//设置画笔,设置Paint属性
        Font font = new Font(Font.SERIF,Font.BOLD,20);
        graphics.setFont(font);
        for(String key: car_person_map.keySet()) {
            graphics.drawString(key + ": " + car_person_map.get(key), 0, y );
            y=y+20;
        }
        LOGGER.warn("car"+ car_person_map.get("car") + "person" + car_person_map.get("person"));
        graphics.setPaint(Color.GREEN);//设置画笔,设置Paint属性
        byte[] data = ((DataBufferByte) bufferedImage.getData().getDataBuffer()).getData();
//        saveImage(bufferedImage, Config.OUTPUT_DIR + "/" + fileName);
        graphics.dispose();
        return  data;
    }

    public void saveImage(final BufferedImage image, final String target) {
        try {
            ImageIO.write(image,"jpg", new File(target));
        } catch (IOException e) {
            LOGGER.error("Unagle to save image {}!", target);
        }
    }
    /***
     * 为labelImage提供位置标识转换的服务函数
     */
    public  List<Recognition> transfor(BoxesAndAcc[] Box_acc, int w, int h ){
        List<Recognition>  list =  new ArrayList(); ;
        for (int i = 0; i< Box_acc.length; i++){

            BoxesAndAcc tmp = Box_acc[i];
            if(tmp.boxes.x == Float.NEGATIVE_INFINITY || tmp.boxes.x == Float.POSITIVE_INFINITY ||
                tmp.boxes.y == Float.NEGATIVE_INFINITY || tmp.boxes.y == Float.POSITIVE_INFINITY ||
                    tmp.boxes.h == Float.NEGATIVE_INFINITY || tmp.boxes.h == Float.POSITIVE_INFINITY ||
                    tmp.boxes.w == Float.NEGATIVE_INFINITY || tmp.boxes.w == Float.POSITIVE_INFINITY
                ){
                continue;
            }
            list.add(new Recognition(lableAndClass.get(tmp.names),tmp.names,tmp.acc,transPos(tmp.boxes,w,h)));
            System.out.println("list add on" + i);
        }
        return list;
    }


    // 找出位置对应的中心点，为Box_acc找出对应的边界
    private  BoxPosition transPos(Box box, int w, int h){
        System.out.println( String.format("(%.2f,%.2f,%.2f,%.2f)",box.x,box.y,box.w,box.h));
        float left = (box.x - box.w/2) * w;//w*（box.x - box.w/2）
        float top =  (box.y  - box.h/2)*h;

        float bot   = (box.y + box.h/2)*h;
        float right = (box.x + box.w/2)*w;
        System.out.println("========left top===================left"+ left +"top"+ top);

        BoxPosition res = new BoxPosition(left, top,Math.abs(bot - top), Math.abs(right -left) );
        System.out.println("res"+res == null);
        return res;
    }
}
