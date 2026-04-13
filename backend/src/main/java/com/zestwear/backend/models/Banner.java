package com.zestwear.backend.models;

import jakarta.persistence.*;

@Entity
@Table(name = "banners")
public class Banner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String imageUrl;
    private String link;
    private Boolean active = true;
    private Integer orderIndex = 0;

    public Banner() {}

    public Banner(String title, String imageUrl, String link, Boolean active, Integer orderIndex) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.link = link;
        this.active = active;
        this.orderIndex = orderIndex;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
}
