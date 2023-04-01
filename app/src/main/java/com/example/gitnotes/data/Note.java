package com.example.gitnotes.data;

import java.io.Serializable;

public class Note implements Serializable, Comparable {
    private String title;
    private String body;

    public Note(String title, String body) {
        this.title = title;
        this.body = body;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "title: " + title + "\nbody: " + body;
    }

    @Override
    public int compareTo(Object other) {
        int titleCompare = this.title.compareTo(((Note) other).title);
        if (titleCompare != 0) {
            return titleCompare;
        } else {
            return this.body.compareTo(((Note) other).body);
        }
    }
}
