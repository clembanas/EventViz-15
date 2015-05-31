package logic.clustering.networking;


public enum ClusteringMessageType {
	AddLocation(0), WaitForResult(1), Exception(2), WaitForResultResponse(3);
	
	private byte val;

	ClusteringMessageType(int val) {
        this.val = (byte)val;
    }
	
	public byte getVal() {
        return val;
    }
	
	public static ClusteringMessageType fromByte(byte x) {
        switch(x) {
        case 0:
            return AddLocation;
        case 1:
            return WaitForResult;
        }
        return null;
    }
}
