package no.andreas.fugler;

import java.io.Serializable;

public class BirdImage implements Serializable{
    private String slug;
    private String name;

    public BirdImage(String slug, String photographer) {
        this.slug = slug;
        this.name = photographer;
    }

    public String getSlug() {
        return slug;
    }
    public String getName() {
        return name;
    }

    public void setSlug(final String slug) {
        this.slug = slug;
    }
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.slug;
    }
}
