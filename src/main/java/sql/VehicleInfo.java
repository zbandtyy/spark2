package sql;

public class VehicleInfo{
    String plateStr;
    String VIN;
    String carBranch;

    @Override
    public String toString() {
        return "VehicleInfo{" +
                "plateStr='" + plateStr + '\'' +
                ", VIN='" + VIN + '\'' +
                ", carBranch='" + carBranch + '\'' +
                ", owner='" + owner + '\'' +
                ", ownerID='" + ownerID + '\'' +
                '}';
    }

    String owner;
    String ownerID;

    public VehicleInfo(String plateStr, String VIN, String carBranch, String owner, String ownerID) {
        this.plateStr = plateStr;
        this.VIN = VIN;
        this.carBranch = carBranch;
        this.owner = owner;
        this.ownerID = ownerID;
    }
}
