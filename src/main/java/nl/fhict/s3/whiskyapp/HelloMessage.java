package nl.fhict.s3.whiskyapp;

public class HelloMessage {
    private String userid;

    public HelloMessage() {
    }

    public HelloMessage(String id) {
        this.userid = id;
    }

    public String getId() {
        return userid;
    }

    public void setId(String id) {
        this.userid = id;
    }
}
