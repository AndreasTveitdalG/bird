package no.andreas.fugler;

import java.io.Serializable;

public class Bird implements Serializable{
    private Integer id;
    private String slug;
    private String name;

    public Bird(Integer id, String slug, String name) {
        this.id = id;
        this.slug = slug;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }
    public String getSlug() {
        return slug;
    }
    public String getName() {
        return name;
    }

    public void setId(final Integer id) {
        this.id = id;
    }
    public void setSlug(final String slug) {
        this.slug = slug;
    }
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return (this.id == null ? "missing" : this.id) + " " + this.slug;
    }
}
