package no.andreas.fugler;

import java.io.Serializable;

class BirdSound implements Serializable {
    private Integer id;
    private String slug;

    public BirdSound(Integer id, String slug) {
        this.id = id;
        this.slug = slug;
    }

    public Integer getId() {
        return id;
    }
    public String getSlug() {
        return slug;
    }

    public void setId(final Integer id) {
        this.id = id;
    }
    public void setSlug(final String type) {
        this.slug = type;
    }

    @Override
    public String toString() {
        return this.id + " " + this.slug;
    }
}