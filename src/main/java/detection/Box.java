package detection;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;


@ToString
public class Box  implements Serializable {
    @Getter @Setter
    public float x;
    @Getter @Setter
    public float y;
    @Getter @Setter
    public float w;
    @Getter @Setter
    public float h;

}
