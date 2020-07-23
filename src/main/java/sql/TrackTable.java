package sql;

import java.util.HashMap;

public  class TrackTable{
    @Override
    public String toString() {
        return "TrackTable{" +
                "plateStr='" + plateStr + '\'' +
                ", cameraSeq='" + cameraSeq + '\'' +
                '}';
    }
   public void append(TrackTable tnew){
        this.cameraSeq += ","+ tnew.getCameraSeq();
   }
    public String getPlateStr() {
        return plateStr;
    }

    public void setPlateStr(String plateStr) {
        this.plateStr = plateStr;
    }

    public String getCameraSeq() {
        return cameraSeq;
    }

    public void setCameraSeq(String cameraSeq) {
        this.cameraSeq = cameraSeq;
    }

    private  String plateStr = null;
    private  String cameraSeq = null;//最长为64KB，受限于TEXT的大小

    public TrackTable(String plateStr, String cameraSeq) {
        this.plateStr = plateStr;
        this.cameraSeq = cameraSeq;
    }

    public TrackTable(String plateStr, String[] cameraSeq) {
        this.plateStr = plateStr;
        for(String cmera:cameraSeq){
            this.cameraSeq += cmera + ",";
        }
    }

    public static HashMap<Integer, String> resolveTrace(TrackTable tt){
        HashMap<Integer,String> cmeraSeq = new HashMap<>();
        String[] traces = tt.cameraSeq.split(",");
        for(String trace: traces){
            if(trace.isEmpty())
                continue;
            String str[] = trace.split(":");
            String cameraID = str[0];
            Integer time = new Integer(str[1]);
            cmeraSeq.put(time, cameraID);
        }
        return cmeraSeq;
    }



}
