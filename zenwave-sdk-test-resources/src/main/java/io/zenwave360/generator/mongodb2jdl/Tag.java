package io.zenwave360.generator.mongodb2jdl;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A Tag.
 */
@Document(collection = "tag")
public class Tag implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @NotNull(message = "must not be null")
    @Size(min = 2)
    @Field("name")
    private String name;

    @Field("posts")
    @DBRef
    @JsonIgnoreProperties(value = {"blog", "tags"}, allowSetters = true)
    private Set<Post> posts = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public String getId() {
        return this.id;
    }

    public Tag id(String id) {
        this.setId(id);
        return this;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public Tag name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Post> getPosts() {
        return this.posts;
    }

    public void setPosts(Set<Post> posts) {
        if (this.posts != null) {
            this.posts.forEach(i -> i.removeTag(this));
        }
        if (posts != null) {
            posts.forEach(i -> i.addTag(this));
        }
        this.posts = posts;
    }

    public Tag posts(Set<Post> posts) {
        this.setPosts(posts);
        return this;
    }

    public Tag addPost(Post post) {
        this.posts.add(post);
        post.getTags().add(this);
        return this;
    }

    public Tag removePost(Post post) {
        this.posts.remove(post);
        post.getTags().remove(this);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tag)) {
            return false;
        }
        return id != null && id.equals(((Tag) o).id);
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Tag{" +
                "id=" + getId() +
                ", name='" + getName() + "'" +
                "}";
    }
}
