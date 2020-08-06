package serialize;


import datatype.VideoEventData;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import org.apache.kafka.common.serialization.Deserializer;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.Map;

/**
 * @author ：tyy
 * @date ：Created in 2020/7/29 19:48
 * @description：
 * @modified By：
 * @version: $
 */
public class VideoEventDataKryoDeSerializer implements Deserializer<VideoEventData>, Serializable {

    static private final ThreadLocal<Kryo> tLocal = new ThreadLocal<Kryo>() {
        protected Kryo initialValue() {
            Kryo kryo = new Kryo();
            kryo.register(VideoEventData.class);
            kryo.setReferences(false);//好像很重要！！！！！！
            kryo.setRegistrationRequired(false);
            kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());

            return kryo;
        };
    };
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public VideoEventData deserialize(String topic, byte[] data) {
        Kryo kryo=tLocal.get();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        //2.1构建流对象
        Input input=new Input(byteArrayInputStream);

        //2.2对象反序列化
        VideoEventData obj=  kryo.readObject(input,VideoEventData.class);
        //2.3释放资源
        input.close();
        return obj;
    }

    @Override
    public void close() {

    }
}
