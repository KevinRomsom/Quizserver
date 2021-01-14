package nl.fhict.s3.whiskyapp;

public class Greeting {
    private String content;

    private String name;

    public Greeting() {
    }

    public Greeting(String content, String name) {
        this.content = content;
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public String getName() {
        return name;
    }
}
