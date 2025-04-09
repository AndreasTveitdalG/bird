package no.andreas.fugler;

import java.io.Serializable;

class BirdSound implements Serializable {
    private Integer id;
    private String type;

    public BirdSound(Integer id, String type) {
        this.id = id;
        this.type = type;
    }

    public Integer getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return this.id + " " + this.type;
    }
}