package serialize;

import datatype.VideoEventData;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import org.apache.kafka.common.serialization.Serializer;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @author ：tyy
 * @date ：Created in 2020/7/29 18:16
 * @description：
 * @modified By：
 * @version: $
 */
public class VideoEventDataKryoSerializer implements Serializer<VideoEventData> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }
    static private final ThreadLocal<Kryo> tLocal = new ThreadLocal<Kryo>() {
        protected Kryo initialValue() {
            Kryo kryo = new Kryo();
            kryo.setRegistrationRequired(false);
            return kryo;
        };
    };
    @Override
    public byte[] serialize(String topic, VideoEventData data) {



        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        Kryo kryo = tLocal.get();
        kryo.setReferences(false);
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        kryo.register(VideoEventData.class);
        kryo.writeObject(output,data);

        try {
            baos.close();
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] data1 = baos.toByteArray();
        System.out.println(data1.length);
        return  data1;

    }

    @Override
    public void close() {

    }
}
