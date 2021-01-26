package nl.fhict.s3.whiskyapp;

public class Player {
    private int id;
    private String name;

    //regel comment om build te triggeren
    public Player(int id, String name){
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
